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
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
        if (rawPoints.isEmpty()) return new MapDataResponse(List.of(), List.of());

        List<TrackPoint> points = filterPointsForMap(rawPoints, MIN_DISTANCE_METERS, MIN_TIME_SECONDS);
        System.out.println("Optymalizacja mapy: Zredukowano z " + rawPoints.size() + " do " + points.size() + " punktów.");


        Map<LocalDate, List<TrackPoint>> pointsByDay = groupPointsByDay(points);
        List<TrackPointDto> route = new ArrayList<>();
        List<AstronomyMarkerDto> markers = new ArrayList<>();

        for (Map.Entry<LocalDate, List<TrackPoint>> entry : pointsByDay.entrySet()) {
            LocalDate currentDate = entry.getKey();
            List<TrackPoint> dayPoints = entry.getValue();

            AstronomyStats astro = AstronomyAnalyzer.calculateSun(dayPoints, null, DEFAULT_ZONE);

            // Dodawanie znaczników zjawisk
            addMarkerIfPresent(markers, "ŚWIT ASTRONOMICZNY", astro.astronomicalDawnPt(), astro.astronomicalDawn(), currentDate);
            addMarkerIfPresent(markers, "ŚWIT NAUTYCZNY", astro.nauticalDawnPt(), astro.nauticalDawn(), currentDate);
            addMarkerIfPresent(markers, "ŚWIT CYWILNY", astro.civilDawnPt(), astro.civilDawn(), currentDate);
            addMarkerIfPresent(markers, "WSCHÓD", astro.sunrisePt(), astro.sunrise(), currentDate);
            addMarkerIfPresent(markers, "KULMINACJA", astro.noonPt(), astro.solarNoon(), currentDate);
            addMarkerIfPresent(markers, "ZACHÓD", astro.sunsetPt(), astro.sunset(), currentDate);
            addMarkerIfPresent(markers, "ZMIERZCH CYWILNY", astro.civilDuskPt(), astro.civilDusk(), currentDate);
            addMarkerIfPresent(markers, "ZMIERZCH NAUTYCZNY", astro.nauticalDuskPt(), astro.nauticalDusk(), currentDate);
            addMarkerIfPresent(markers, "ZMIERZCH ASTRONOMICZNY", astro.astronomicalDuskPt(), astro.astronomicalDusk(), currentDate);
            addMarkerIfPresent(markers, "WSCHÓD KSIĘŻYCA", astro.moonRisePt(), astro.moonRise(), currentDate);
            addMarkerIfPresent(markers, "ZACHÓD KSIĘŻYCA", astro.moonSetPt(), astro.moonSet(), currentDate);

            // Mapowanie punktów i wyliczanie fazy dnia
            for (TrackPoint point : dayPoints) {
                int phase = calculateDayPhase(point, astro);
                route.add(mapToDto(point, phase));
            }
        }

        return new MapDataResponse(route, markers);
    }

    private Map<LocalDate, List<TrackPoint>> groupPointsByDay(List<TrackPoint> points) {
        Map<LocalDate, List<TrackPoint>> pointsByDay = new TreeMap<>();
        for (TrackPoint point : points) {
            LocalDate date = LocalDate.ofInstant(point.getTime(), DEFAULT_ZONE);
            pointsByDay.computeIfAbsent(date, k -> new ArrayList<>()).add(point);
        }
        return pointsByDay;
    }

    private void addMarkerIfPresent(List<AstronomyMarkerDto> markers, String label, TrackPoint pt, LocalTime time, LocalDate date) {
        if (pt != null && time != null) {
            String formattedTime = LocalDateTime.of(date, time).format(MARKER_FMT);
            markers.add(new AstronomyMarkerDto(label, pt.getLocation().getY(), pt.getLocation().getX(), formattedTime));
        }
    }

    private int calculateDayPhase(TrackPoint point, AstronomyStats astro) {
        LocalTime ptTime = LocalTime.ofInstant(point.getTime(), DEFAULT_ZONE);

        LocalTime eSunrise = extractRefTime(astro.sunrisePt(), astro.sunrise());
        LocalTime eSunset = extractRefTime(astro.sunsetPt(), astro.sunset());

        if (eSunrise == null || eSunset == null) return 4;

        if (ptTime.isBefore(eSunrise)) {
            if (isBeforeEvent(ptTime, astro.astronomicalDawnPt(), astro.astronomicalDawn())) return 0;
            if (isBeforeEvent(ptTime, astro.nauticalDawnPt(), astro.nauticalDawn())) return 1;
            if (isBeforeEvent(ptTime, astro.civilDawnPt(), astro.civilDawn())) return 2;
            return 3;
        } else if (!ptTime.isBefore(eSunset)) {
            if (isAfterOrEqualEvent(ptTime, astro.astronomicalDuskPt(), astro.astronomicalDusk())) return 8;
            if (isAfterOrEqualEvent(ptTime, astro.nauticalDuskPt(), astro.nauticalDusk())) return 7;
            if (isAfterOrEqualEvent(ptTime, astro.civilDuskPt(), astro.civilDusk())) return 6;
            return 5;
        }

        return 4;
    }

    private LocalTime extractRefTime(TrackPoint pt, LocalTime fallbackTime) {
        return pt != null ? LocalTime.ofInstant(pt.getTime(), DEFAULT_ZONE) : fallbackTime;
    }

    private boolean isBeforeEvent(LocalTime ptTime, TrackPoint eventPt, LocalTime fallbackTime) {
        LocalTime refTime = extractRefTime(eventPt, fallbackTime);
        return refTime != null && ptTime.isBefore(refTime);
    }

    private boolean isAfterOrEqualEvent(LocalTime ptTime, TrackPoint eventPt, LocalTime fallbackTime) {
        LocalTime refTime = extractRefTime(eventPt, fallbackTime);
        return refTime != null && !ptTime.isBefore(refTime);
    }

    private TrackPointDto mapToDto(TrackPoint point, int dayPhase) {
        Weather w = point.getWeather();
        double lat = point.getLocation().getY();
        double lon = point.getLocation().getX();
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
                w.getOceanCurrentVelocity(), w.getSeaTemperature(), w.getOceanCurrentDirection(), null
        );
    }

    private List<TrackPoint> filterPointsForMap(List<TrackPoint> originalPoints, double minDistanceMeters, long minTimeSeconds) {
        if (originalPoints == null || originalPoints.isEmpty()) {
            return originalPoints;
        }

        List<TrackPoint> filtered = new ArrayList<>();

        TrackPoint lastKeptPoint = originalPoints.get(0);
        filtered.add(lastKeptPoint);

        for (int i = 1; i < originalPoints.size(); i++) {
            TrackPoint currentPoint = originalPoints.get(i);

            double distance = GeoUtils.calculateDistance(lastKeptPoint.getLocation(), currentPoint.getLocation());
            long timeGap = java.time.Duration.between(lastKeptPoint.getTime(), currentPoint.getTime()).abs().getSeconds();

            if (distance >= minDistanceMeters || timeGap >= minTimeSeconds || i == originalPoints.size() - 1) {
                filtered.add(currentPoint);
                lastKeptPoint = currentPoint;
            }
        }
        return filtered;
    }
}