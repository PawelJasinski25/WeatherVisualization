package jasinski.pawel.weather_visualization.dto;
import jasinski.pawel.weather_visualization.entity.TrackPoint;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record TripAnalysisContext(
        List<TrackPoint> points,
        Map<LocalDate, DayData> dailyMovements,
        List<EnrichedSegment> segments
) {}
