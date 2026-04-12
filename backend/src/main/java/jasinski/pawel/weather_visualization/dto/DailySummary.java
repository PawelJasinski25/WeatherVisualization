package jasinski.pawel.weather_visualization.dto;

import jasinski.pawel.weather_visualization.entity.TrackPoint;
import java.time.LocalDate;
import java.util.List;

public record DailySummary(
        LocalDate date,
        List<TrackPoint> points,
        DayMovementStats movementStats,
        WeatherStats weatherStats,
        AstronomyStats astroStats,
        List<StopPlace> stops
) {}