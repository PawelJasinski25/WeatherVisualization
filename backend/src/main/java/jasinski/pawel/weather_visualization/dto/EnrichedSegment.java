package jasinski.pawel.weather_visualization.dto;

import jasinski.pawel.weather_visualization.entity.TrackPoint;

public record EnrichedSegment(
        TrackPoint p1,
        TrackPoint p2,
        Double distanceMeters,
        double durationSeconds,
        Double rawSpeedKmh,
        boolean isMoving
) {}