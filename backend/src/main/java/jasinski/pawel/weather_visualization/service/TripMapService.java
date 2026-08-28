package jasinski.pawel.weather_visualization.service;

import jasinski.pawel.weather_visualization.dto.AstronomyMarkerDto;
import jasinski.pawel.weather_visualization.dto.AstronomyStats;
import jasinski.pawel.weather_visualization.dto.MapDataResponse;
import jasinski.pawel.weather_visualization.dto.TrackPointDto;
import jasinski.pawel.weather_visualization.entity.TrackPoint;
import jasinski.pawel.weather_visualization.entity.Weather;
import jasinski.pawel.weather_visualization.repository.TrackPointRepository;
import jasinski.pawel.weather_visualization.utils.AstronomyAnalyzer;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.util.*;

@Service
public class TripMapService {

    private static final DateTimeFormatter MARKER_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm");

    private final TrackPointRepository trackPointRepository;

    public TripMapService(TrackPointRepository trackPointRepository) {
        this.trackPointRepository = trackPointRepository;
    }

    public MapDataResponse getTripMapData(Long tripId, String timezoneParam) {
        ZoneId zoneId = ZoneId.of(timezoneParam != null ? timezoneParam : "UTC");
        List<TrackPoint> rawPoints = trackPointRepository.findByTripIdOrderByTimeAsc(tripId);
        if (rawPoints.isEmpty()) return new MapDataResponse(List.of(), List.of(), Map.of());

        List<TrackPoint> points = filterPointsForMap(rawPoints);
        System.out.println("Optymalizacja mapy: Zredukowano z " + rawPoints.size() + " do " + points.size() + " punktów.");



        Map<LocalDate, List<TrackPoint>> pointsByDay = groupPointsByDay(points,zoneId);
        Map<LocalDate, List<TrackPoint>> rawPointsByDay = groupPointsByDay(rawPoints,zoneId);
        List<TrackPointDto> route = new ArrayList<>();
        List<AstronomyMarkerDto> markers = new ArrayList<>();

        TreeMap<Instant, Integer> timeline = new TreeMap<>();
        List<TrackPoint> allEnrichedPoints = new ArrayList<>(points);

        AstronomyStats firstDayAstro = null;
        LocalDate firstDate = null;

        for (Map.Entry<LocalDate, List<TrackPoint>> entry : pointsByDay.entrySet()) {
            LocalDate currentDate = entry.getKey();
            List<TrackPoint> dayPoints = entry.getValue();

            List<TrackPoint> rawDayPoints = rawPointsByDay.get(currentDate);
            AstronomyStats astro = AstronomyAnalyzer.calculateAstronomy(rawDayPoints, rawPoints, null, zoneId);

            if (firstDayAstro == null) {
                firstDayAstro = astro;
                firstDate = currentDate;
            }

            // Dodawanie znaczników zjawisk
            addMarkerIfPresent(markers, "ŚWIT ASTRONOMICZNY", astro.astronomicalDawnPt(), astro.astronomicalDawn(),zoneId);
            addMarkerIfPresent(markers, "ŚWIT NAUTYCZNY", astro.nauticalDawnPt(), astro.nauticalDawn(),zoneId);
            addMarkerIfPresent(markers, "ŚWIT CYWILNY", astro.civilDawnPt(), astro.civilDawn(),zoneId);
            addMarkerIfPresent(markers, "WSCHÓD SŁOŃCA", astro.sunrisePt(), astro.sunrise(),zoneId);
            addMarkerIfPresent(markers, "KULMINACJA SŁOŃCA", astro.noonPt(), astro.solarNoon(),zoneId);
            addMarkerIfPresent(markers, "ZACHÓD SŁOŃCA", astro.sunsetPt(), astro.sunset(),zoneId);
            addMarkerIfPresent(markers, "ZMIERZCH CYWILNY", astro.civilDuskPt(), astro.civilDusk(),zoneId);
            addMarkerIfPresent(markers, "ZMIERZCH NAUTYCZNY", astro.nauticalDuskPt(), astro.nauticalDusk(),zoneId);
            addMarkerIfPresent(markers, "ZMIERZCH ASTRONOMICZNY", astro.astronomicalDuskPt(), astro.astronomicalDusk(),zoneId);
            addMarkerIfPresent(markers, "WSCHÓD KSIĘŻYCA", astro.moonRisePt(), astro.moonRise(),zoneId);
            addMarkerIfPresent(markers, "ZACHÓD KSIĘŻYCA", astro.moonSetPt(), astro.moonSet(),zoneId);

            injectAstroPoint(allEnrichedPoints, astro.astronomicalDawnPt());
            injectAstroPoint(allEnrichedPoints, astro.nauticalDawnPt());
            injectAstroPoint(allEnrichedPoints, astro.civilDawnPt());
            injectAstroPoint(allEnrichedPoints, astro.sunrisePt());
            injectAstroPoint(allEnrichedPoints, astro.noonPt());
            injectAstroPoint(allEnrichedPoints, astro.sunsetPt());
            injectAstroPoint(allEnrichedPoints, astro.civilDuskPt());
            injectAstroPoint(allEnrichedPoints, astro.nauticalDuskPt());
            injectAstroPoint(allEnrichedPoints, astro.astronomicalDuskPt());
            injectAstroPoint(allEnrichedPoints, astro.moonRisePt());
            injectAstroPoint(allEnrichedPoints, astro.moonSetPt());

            addToTimeline(timeline, astro.astronomicalDawn(), 1);
            addToTimeline(timeline, astro.nauticalDawn(), 2);
            addToTimeline(timeline, astro.civilDawn(), 3);
            addToTimeline(timeline, astro.sunrise(), 4);
            addToTimeline(timeline, astro.sunset(), 5);
            addToTimeline(timeline, astro.civilDusk(), 6);
            addToTimeline(timeline, astro.nauticalDusk(), 7);
            addToTimeline(timeline, astro.astronomicalDusk(), 8);
        }

        allEnrichedPoints.sort(Comparator.comparing(TrackPoint::getTime));
        Map<String, double[]> ranges = new HashMap<>();

        for (TrackPoint point : allEnrichedPoints) {
            int phase = calculatePhaseFromTimeline(point, timeline, firstDayAstro, firstDate);
            route.add(mapToDto(point, phase));

            Weather w = point.getWeather();
            if (w != null) {
                updateRanges(ranges, "temp", w.getTemp() != null ? w.getTemp() + 273.15 : null);
                updateRanges(ranges, "humidity", w.getHumidity());
                updateRanges(ranges, "dew", w.getDewPoint() != null ? w.getDewPoint() + 273.15 : null);
                updateRanges(ranges, "pressure", w.getPressure() != null ? w.getPressure() * 100 : null);
                updateRanges(ranges, "rain", w.getRain());
                updateRanges(ranges, "snow", w.getSnowfall());
                updateRanges(ranges, "clouds", w.getCloudCover());
                updateRanges(ranges, "clouds_low", w.getCloudCoverLow());
                updateRanges(ranges, "clouds_mid", w.getCloudCoverMid());
                updateRanges(ranges, "clouds_high", w.getCloudCoverHigh());
                updateRanges(ranges, "wind", w.getWindSpeed() != null ? w.getWindSpeed() / 3.6 : null);
                updateRanges(ranges, "gusts", w.getWindGusts() != null ? w.getWindGusts() / 3.6 : null);
                updateRanges(ranges, "wave_h", w.getWaveHeight());
                updateRanges(ranges, "wave_p", w.getWavePeriod());
                updateRanges(ranges, "sea_temperature", w.getSeaTemperature() != null ? w.getSeaTemperature() + 273.15 : null);
                updateRanges(ranges, "ocean_current_velocity", w.getOceanCurrentVelocity() != null ? w.getOceanCurrentVelocity() / 3.6 : null);
                updateRanges(ranges, "wind_wave_h", w.getWindWaveHeight());
                updateRanges(ranges, "wind_wave_p", w.getWindWavePeriod());
                updateRanges(ranges, "swell_wave_h", w.getSwellWaveHeight());
                updateRanges(ranges, "swell_wave_p", w.getSwellWavePeriod());
            }

        }

        return new MapDataResponse(route, markers, ranges);
    }

