package jasinski.pawel.weather_visualization.dto;

import jasinski.pawel.weather_visualization.entity.TrackPoint;
import java.time.LocalDate;
import java.util.List;

public record DailySummary(
        LocalDate date,
        List<TrackPoint> points,
        DayMovementStats movementStats,
        WeatherStats overallWeatherStats,
        WeatherStats movingWeatherStats,
        AstronomyStats astroStats,
        List<TimelineEvent> timelineEvents
) {}