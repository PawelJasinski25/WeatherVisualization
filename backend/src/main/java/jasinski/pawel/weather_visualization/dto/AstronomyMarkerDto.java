package jasinski.pawel.weather_visualization.dto;

public record AstronomyMarkerDto(
        String type,
        double lat,
        double lng,
        String time
) {}