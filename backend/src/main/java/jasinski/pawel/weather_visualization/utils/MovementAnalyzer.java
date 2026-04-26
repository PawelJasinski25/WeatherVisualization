package jasinski.pawel.weather_visualization.utils;

import jasinski.pawel.weather_visualization.dto.DayData;
import jasinski.pawel.weather_visualization.dto.TimelineEvent;
import jasinski.pawel.weather_visualization.entity.TrackPoint;
import org.locationtech.jts.geom.Point;

import java.time.*;
import java.util.*;

public class MovementAnalyzer {
    private static final double STOP_RADIUS_METERS = 50.0;
    private static final long MIN_STOP_DURATION_SECONDS = 3 * 60;
    private static final double MIN_GAP_DISTANCE_METERS = 2000;
    private static final long MIN_GAP_DURATION_SECONDS = 60 * 90;
    private static final long MIN_MOVE_DURATION_SECONDS = 90;


    public static Map<LocalDate, DayData> analyzeTripTimeline(List<TrackPoint> allPoints, ZoneId zoneId) {
        if (allPoints == null || allPoints.isEmpty()) {
            return new TreeMap<>();
        }

        List<TimelineEvent> knownEvents = new ArrayList<>();
        knownEvents.addAll(findStops(allPoints));
        knownEvents.addAll(findGaps(allPoints));
        knownEvents.sort(Comparator.comparing(TimelineEvent::start));

        // Puste przestrzenie wypelniam fazą "RUCH"
        List<TimelineEvent> continuousTimeline = fillMovementPhases(knownEvents, allPoints);

        // Przekształcam bardzo krótkie ruchy w postój
        List<TimelineEvent> cleanedTimeline = filterMicroMovements(continuousTimeline);

        // Łączenie sąsiadujących postoji
        List<TimelineEvent> mergedTimeline = mergeAdjacentPhases(cleanedTimeline);

        return splitByMidnight(mergedTimeline, zoneId);
    }


    private static List<TimelineEvent> findStops(List<TrackPoint> allPoints) {
        List<TimelineEvent> stops = new ArrayList<>();
        int i = 0;

        while (i < allPoints.size()) {
            TrackPoint startPoint = allPoints.get(i);
            int j = i + 1;
            TrackPoint lastInsidePoint = startPoint;

            while (j < allPoints.size()) {
                TrackPoint currPoint = allPoints.get(j);
                double dist = calculateDistance(startPoint.getLocation(), currPoint.getLocation());
                if (dist <= STOP_RADIUS_METERS) {
                    lastInsidePoint = currPoint;
                    j++;
                } else {
                    break;
                }
            }

            long durationSeconds = Math.abs(Duration.between(startPoint.getTime(), lastInsidePoint.getTime()).getSeconds());
            if (durationSeconds >= MIN_STOP_DURATION_SECONDS) {
                stops.add(new TimelineEvent("POSTÓJ", startPoint.getTime(), lastInsidePoint.getTime(), startPoint.getLocation().getY(), startPoint.getLocation().getX()));
                i = j - 1;
                if (i == allPoints.indexOf(startPoint)) i++;
            } else {
                i++;
            }
        }
        return stops;
    }

    private static List<TimelineEvent> findGaps(List<TrackPoint> allPoints) {
        List<TimelineEvent> gaps = new ArrayList<>();

        for (int i = 0; i < allPoints.size() - 1; i++) {
            TrackPoint p1 = allPoints.get(i);
            TrackPoint p2 = allPoints.get(i + 1);

            long dur = Math.abs(Duration.between(p1.getTime(), p2.getTime()).getSeconds());
            double dist = calculateDistance(p1.getLocation(), p2.getLocation());

            if (dur >= MIN_GAP_DURATION_SECONDS && dist >= MIN_GAP_DISTANCE_METERS) {
                gaps.add(new TimelineEvent("BRAK DANYCH", p1.getTime(), p2.getTime(), p1.getLocation().getY(), p1.getLocation().getX()));
            }
        }
        return gaps;
    }

