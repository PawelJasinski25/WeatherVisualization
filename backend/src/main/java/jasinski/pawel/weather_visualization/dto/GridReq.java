package jasinski.pawel.weather_visualization.dto;

public record GridReq(String dateStr, double lat, double lon, String cacheKey) {
}