    private Map<LocalDate, List<TrackPoint>> groupPointsByDay(List<TrackPoint> points, ZoneId zoneId) {
        Map<LocalDate, List<TrackPoint>> pointsByDay = new TreeMap<>();
        for (TrackPoint point : points) {
            LocalDate date = LocalDate.ofInstant(point.getTime(), zoneId);
            pointsByDay.computeIfAbsent(date, k -> new ArrayList<>()).add(point);
        }
        return pointsByDay;
    }

    private void addMarkerIfPresent(List<AstronomyMarkerDto> markers, String label, TrackPoint pt, Instant exactTime, ZoneId zoneId) {
        if (pt != null && exactTime != null) {
            String formattedTime = LocalDateTime.ofInstant(exactTime, zoneId).format(MARKER_FMT);
            markers.add(new AstronomyMarkerDto(label, pt.getLatitude(), pt.getLongitude(), formattedTime));
        }
    }

    private void injectAstroPoint(List<TrackPoint> list, TrackPoint pt) {
        if (pt != null && !list.contains(pt)) {
            list.add(pt);
        }
    }

    private void addToTimeline(TreeMap<Instant, Integer> timeline, Instant time, int phase) {
        if (time != null) {
            timeline.put(time, phase);
        }
    }

    private int calculatePhaseFromTimeline(TrackPoint point, TreeMap<Instant, Integer> timeline, AstronomyStats firstAstro, LocalDate firstDate) {

        Map.Entry<Instant, Integer> entry = timeline.floorEntry(point.getTime());

        if (entry != null) {
            return entry.getValue();
        }

        return calculateInitialPhase(point.getTime(), firstAstro, firstDate);
    }


