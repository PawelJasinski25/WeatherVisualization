package jasinski.pawel.weather_visualization.dto;

import java.time.Instant;

public record StopPlace(
        double latitude,
        double longitude,
        Instant startTime,
        Instant endTime,
        long durationSeconds
) {}
