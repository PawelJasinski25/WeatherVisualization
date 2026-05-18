package jasinski.pawel.weather_visualization.dto;

import java.util.List;

public record TripMergeRequestDto(
        String newTripName,
        List<TripMergeSegment> segments
) {
    public record TripMergeSegment(
            Long tripId,
            String trimStartTime,
            String trimEndTime
    ) {}
}