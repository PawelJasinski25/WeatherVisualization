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


    @Test
    void getTripMapData_shouldReturnEmptyResponse_whenNoPointsInDatabase() {
        when(trackPointRepository.findByTripIdOrderByTimeAsc(100L)).thenReturn(List.of());

        MapDataResponse response = tripMapService.getTripMapData(100L);

        assertThat(response.route()).isEmpty();
        assertThat(response.astronomyMarkers()).isEmpty();
    }


    @Test
    void getTripMapData_shouldFilterPoints_basedOnDistanceAndTimeGap_Phase1() {

        TrackPoint p1 = createPoint(52.2, 21.0, "2023-05-10T10:00:00Z", 1, null);
        TrackPoint p2 = createPoint(52.2, 21.0, "2023-05-10T10:00:05Z", 1, null);
        TrackPoint p3 = createPoint(52.21, 21.0, "2023-05-10T10:00:10Z", 1, null);
        TrackPoint p4 = createPoint(52.21, 21.0, "2023-05-10T10:00:30Z", 1, null);
        TrackPoint p5 = createPoint(52.21, 21.0, "2023-05-10T10:00:32Z", 1, null);

        when(trackPointRepository.findByTripIdOrderByTimeAsc(100L)).thenReturn(List.of(p1, p2, p3, p4, p5));

        MapDataResponse response = tripMapService.getTripMapData(100L);

        assertThat(response.route()).hasSize(4);
        assertThat(response.route().get(0).timeMs()).isEqualTo(Instant.parse("2023-05-10T10:00:00Z").toEpochMilli());
        assertThat(response.route().get(1).timeMs()).isEqualTo(Instant.parse("2023-05-10T10:00:10Z").toEpochMilli());
        assertThat(response.route().get(2).timeMs()).isEqualTo(Instant.parse("2023-05-10T10:00:30Z").toEpochMilli());
        assertThat(response.route().get(3).timeMs()).isEqualTo(Instant.parse("2023-05-10T10:00:32Z").toEpochMilli());
    }

    @Test
    void getTripMapData_shouldDownsample_whenMoreThan2500Points_Phase2() {
        List<TrackPoint> hugePointList = new ArrayList<>();
        long baseTimeSeconds = 1680000000L;

        for (int i = 0; i < 3000; i++) {
            String timeStr = Instant.ofEpochSecond(baseTimeSeconds + (i * 20L)).toString();
            hugePointList.add(createPoint(52.2, 21.0, timeStr, 1, null));
        }

        when(trackPointRepository.findByTripIdOrderByTimeAsc(100L)).thenReturn(hugePointList);

        MapDataResponse response = tripMapService.getTripMapData(100L);

        assertThat(response.route().size()).isBetween(2450, 2550);
    }

    @Test
    void getTripMapData_shouldMapWeatherToDtoCorrectly_andDetermineDayPhase() {
        Weather weather = new Weather();
        weather.setTemp(25.5);
        weather.setWindSpeed(12.0);
        weather.setCloudCover(80);
        weather.setWaveHeight(2.1);
        weather.setWeatherCode(3);

        TrackPoint p1 = createPoint(52.2, 21.0, "2023-05-10T12:00:00Z", 1, weather);
        when(trackPointRepository.findByTripIdOrderByTimeAsc(100L)).thenReturn(List.of(p1));

        MapDataResponse response = tripMapService.getTripMapData(100L);

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
    void getTripMapData_shouldGenerateAstronomyMarkers() {

        TrackPoint morning = createPoint(52.2, 21.0, "2023-05-10T02:00:00Z", 1, null);
        TrackPoint noon = createPoint(52.2, 21.0, "2023-05-10T12:00:00Z", 1, null);
        TrackPoint evening = createPoint(52.2, 21.0, "2023-05-10T21:00:00Z", 1, null);

        when(trackPointRepository.findByTripIdOrderByTimeAsc(100L)).thenReturn(List.of(morning, noon, evening));

        MapDataResponse response = tripMapService.getTripMapData(100L);

        List<AstronomyMarkerDto> markers = response.astronomyMarkers();

        assertThat(markers).extracting(AstronomyMarkerDto::type)
                .contains("WSCHÓD", "ZACHÓD", "KULMINACJA");
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