    private static List<TimelineEvent> fillMovementPhases(List<TimelineEvent> knownEvents, List<TrackPoint> allPoints) {
        List<TimelineEvent> continuousTimeline = new ArrayList<>();
        Instant currentTime = allPoints.get(0).getTime();

        for (TimelineEvent event : knownEvents) {
            if (event.start().isAfter(currentTime)) {
                continuousTimeline.add(new TimelineEvent("RUCH", currentTime, event.start(), 0, 0));
            }

            if (event.end().isAfter(currentTime)) {
                Instant actualStart = event.start().isBefore(currentTime) ? currentTime : event.start();
                continuousTimeline.add(new TimelineEvent(event.type(), actualStart, event.end(), event.lat(), event.lon()));
                currentTime = event.end();
            }
        }

        Instant tripEnd = allPoints.get(allPoints.size() - 1).getTime();
        if (currentTime.isBefore(tripEnd)) {
            continuousTimeline.add(new TimelineEvent("RUCH", currentTime, tripEnd, 0, 0));
        }
        return continuousTimeline;
    }


    private static List<TimelineEvent> filterMicroMovements(List<TimelineEvent> timeline) {
        List<TimelineEvent> filtered = new ArrayList<>();

        for (int i = 0; i < timeline.size(); i++) {
            TimelineEvent ev = timeline.get(i);

            if ("RUCH".equals(ev.type()) && ev.durationSeconds() < MIN_MOVE_DURATION_SECONDS) {

                double lat = 0;
                double lon = 0;

                if (i > 0 && "POSTÓJ".equals(timeline.get(i - 1).type())) {
                    lat = timeline.get(i - 1).lat();
                    lon = timeline.get(i - 1).lon();
                } else if (i < timeline.size() - 1 && "POSTÓJ".equals(timeline.get(i + 1).type())) {
                    lat = timeline.get(i + 1).lat();
                    lon = timeline.get(i + 1).lon();
                }

                filtered.add(new TimelineEvent("POSTÓJ", ev.start(), ev.end(), lat, lon));
            } else {
                filtered.add(ev);
            }
        }
        return filtered;
    }

    private static List<TimelineEvent> mergeAdjacentPhases(List<TimelineEvent> timeline) {
        List<TimelineEvent> merged = new ArrayList<>();

        for (TimelineEvent ev : timeline) {
            if (merged.isEmpty()) {
                merged.add(ev);
            } else {
                TimelineEvent last = merged.get(merged.size() - 1);

                if (last.type().equals(ev.type())) {
                    merged.remove(merged.size() - 1);
                    merged.add(new TimelineEvent(last.type(), last.start(), ev.end(), last.lat(), last.lon()));
                } else {
                    merged.add(ev);
                }
            }
        }
        return merged;
    }

    private static Map<LocalDate, DayData> splitByMidnight(List<TimelineEvent> timeline, ZoneId zoneId) {
        Map<LocalDate, DayData> dailyData = new TreeMap<>();

        for (TimelineEvent event : timeline) {
            Instant currentStart = event.start();
            Instant end = event.end();

            while (currentStart.isBefore(end)) {
                ZonedDateTime zStart = currentStart.atZone(zoneId);
                ZonedDateTime zEnd = end.atZone(zoneId);

                ZonedDateTime nextMidnight = zStart.toLocalDate().plusDays(1).atStartOfDay(zoneId);
                Instant splitEnd = zEnd.toInstant();
                if (nextMidnight.toInstant().isBefore(splitEnd)) {
                    splitEnd = nextMidnight.toInstant();
                }

                LocalDate date = zStart.toLocalDate();
                dailyData.computeIfAbsent(date, DayData::new)
                        .addEvent(new TimelineEvent(event.type(), currentStart, splitEnd, event.lat(), event.lon()));

                currentStart = splitEnd;
            }
        }
        return dailyData;
    }

    private static double calculateDistance(Point p1, Point p2) {
        double earthRadius = 6371000;
        double dLat = Math.toRadians(p2.getY() - p1.getY());
        double dLon = Math.toRadians(p2.getX() - p1.getX());
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(p1.getY())) * Math.cos(Math.toRadians(p2.getY())) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return earthRadius * c;
    }
}