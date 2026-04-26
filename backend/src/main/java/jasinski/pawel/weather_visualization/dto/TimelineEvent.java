package jasinski.pawel.weather_visualization.dto;

import java.time.Duration;
import java.time.Instant;

public record TimelineEvent(
        String type,
        Instant start,
        Instant end,
        double lat,
        double lon
) {
    public long durationSeconds() {
        return Math.abs(Duration.between(start, end).getSeconds());
    }
}