package jasinski.pawel.weather_visualization.dto;
import java.util.List;

public record MapDataResponse(
        List<TrackPointDto> route,
        List<AstronomyMarkerDto> astronomyMarkers
) {}