package jasinski.pawel.weather_visualization.utils;

import org.locationtech.jts.geom.Point;

public class GeoUtils {
    public static double calculateDistance(Point p1, Point p2) {
        if (p1 == null || p2 == null) return 0.0;
        double earthRadius = 6371000;
        double dLat = Math.toRadians(p2.getY() - p1.getY());
        double dLon = Math.toRadians(p2.getX() - p1.getX());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(p1.getY())) * Math.cos(Math.toRadians(p2.getY())) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
}