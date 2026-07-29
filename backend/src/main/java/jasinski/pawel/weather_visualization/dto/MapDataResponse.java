package jasinski.pawel.weather_visualization.dto;
import java.util.List;
import java.util.Map;

public record MapDataResponse(
        List<TrackPointDto> route,
        List<AstronomyMarkerDto> astronomyMarkers,
        Map<String, double[]> ranges
) {}