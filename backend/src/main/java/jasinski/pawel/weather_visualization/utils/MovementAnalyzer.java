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
    private static final long DISPLACEMENT_WINDOW_SECONDS = 120;
    private static final double MIN_NET_DISPLACEMENT_METERS = 80;
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
        double[] netDisplacements = calculateNetDisplacements(allPoints, DISPLACEMENT_WINDOW_SECONDS);
        boolean[] movingStates = calculateMovementStates(smoothedSpeeds, netDisplacements);

        int n = allPoints.size() - 1;
        int i = 0;


        while (i < n) {
            TrackPoint startPoint = allPoints.get(i);
            TrackPoint nextPoint = allPoints.get(i + 1);

            long gapSec = Math.abs(Duration.between(startPoint.getTime(), nextPoint.getTime()).getSeconds());
            double gapDist = calculateDistance(startPoint.getLatitude(),startPoint.getLongitude(), nextPoint.getLatitude(), nextPoint.getLongitude());

            if (gapSec >= MIN_GAP_DURATION_SECONDS && gapDist >= MIN_GAP_DISTANCE_METERS) {
                rawEvents.add(new TimelineEvent("BRAK DANYCH", startPoint.getTime(), nextPoint.getTime(), startPoint.getLatitude(), startPoint.getLongitude(), null));
                i++;
                continue;
            }

            boolean isMovingBlock = movingStates[i];
            TrackPoint anchor = isMovingBlock ? null : startPoint;

            int j = i;
            while (j < n) {
                boolean nextStepIsMoving = movingStates[j];

                TrackPoint currentJ = allPoints.get(j);
                TrackPoint nextJ = allPoints.get(j + 1);
                long nextGapSec = Math.abs(Duration.between(currentJ.getTime(), nextJ.getTime()).getSeconds());
                double nextGapDist = calculateDistance(currentJ.getLatitude(),currentJ.getLongitude(), nextJ.getLatitude(), nextJ.getLongitude());

                if (nextStepIsMoving != isMovingBlock || (nextGapSec >= MIN_GAP_DURATION_SECONDS && nextGapDist >= MIN_GAP_DISTANCE_METERS)) {
                    break;
                }
                j++;
            }


            TrackPoint endPoint = allPoints.get(j);
            String type = isMovingBlock ? "RUCH" : "POSTÓJ";
            double lat = anchor != null ? anchor.getLatitude() : 0.0;
            double lon = anchor != null ? anchor.getLongitude() : 0.0;

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
            dists[i] = calculateDistance(points.get(i).getLatitude(),points.get(i).getLongitude(), points.get(i+1).getLatitude(), points.get(i+1).getLongitude());
            durs[i] = Math.abs(Duration.between(points.get(i).getTime(), points.get(i+1).getTime()).getSeconds());
        }

        for (int i = 0; i < n; i++) {

            Double gpxSpeed = points.get(i + 1).getSpeed();
            if (gpxSpeed != null) {
                speeds[i] = gpxSpeed;
                continue;
            }

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

    //liczy realne przemieszczenie między początkiem okna a końcem pomijając np. dryfowanie
    private static double[] calculateNetDisplacements(List<TrackPoint> points, long windowSeconds) {
        int n = points.size() - 1;
        double[] net = new double[n];

        for (int i = 0; i < n; i++) {
            int left = i;
            long accLeft = 0;
            while (left > 0) {
                long segDur = Math.abs(Duration.between(points.get(left - 1).getTime(), points.get(left).getTime()).getSeconds());
                if (segDur > 120 || accLeft + segDur > windowSeconds / 2) break;
                accLeft += segDur;
                left--;
            }

            int right = i + 1;
            long accRight = 0;
            while (right < n) {
                long segDur = Math.abs(Duration.between(points.get(right).getTime(), points.get(right + 1).getTime()).getSeconds());
                if (segDur > 120 || accRight + segDur > windowSeconds / 2) break;
                accRight += segDur;
                right++;
            }

            TrackPoint a = points.get(left);
            TrackPoint b = points.get(right);
            net[i] = calculateDistance(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
        }

        return net;
    }

    //klasyfikuje każdy punkt jako ruch(true) lub postój(false)
    private static boolean[] calculateMovementStates(double[] speeds, double[] netDisplacements) {
        boolean[] states = new boolean[speeds.length];
        boolean currentlyMoving = false;

        for (int i = 0; i < speeds.length; i++) {
            if (currentlyMoving) {
                if (speeds[i] < SPEED_THRESHOLD_STOP_MOVING_KMH) {
                    currentlyMoving = false;
                }
            } else {
                boolean fastEnough = speeds[i] >= SPEED_THRESHOLD_START_MOVING_KMH;
                boolean actuallyProgressing = netDisplacements[i] >= MIN_NET_DISPLACEMENT_METERS;
                if (fastEnough && actuallyProgressing) {
                    currentlyMoving = true;
                }
            }
            states[i] = currentlyMoving;
        }

        return states;
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

}