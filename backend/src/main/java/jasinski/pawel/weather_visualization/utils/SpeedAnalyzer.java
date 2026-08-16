package jasinski.pawel.weather_visualization.utils;

import jasinski.pawel.weather_visualization.dto.EnrichedSegment;
import jasinski.pawel.weather_visualization.dto.SpeedStats;
import jasinski.pawel.weather_visualization.entity.TrackPoint;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SpeedAnalyzer {

    public static SpeedStats calculateSpeed(List<EnrichedSegment> segments) {
        if (segments == null || segments.isEmpty()) {
            return new SpeedStats(null, null, 0.0);
        }

        double totalMovingDistanceMeters = 0.0;
        double totalDistanceMeters = 0.0;
        double totalMovingSeconds = 0.0;
        double maxSpeed = 0.0;

        for (EnrichedSegment seg : segments) {
            Double dist = seg.distanceMeters();
            if (dist != null) {

                totalDistanceMeters += dist;

                if (seg.isMoving()) {
                    totalMovingDistanceMeters += dist;
                    totalMovingSeconds += seg.durationSeconds();

                    Double speed = seg.rawSpeedKmh();
                    if (speed != null && speed > maxSpeed) {
                        maxSpeed = speed;
                    }
                }
            }
        }

        Double avgSpeed = null;
        if (totalMovingSeconds > 0) {
            avgSpeed = roundToTwoDecimals((totalMovingDistanceMeters / totalMovingSeconds) * 3.6);
        }

        Double finalMaxSpeed = maxSpeed > 0 ? roundToTwoDecimals(maxSpeed) : null;
        Double distanceKm = roundToTwoDecimals(totalDistanceMeters / 1000.0);

        return new SpeedStats(finalMaxSpeed, avgSpeed, distanceKm);
    }


    public static boolean isAnomaly(List<Double> speeds, List<Double> durations, int index) {
        if (speeds == null || durations == null || index < 0 || index >= speeds.size()) {
            return false;
        }

        Double currentSpeedOpt = speeds.get(index);
        if (currentSpeedOpt == null) {
            return false;
        }
        double currentSpeed = currentSpeedOpt;

        double timeWindowSec = 30.0;
        double maxAccel = 15.0;
        double multiplier = 2.0;
        double minDiff = 8.0;

        double[] neighborhood = new double[100];
        int count = 0;

        double accumulatedTimeBack = 0.0;
        for (int i = index - 1; i >= 0; i--) {
            Double d = (i < durations.size()) ? durations.get(i) : null;
            if (d != null) accumulatedTimeBack += d;

            if (accumulatedTimeBack > timeWindowSec) break;

            if (speeds.get(i) != null) {
                if (count == neighborhood.length) {
                    neighborhood = Arrays.copyOf(neighborhood, neighborhood.length * 2);
                }
                neighborhood[count++] = speeds.get(i);
            }
        }

        double accumulatedTimeForward = 0.0;
        for (int i = index; i < speeds.size() - 1; i++) {
            Double d = (i < durations.size()) ? durations.get(i) : null;
            if (d != null) accumulatedTimeForward += d;

            if (accumulatedTimeForward > timeWindowSec) break;

            if (speeds.get(i + 1) != null) {
                if (count == neighborhood.length) {
                    neighborhood = Arrays.copyOf(neighborhood, neighborhood.length * 2);
                }
                neighborhood[count++] = speeds.get(i + 1);
            }
        }

        if (count == 0) return false;
        Arrays.sort(neighborhood, 0, count);
        double localMedian;
        if (count % 2 == 0) {
            localMedian = (neighborhood[count / 2 - 1] + neighborhood[count / 2]) / 2.0;
        } else {
            localMedian = neighborhood[count / 2];
        }

        double safeMedian = Math.max(localMedian, 1.0);

        if (index > 0 && index - 1 < durations.size()) {
            Double prevDuration = durations.get(index - 1);
            Double prevSpeed = speeds.get(index - 1);

            if (prevDuration != null && prevDuration > 0 && prevSpeed != null) {
                double accel = Math.abs(currentSpeed - prevSpeed) / prevDuration;
                if (accel > maxAccel && currentSpeed > safeMedian * 1.5) {
                    return true;
                }
            }
        }

        if (currentSpeed > safeMedian * multiplier && (currentSpeed - safeMedian) > minDiff) {
            return true;
        }

        return false;
    }

    private static double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}