package jasinski.pawel.weather_visualization.utils;

import jasinski.pawel.weather_visualization.dto.DayData;
import jasinski.pawel.weather_visualization.dto.TimelineEvent;
import jasinski.pawel.weather_visualization.entity.TrackPoint;

import java.time.*;
import java.util.*;

import static jasinski.pawel.weather_visualization.utils.GeoUtils.calculateDistance;

public class MovementAnalyzer {

    private static final double SPEED_THRESHOLD_START_MOVING_KMH = 0.8;
    private static final double SPEED_THRESHOLD_STOP_MOVING_KMH = 0.4;
    private static final double MIN_MOVE_DISTANCE_METERS = 80.0;
    private static final long MIN_STOP_DURATION_SECONDS = 60;
    private static final double MIN_GAP_DISTANCE_METERS = 2000.0;
    private static final long MIN_GAP_DURATION_SECONDS = 60 * 45;

    public static Map<LocalDate, DayData> analyzeTripTimeline(List<TrackPoint> allPoints, ZoneId zoneId) {
        if (allPoints == null || allPoints.isEmpty()) {
            return new TreeMap<>();
        }

        List<TimelineEvent> events = new ArrayList<>();

        String currentState = null;
        Instant phaseStart = allPoints.get(0).getTime();
        double phaseLat = allPoints.get(0).getLatitude();
        double phaseLon = allPoints.get(0).getLongitude();

        TrackPoint lastStopPoint = null;
        TrackPoint potentialStopStartPoint = null;

        for (int i = 0; i < allPoints.size() - 1; i++) {
            TrackPoint p1 = allPoints.get(i);
            TrackPoint p2 = allPoints.get(i + 1);

            long timeGap = Duration.between(p1.getTime(), p2.getTime()).abs().getSeconds();
            double distance = calculateDistance(p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude());

            if (timeGap >= MIN_GAP_DURATION_SECONDS && distance >= MIN_GAP_DISTANCE_METERS) {
                if (!phaseStart.equals(p1.getTime())) {
                    String stateToSave = (currentState != null) ? currentState : "RUCH";
                    events.add(new TimelineEvent(stateToSave, phaseStart, p1.getTime(), phaseLat, phaseLon, null));
                }

                events.add(new TimelineEvent("BRAK DANYCH", p1.getTime(), p2.getTime(), p1.getLatitude(), p1.getLongitude(), null));

                currentState = null;
                phaseStart = p2.getTime();
                phaseLat = p2.getLatitude();
                phaseLon = p2.getLongitude();
                potentialStopStartPoint = null;
                lastStopPoint = null;
                continue;
            }

            double speedKmh = 0.0;
            if (p2.getSpeed() != null) {
                speedKmh = p2.getSpeed();
            }

            if (currentState == null) {
                if (speedKmh >= SPEED_THRESHOLD_START_MOVING_KMH) {
                    currentState = "RUCH";
                } else if (speedKmh < SPEED_THRESHOLD_STOP_MOVING_KMH) {
                    currentState = "POSTÓJ";
                    lastStopPoint = p1;
                }
                continue;
            }

            if (currentState.equals("RUCH")) {
                if (speedKmh < SPEED_THRESHOLD_STOP_MOVING_KMH) {
                    if (potentialStopStartPoint == null) {
                        potentialStopStartPoint = p1;
                    }

                    long lowSpeedDuration = Duration.between(potentialStopStartPoint.getTime(), p2.getTime()).getSeconds();

                    if (lowSpeedDuration >= MIN_STOP_DURATION_SECONDS) {
                        events.add(new TimelineEvent("RUCH", phaseStart, potentialStopStartPoint.getTime(), phaseLat, phaseLon, null));

                        currentState = "POSTÓJ";
                        phaseStart = potentialStopStartPoint.getTime();
                        lastStopPoint = potentialStopStartPoint;
                        phaseLat = lastStopPoint.getLatitude();
                        phaseLon = lastStopPoint.getLongitude();
                        potentialStopStartPoint = null;
                    }
                } else {
                    potentialStopStartPoint = null;
                }

            } else {
                if (speedKmh >= SPEED_THRESHOLD_START_MOVING_KMH && lastStopPoint != null) {
                    double distFromStop = calculateDistance(lastStopPoint.getLatitude(), lastStopPoint.getLongitude(), p2.getLatitude(), p2.getLongitude());

                    if (distFromStop >= MIN_MOVE_DISTANCE_METERS) {
                        events.add(new TimelineEvent("POSTÓJ", phaseStart, p1.getTime(), phaseLat, phaseLon, null));

                        currentState = "RUCH";
                        phaseStart = p1.getTime();
                        phaseLat = p1.getLatitude();
                        phaseLon = p1.getLongitude();
                        lastStopPoint = null;
                        potentialStopStartPoint = null;
                    }
                }
            }
        }

        TrackPoint lastPoint = allPoints.get(allPoints.size() - 1);
        if (!phaseStart.equals(lastPoint.getTime())) {
            String finalState = (currentState != null) ? currentState : "RUCH";
            events.add(new TimelineEvent(finalState, phaseStart, lastPoint.getTime(), phaseLat, phaseLon, null));
        }

        List<TimelineEvent> cleanedTimeline = mergeAdjacentPhases(events);
        return splitByMidnight(cleanedTimeline, zoneId);
    }

    private static List<TimelineEvent> mergeAdjacentPhases(List<TimelineEvent> timeline) {
        List<TimelineEvent> merged = new ArrayList<>();
        for (TimelineEvent ev : timeline) {
            if (merged.isEmpty()) {
                merged.add(ev);
            } else {
                TimelineEvent last = merged.get(merged.size() - 1);
                if (last.type().equals(ev.type())) {
                    merged.set(merged.size() - 1, new TimelineEvent(last.type(), last.start(), ev.end(), last.lat(), last.lon(), last.placeName()));
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
                if (nextMidnight.toInstant().isBefore(splitEnd)) splitEnd = nextMidnight.toInstant();

                LocalDate date = zStart.toLocalDate();
                dailyData.computeIfAbsent(date, DayData::new)
                        .addEvent(new TimelineEvent(event.type(), currentStart, splitEnd, event.lat(), event.lon(), event.placeName()));
                currentStart = splitEnd;
            }
        }
        return dailyData;
    }
}