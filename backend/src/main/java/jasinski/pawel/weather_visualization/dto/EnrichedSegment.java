package jasinski.pawel.weather_visualization.dto;

import jasinski.pawel.weather_visualization.entity.TrackPoint;

public record EnrichedSegment(
        TrackPoint p1,
        TrackPoint p2,
        double distanceMeters,
        double durationSeconds,
        double rawSpeedKmh,
        boolean isMoving
) {}