    private int calculateInitialPhase(Instant pt, AstronomyStats astro, LocalDate date) {
        if (astro == null) return 4;

        Instant dawnAstro = astro.astronomicalDawn();
        Instant dawnNaut  = astro.nauticalDawn();
        Instant dawnCivil = astro.civilDawn();
        Instant sunrise   = astro.sunrise();
        Instant sunset    = astro.sunset();
        Instant duskCivil = astro.civilDusk();
        Instant duskNaut  = astro.nauticalDusk();
        Instant duskAstro = astro.astronomicalDusk();

        if (duskAstro != null && !pt.isBefore(duskAstro)) return 8;
        if (duskNaut != null && !pt.isBefore(duskNaut)) return 7;
        if (duskCivil != null && !pt.isBefore(duskCivil)) return 6;
        if (sunset != null && !pt.isBefore(sunset)) return 5;
        if (sunrise != null && !pt.isBefore(sunrise)) return 4;
        if (dawnCivil != null && !pt.isBefore(dawnCivil)) return 3;
        if (dawnNaut != null && !pt.isBefore(dawnNaut)) return 2;
        if (dawnAstro != null && !pt.isBefore(dawnAstro)) return 1;

        return 0;
    }

    private TrackPointDto mapToDto(TrackPoint point, int dayPhase) {
        Weather w = point.getWeather();
        double lat = point.getLatitude();
        double lon = point.getLongitude();
        double segmentId = point.getSegmentId() != null ? point.getSegmentId() : 0.0;
        double timeMs = point.getTime() != null ? (double) point.getTime().toEpochMilli() : 0.0;

        if (w == null) {
            return new TrackPointDto(lat, lon, segmentId, timeMs, dayPhase);
        }

        return new TrackPointDto(
                lat, lon, segmentId, timeMs, dayPhase,
                w.getWindSpeed(), w.getTemp(), w.getWindGusts(), w.getDewPoint(), w.getRain(),
                w.getHumidity(), w.getPressure(), w.getCloudCover(), w.getCloudCoverLow(),
                w.getCloudCoverMid(), w.getCloudCoverHigh(), w.getWindDir(), w.getSnowfall(),
                w.getWaveHeight(), w.getWavePeriod(), w.getWaveDirection(), w.getWindWaveHeight(),
                w.getWindWavePeriod(), w.getSwellWaveHeight(), w.getSwellWavePeriod(),
                w.getOceanCurrentVelocity(), w.getSeaTemperature(), w.getOceanCurrentDirection(), null, w.getWeatherCode()
        );
    }

    private List<TrackPoint> filterPointsForMap(List<TrackPoint> originalPoints) {
        if (originalPoints == null || originalPoints.isEmpty()) {
            return originalPoints;
        }

        int MAX_POINTS_FOR_MAP = 6000;
        if (originalPoints.size() <= MAX_POINTS_FOR_MAP) {
            return originalPoints;
        }

        List<TrackPoint> filteredPoints = new ArrayList<>(MAX_POINTS_FOR_MAP);
        Map<Integer, List<TrackPoint>> segments = new LinkedHashMap<>();
        for (TrackPoint pt : originalPoints) {
            int segId = pt.getSegmentId() != null ? pt.getSegmentId() : 0;
            segments.computeIfAbsent(segId, k -> new ArrayList<>()).add(pt);
        }

        for (List<TrackPoint> segPoints : segments.values()) {
            if (segPoints.size() < 2) {
                filteredPoints.addAll(segPoints);
                continue;
            }

            int targetSize = (int) Math.round((double) segPoints.size() / originalPoints.size() * MAX_POINTS_FOR_MAP);
            targetSize = Math.max(2, targetSize);

            if (segPoints.size() <= targetSize) {
                filteredPoints.addAll(segPoints);
            } else {
                double step = (double) (segPoints.size() - 1) / (targetSize - 1);
                for (int i = 0; i < targetSize; i++) {
                    int index = (int) Math.round(i * step);
                    index = Math.min(index, segPoints.size() - 1);
                    filteredPoints.add(segPoints.get(index));
                }
            }
        }
        return filteredPoints;
    }

    private void updateRanges(Map<String, double[]> ranges, String key, Number value) {
        if (value == null || Double.isNaN(value.doubleValue())) return;

        ranges.computeIfAbsent(key, k -> new double[]{Double.MAX_VALUE, -Double.MAX_VALUE});
        double[] minMax = ranges.get(key);
        double val = value.doubleValue();

        if (val < minMax[0]) minMax[0] = val;
        if (val > minMax[1]) minMax[1] = val;
    }
}