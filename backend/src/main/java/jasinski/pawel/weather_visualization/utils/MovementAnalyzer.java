package jasinski.pawel.weather_visualization.utils;

import jasinski.pawel.weather_visualization.dto.DayMovementStats;
import jasinski.pawel.weather_visualization.dto.StopPlace;
import jasinski.pawel.weather_visualization.entity.TrackPoint;
import org.locationtech.jts.geom.Point;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class MovementAnalyzer {

    private static final double STOP_RADIUS_METERS = 30.0;
    private static final long MIN_STOP_DURATION_SECONDS = 1 * 60;

    public static DayMovementStats analyzeDay(List<TrackPoint> pointsOfDay, List<StopPlace> stops) {
        if (pointsOfDay == null || pointsOfDay.size() < 2) {
            return new DayMovementStats(0, 0);
        }


        long totalDurationSeconds = 0;
        for (int i = 1; i < pointsOfDay.size(); i++) {
            TrackPoint prev = pointsOfDay.get(i - 1);
            TrackPoint curr = pointsOfDay.get(i);
            long diff = Duration.between(prev.getTime(), curr.getTime()).getSeconds();
            if (diff > 0) {
                totalDurationSeconds += diff;
            }
        }


        long timeStoppedSeconds = 0;
        if (stops != null) {
            for (StopPlace stop : stops) {
                timeStoppedSeconds += stop.durationSeconds();
            }
        }

        long timeMovingSeconds = totalDurationSeconds - timeStoppedSeconds;

        if (timeMovingSeconds < 0) timeMovingSeconds = 0;

        return new DayMovementStats(timeMovingSeconds, timeStoppedSeconds);
    }


    public static List<StopPlace> findStops(List<TrackPoint> pointsOfDay) {
        List<StopPlace> stops = new ArrayList<>();
        if (pointsOfDay == null || pointsOfDay.isEmpty()) return stops;

        int i = 0;
        while (i < pointsOfDay.size()) {
            TrackPoint startPoint = pointsOfDay.get(i);
            int j = i + 1;
            TrackPoint lastInsidePoint = startPoint;

            while (j < pointsOfDay.size()) {
                TrackPoint currPoint = pointsOfDay.get(j);
                double dist = calculateDistance(startPoint.getLocation(), currPoint.getLocation());

                if (dist <= STOP_RADIUS_METERS) {
                    lastInsidePoint = currPoint;
                    j++;
                } else {
                    break;
                }
            }

            long durationSeconds = Duration.between(startPoint.getTime(), lastInsidePoint.getTime()).getSeconds();

            if (durationSeconds >= MIN_STOP_DURATION_SECONDS) {
                stops.add(new StopPlace(
                        startPoint.getLocation().getY(),
                        startPoint.getLocation().getX(),
                        startPoint.getTime(),
                        lastInsidePoint.getTime(),
                        durationSeconds
                ));
            }

            i = Math.max(i + 1, j);
        }

        return stops;
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