package jasinski.pawel.weather_visualization.utils;

import jasinski.pawel.weather_visualization.dto.DayData;
import jasinski.pawel.weather_visualization.dto.TimelineEvent;
import jasinski.pawel.weather_visualization.entity.TrackPoint;
import org.locationtech.jts.geom.Point;

import java.time.*;
import java.util.*;

public class MovementAnalyzer {

    private static final double MOVING_SPEED_THRESHOLD_KMH = 0.7;
    private static final long SMOOTHING_WINDOW_SECONDS = 60;
    private static final long MIN_PHASE_DURATION_SECONDS = 60;

    private static final double MIN_GAP_DISTANCE_METERS = 6000;
    private static final long MIN_GAP_DURATION_SECONDS = 60 * 90;

    public static Map<LocalDate, DayData> analyzeTripTimeline(List<TrackPoint> allPoints, ZoneId zoneId) {
        if (allPoints == null || allPoints.isEmpty()) {
            return new TreeMap<>();
        }

        List<TimelineEvent> rawEvents = new ArrayList<>();
        double[] smoothedSpeeds = calculateTimeWindowSpeeds(allPoints);

        int n = allPoints.size() - 1;
        int i = 0;


        while (i < n) {
            TrackPoint startPoint = allPoints.get(i);
            TrackPoint nextPoint = allPoints.get(i + 1);

            long gapSec = Math.abs(Duration.between(startPoint.getTime(), nextPoint.getTime()).getSeconds());
            double gapDist = calculateDistance(startPoint.getLocation(), nextPoint.getLocation());

            if (gapSec >= MIN_GAP_DURATION_SECONDS && gapDist >= MIN_GAP_DISTANCE_METERS) {
                rawEvents.add(new TimelineEvent("BRAK DANYCH", startPoint.getTime(), nextPoint.getTime(), startPoint.getLocation().getY(), startPoint.getLocation().getX(), null));
                i++;
                continue;
            }

            boolean isMovingBlock = smoothedSpeeds[i] >= MOVING_SPEED_THRESHOLD_KMH;
            TrackPoint anchor = isMovingBlock ? null : startPoint;

            int j = i;
            while (j < n) {
                boolean nextStepIsMoving = smoothedSpeeds[j] >= MOVING_SPEED_THRESHOLD_KMH;

                TrackPoint currentJ = allPoints.get(j);
                TrackPoint nextJ = allPoints.get(j + 1);
                long nextGapSec = Math.abs(Duration.between(currentJ.getTime(), nextJ.getTime()).getSeconds());
                double nextGapDist = calculateDistance(currentJ.getLocation(), nextJ.getLocation());

                if (nextStepIsMoving != isMovingBlock || (nextGapSec >= MIN_GAP_DURATION_SECONDS && nextGapDist >= MIN_GAP_DISTANCE_METERS)) {
                    break;
                }
                j++;
            }


            TrackPoint endPoint = allPoints.get(j);
            String type = isMovingBlock ? "RUCH" : "POSTÓJ";
            double lat = anchor != null ? anchor.getLocation().getY() : 0.0;
            double lon = anchor != null ? anchor.getLocation().getX() : 0.0;

            rawEvents.add(new TimelineEvent(type, startPoint.getTime(), endPoint.getTime(), lat, lon, null));

            i = j;
        }

        List<TimelineEvent> cleanedTimeline = debounceTimeline(rawEvents);

        return splitByMidnight(cleanedTimeline, zoneId);
    }

    private static double[] calculateTimeWindowSpeeds(List<TrackPoint> points) {
        int n = points.size() - 1;
        double[] speeds = new double[n];


        double[] dists = new double[n];
        long[] durs = new long[n];
        for (int i = 0; i < n; i++) {
            dists[i] = calculateDistance(points.get(i).getLocation(), points.get(i+1).getLocation());
            durs[i] = Math.abs(Duration.between(points.get(i).getTime(), points.get(i+1).getTime()).getSeconds());
        }

        for (int i = 0; i < n; i++) {
            double sumDist = dists[i];
            long sumDur = durs[i];

            // porównywnanie przeszłości
            long accumulatedDur = 0;
            for (int j = i - 1; j >= 0; j--) {
                if (durs[j] > 120) break;
                accumulatedDur += durs[j];
                if (accumulatedDur > SMOOTHING_WINDOW_SECONDS) break;
                sumDist += dists[j];
                sumDur += durs[j];
            }

            // porównywanie przyszłości
            accumulatedDur = 0;
            for (int j = i + 1; j < n; j++) {
                if (durs[j] > 120) break;
                accumulatedDur += durs[j];
                if (accumulatedDur > SMOOTHING_WINDOW_SECONDS) break;
                sumDist += dists[j];
                sumDur += durs[j];
            }


            if (sumDur > 0) {
                speeds[i] = (sumDist / sumDur) * 3.6;
            } else {
                speeds[i] = 0.0;
            }
        }
        return speeds;
    }

    private static List<TimelineEvent> debounceTimeline(List<TimelineEvent> timeline) {
        if (timeline.isEmpty()) return timeline;

        List<TimelineEvent> result = new ArrayList<>(timeline);
        boolean changed = true;

        while (changed) {
            changed = false;
            for (int i = 0; i < result.size(); i++) {
                TimelineEvent ev = result.get(i);

                if ("BRAK DANYCH".equals(ev.type())) continue;

                if (ev.durationSeconds() < MIN_PHASE_DURATION_SECONDS) {

                    String dominantNeighborType = null;
                    if (i > 0 && i < result.size() - 1) {
                        String prevType = result.get(i-1).type();
                        String nextType = result.get(i+1).type();
                        if (prevType.equals(nextType) && !prevType.equals("BRAK DANYCH")) {
                            dominantNeighborType = prevType;
                        }
                    } else if (i > 0 && !result.get(i-1).type().equals("BRAK DANYCH")) {
                        dominantNeighborType = result.get(i-1).type();
                    } else if (i < result.size() - 1 && !result.get(i+1).type().equals("BRAK DANYCH")) {
                        dominantNeighborType = result.get(i+1).type();
                    }

                    if (dominantNeighborType != null && !dominantNeighborType.equals(ev.type())) {

                        double lat = 0.0;
                        double lon = 0.0;

                        if (dominantNeighborType.equals("POSTÓJ") && i > 0) {
                            lat = result.get(i-1).lat();
                            lon = result.get(i-1).lon();
                        }

                        result.set(i, new TimelineEvent(dominantNeighborType, ev.start(), ev.end(), lat, lon, ev.placeName()));
                        changed = true;
                        break;
                    }
                }
            }
            if (changed) {
                result = mergeAdjacentPhases(result);
            }
        }
        return result;
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
                    merged.add(new TimelineEvent(last.type(), last.start(), ev.end(), last.lat(), last.lon(), last.placeName()));
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