package jasinski.pawel.weather_visualization.utils;

import jasinski.pawel.weather_visualization.dto.EnrichedSegment;
import jasinski.pawel.weather_visualization.dto.SpeedStats;
import jasinski.pawel.weather_visualization.entity.TrackPoint;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class SpeedAnalyzer {

    public static SpeedStats calculateSpeed(List<EnrichedSegment> segments) {
        if (segments == null || segments.isEmpty()) {
            return new SpeedStats(null, null, 0.0);
        }

        double totalMovingDistanceMeters = 0.0;
        double totalDistanceMeters = 0.0;
        double totalMovingSeconds = 0.0;
        List<Double> validSpeeds = new ArrayList<>();

        for (EnrichedSegment seg : segments) {

            if (seg.isMoving()) {
                totalMovingDistanceMeters += seg.distanceMeters();
                totalDistanceMeters += seg.distanceMeters();
                totalMovingSeconds += seg.durationSeconds();

                double actualSpeed = (seg.p2().getSpeed() != null) ? seg.p2().getSpeed() : seg.rawSpeedKmh();
                validSpeeds.add(actualSpeed);
            }
            else if (seg.durationSeconds() > 300) {
                totalDistanceMeters += seg.distanceMeters();
            }
        }

        double maxSpeed = findMaxSpeedWithoutAnomalies(validSpeeds);

        Double avgSpeed = null;
        if (totalMovingSeconds > 0) {
            avgSpeed = roundToTwoDecimals((totalMovingDistanceMeters / totalMovingSeconds) * 3.6);
        }

        Double finalMaxSpeed = null;
        if (!validSpeeds.isEmpty()) {
            finalMaxSpeed = roundToTwoDecimals(maxSpeed);
        }

        Double distanceKm = roundToTwoDecimals(totalDistanceMeters / 1000.0);

        return new SpeedStats(finalMaxSpeed, avgSpeed, distanceKm);
    }


    private static double findMaxSpeedWithoutAnomalies(List<Double> validSpeeds) {
        double maxSpeed = 0.0;
        for (int i = 0; i < validSpeeds.size(); i++) {
            if (!isAnomaly(validSpeeds, i) && validSpeeds.get(i) > maxSpeed) {
                maxSpeed = validSpeeds.get(i);
            }
        }
        return maxSpeed;
    }


    private static boolean isAnomaly(List<Double> speeds, int index) {
        if (index <= 0 || index >= speeds.size() - 1) {
            return false;
        }

        double current = speeds.get(index);
        double prev = speeds.get(index - 1);

        if (current - prev <= 12.0) {
            return false;
        }

        double next = speeds.get(index + 1);
        if (current - next > 12.0) {
            return true;
        }

        if (index < speeds.size() - 2) {
            double next2 = speeds.get(index + 2);
            if (current - next2 > 12.0) {
                return true;
            }
        }

        return false;
    }

    private static double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}