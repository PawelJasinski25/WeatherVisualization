package jasinski.pawel.weather_visualization.service;

import jasinski.pawel.weather_visualization.dto.AstronomyMarkerDto;
import jasinski.pawel.weather_visualization.dto.MapDataResponse;
import jasinski.pawel.weather_visualization.dto.TrackPointDto;
import jasinski.pawel.weather_visualization.entity.TrackPoint;
import jasinski.pawel.weather_visualization.entity.Weather;
import jasinski.pawel.weather_visualization.repository.TrackPointRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripMapServiceTest {

    @Mock
    private TrackPointRepository trackPointRepository;

    @InjectMocks
    private TripMapService tripMapService;

    private final GeometryFactory factory = new GeometryFactory();
    private final String defaultTimezone = "UTC";


    @Test
    void getTripMapData_shouldReturnEmptyResponse_whenNoPointsInDatabase() {
        when(trackPointRepository.findByTripIdOrderByTimeAsc(100L)).thenReturn(List.of());

        MapDataResponse response = tripMapService.getTripMapData(100L, defaultTimezone);

        assertThat(response.route()).isEmpty();
        assertThat(response.astronomyMarkers()).isEmpty();
    }


    @Test
    void getTripMapData_shouldReturnAllPoints_whenUnder6000Points() {
        TrackPoint p1 = createPoint(52.2, 21.0, "2023-05-10T10:00:00Z", 1, null);
        TrackPoint p2 = createPoint(52.2, 21.0, "2023-05-10T10:00:05Z", 1, null);
        TrackPoint p3 = createPoint(52.21, 21.0, "2023-05-10T10:00:10Z", 1, null);

        when(trackPointRepository.findByTripIdOrderByTimeAsc(100L)).thenReturn(List.of(p1, p2, p3));

        MapDataResponse response = tripMapService.getTripMapData(100L, defaultTimezone);

        assertThat(response.route()).hasSize(3);
    }

    @Test
    void getTripMapData_shouldDownsample_whenMoreThan6000Points() {
        List<TrackPoint> hugePointList = new ArrayList<>();
        long baseTimeSeconds = 1680000000L;

        for (int i = 0; i < 7000; i++) {
            String timeStr = Instant.ofEpochSecond(baseTimeSeconds + (i * 20L)).toString();
            hugePointList.add(createPoint(52.2, 21.0, timeStr, 1, null));
        }

        when(trackPointRepository.findByTripIdOrderByTimeAsc(100L)).thenReturn(hugePointList);

        MapDataResponse response = tripMapService.getTripMapData(100L, defaultTimezone);

        assertThat(response.route().size()).isBetween(5980, 6020);
    }

    @Test
    void getTripMapData_shouldMapWeatherAndDayPhase_whenWeatherIsPresent() {
        Weather weather = new Weather();
        weather.setTemp(25.5);
        weather.setWindSpeed(12.0);
        weather.setCloudCover(80);
        weather.setWaveHeight(2.1);
        weather.setWeatherCode(3);

        TrackPoint p1 = createPoint(52.2, 21.0, "2023-05-10T12:00:00Z", 1, weather);
        when(trackPointRepository.findByTripIdOrderByTimeAsc(100L)).thenReturn(List.of(p1));

        MapDataResponse response = tripMapService.getTripMapData(100L, defaultTimezone);

        assertThat(response.route()).hasSize(1);
        TrackPointDto dto = response.route().get(0);

        assertThat(dto.latitude()).isEqualTo(52.2);
        assertThat(dto.longitude()).isEqualTo(21.0);
        assertThat(dto.temp()).isEqualTo(25.5);
        assertThat(dto.windSpeed()).isEqualTo(12.0);
        assertThat(dto.cloudCover()).isEqualTo(80);
        assertThat(dto.waveHeight()).isEqualTo(2.1);
        assertThat(dto.weatherCode()).isEqualTo(3);

        assertThat(dto.dayPhase()).isEqualTo(4);
    }


    @Test
    void getTripMapData_shouldGenerateAstronomyMarkers_whenPointsSpanMultiplePhases() {

        TrackPoint morning = createPoint(52.2, 21.0, "2023-05-10T02:00:00Z", 1, null);
        TrackPoint noon = createPoint(52.2, 21.0, "2023-05-10T12:00:00Z", 1, null);
        TrackPoint evening = createPoint(52.2, 21.0, "2023-05-10T21:00:00Z", 1, null);

        when(trackPointRepository.findByTripIdOrderByTimeAsc(100L)).thenReturn(List.of(morning, noon, evening));

        MapDataResponse response = tripMapService.getTripMapData(100L, defaultTimezone);

        List<AstronomyMarkerDto> markers = response.astronomyMarkers();

        assertThat(markers).extracting(AstronomyMarkerDto::type)
                .contains("WSCHÓD SŁOŃCA", "ZACHÓD SŁOŃCA", "KULMINACJA SŁOŃCA");
    }

    private TrackPoint createPoint(double lat, double lon, String timeStr, int segmentId, Weather weather) {
        TrackPoint pt = new TrackPoint();
        pt.setLongitude(lon);
        pt.setLatitude(lat);
        pt.setTime(Instant.parse(timeStr));
        pt.setSegmentId(segmentId);
        pt.setWeather(weather);
        return pt;
    }
}
