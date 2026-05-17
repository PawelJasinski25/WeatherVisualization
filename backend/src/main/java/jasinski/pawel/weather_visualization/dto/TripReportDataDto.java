package jasinski.pawel.weather_visualization.dto;

import java.util.List;

public record TripReportDataDto(
        String tripName,
        DayMovementStats overallMovement,
        SpeedStats overallSpeed,
        WeatherStats overallWeather,
        WeatherStats overallMovingWeather,
        List<ReportDailySummaryDto> dailySummaries
) {}