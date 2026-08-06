package jasinski.pawel.weather_visualization.service;

import jasinski.pawel.weather_visualization.dto.AstronomyMarkerDto;
import jasinski.pawel.weather_visualization.dto.AstronomyStats;
import jasinski.pawel.weather_visualization.dto.MapDataResponse;
import jasinski.pawel.weather_visualization.dto.TrackPointDto;
import jasinski.pawel.weather_visualization.entity.TrackPoint;
import jasinski.pawel.weather_visualization.entity.Weather;
import jasinski.pawel.weather_visualization.repository.TrackPointRepository;
import jasinski.pawel.weather_visualization.utils.AstronomyAnalyzer;
import jasinski.pawel.weather_visualization.utils.GeoUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.util.*;

@Service
public class TripMapService {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Warsaw");
    private static final DateTimeFormatter MARKER_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm");
    private static final double MIN_DISTANCE_METERS = 15.0;
    private static final long MIN_TIME_SECONDS = 15;

    private final TrackPointRepository trackPointRepository;

    public TripMapService(TrackPointRepository trackPointRepository) {
        this.trackPointRepository = trackPointRepository;
    }

    public MapDataResponse getTripMapData(Long tripId) {
        List<TrackPoint> rawPoints = trackPointRepository.findByTripIdOrderByTimeAsc(tripId);
        if (rawPoints.isEmpty()) return new MapDataResponse(List.of(), List.of(), Map.of());

        List<TrackPoint> points = filterPointsForMap(rawPoints, MIN_DISTANCE_METERS, MIN_TIME_SECONDS);
        System.out.println("Optymalizacja mapy: Zredukowano z " + rawPoints.size() + " do " + points.size() + " punktów.");



        Map<LocalDate, List<TrackPoint>> pointsByDay = groupPointsByDay(points);
        Map<LocalDate, List<TrackPoint>> rawPointsByDay = groupPointsByDay(rawPoints);
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
            AstronomyStats astro = AstronomyAnalyzer.calculateSun(rawDayPoints, rawPoints, null, DEFAULT_ZONE);

            if (firstDayAstro == null) {
                firstDayAstro = astro;
                firstDate = currentDate;
            }

            // Dodawanie znaczników zjawisk
            addMarkerIfPresent(markers, "ŚWIT ASTRONOMICZNY", astro.astronomicalDawnPt(), astro.astronomicalDawn());
            addMarkerIfPresent(markers, "ŚWIT NAUTYCZNY", astro.nauticalDawnPt(), astro.nauticalDawn());
            addMarkerIfPresent(markers, "ŚWIT CYWILNY", astro.civilDawnPt(), astro.civilDawn());
            addMarkerIfPresent(markers, "WSCHÓD", astro.sunrisePt(), astro.sunrise());
            addMarkerIfPresent(markers, "KULMINACJA SŁOŃCA", astro.noonPt(), astro.solarNoon());
            addMarkerIfPresent(markers, "ZACHÓD", astro.sunsetPt(), astro.sunset());
            addMarkerIfPresent(markers, "ZMIERZCH CYWILNY", astro.civilDuskPt(), astro.civilDusk());
            addMarkerIfPresent(markers, "ZMIERZCH NAUTYCZNY", astro.nauticalDuskPt(), astro.nauticalDusk());
            addMarkerIfPresent(markers, "ZMIERZCH ASTRONOMICZNY", astro.astronomicalDuskPt(), astro.astronomicalDusk());
            addMarkerIfPresent(markers, "WSCHÓD KSIĘŻYCA", astro.moonRisePt(), astro.moonRise());
            addMarkerIfPresent(markers, "ZACHÓD KSIĘŻYCA", astro.moonSetPt(), astro.moonSet());

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

    private Map<LocalDate, List<TrackPoint>> groupPointsByDay(List<TrackPoint> points) {
        Map<LocalDate, List<TrackPoint>> pointsByDay = new TreeMap<>();
        for (TrackPoint point : points) {
            LocalDate date = LocalDate.ofInstant(point.getTime(), DEFAULT_ZONE);
            pointsByDay.computeIfAbsent(date, k -> new ArrayList<>()).add(point);
        }
        return pointsByDay;
    }

    private void addMarkerIfPresent(List<AstronomyMarkerDto> markers, String label, TrackPoint pt, Instant exactTime) {
        if (pt != null && exactTime != null) {
            String formattedTime = LocalDateTime.ofInstant(exactTime, DEFAULT_ZONE).format(MARKER_FMT);
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

    private List<TrackPoint> filterPointsForMap(List<TrackPoint> originalPoints, double minDistanceMeters, long minTimeSeconds) {
        if (originalPoints == null || originalPoints.isEmpty()) {
            return originalPoints;
        }

        Map<Integer, List<TrackPoint>> segments = new LinkedHashMap<>();
        for (TrackPoint pt : originalPoints) {
            int segId = pt.getSegmentId() != null ? pt.getSegmentId() : 0;
            segments.computeIfAbsent(segId, k -> new ArrayList<>()).add(pt);
        }

        List<TrackPoint> phase1Filtered = new ArrayList<>();

        for (List<TrackPoint> segPoints : segments.values()) {
            if (segPoints.isEmpty()) continue;

            TrackPoint lastKeptPoint = segPoints.get(0);
            phase1Filtered.add(lastKeptPoint);

            for (int i = 1; i < segPoints.size(); i++) {
                TrackPoint currentPoint = segPoints.get(i);
                double distance = GeoUtils.calculateDistance(lastKeptPoint.getLatitude(), lastKeptPoint.getLongitude(), currentPoint.getLatitude(), currentPoint.getLongitude());
                long timeGap = java.time.Duration.between(lastKeptPoint.getTime(), currentPoint.getTime()).abs().getSeconds();

                if (distance >= minDistanceMeters || timeGap >= minTimeSeconds || i == segPoints.size() - 1) {
                    phase1Filtered.add(currentPoint);
                    lastKeptPoint = currentPoint;
                }
            }
        }

        int MAX_POINTS_FOR_MAP = 2500;
        if (phase1Filtered.size() <= MAX_POINTS_FOR_MAP) {
            return phase1Filtered;
        }

        List<TrackPoint> heavilyFiltered = new ArrayList<>(MAX_POINTS_FOR_MAP);
        Map<Integer, List<TrackPoint>> filteredSegments = new LinkedHashMap<>();
        for (TrackPoint pt : phase1Filtered) {
            int segId = pt.getSegmentId() != null ? pt.getSegmentId() : 0;
            filteredSegments.computeIfAbsent(segId, k -> new ArrayList<>()).add(pt);
        }

        for (List<TrackPoint> segPoints : filteredSegments.values()) {
            if (segPoints.size() < 2) {
                continue;
            }

            int targetSize = (int) Math.round((double) segPoints.size() / phase1Filtered.size() * MAX_POINTS_FOR_MAP);
            targetSize = Math.max(2, targetSize);

            if (segPoints.size() <= targetSize) {
                heavilyFiltered.addAll(segPoints);
            } else {
                double step = (double) (segPoints.size() - 1) / (targetSize - 1);
                for (int i = 0; i < targetSize; i++) {
                    int index = (int) Math.round(i * step);
                    index = Math.min(index, segPoints.size() - 1);
                    heavilyFiltered.add(segPoints.get(index));
                }
            }
        }
        return heavilyFiltered;
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