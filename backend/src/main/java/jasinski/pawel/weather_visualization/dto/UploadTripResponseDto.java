package jasinski.pawel.weather_visualization.dto;

public record UploadTripResponseDto(Long tripId, boolean isDuplicate, String tripName) {
}
