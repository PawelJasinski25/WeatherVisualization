package jasinski.pawel.weather_visualization.dto;

import java.time.Instant;

public record TripResponseDto(
        Long id,
        String name,
        String fileHash,
        Instant startTime,
        Instant endTime
) {}