package jasinski.pawel.weather_visualization.service;

import jasinski.pawel.weather_visualization.dto.GridReq;
import jasinski.pawel.weather_visualization.entity.TrackPoint;
import jasinski.pawel.weather_visualization.entity.Trip;
import jasinski.pawel.weather_visualization.entity.Weather;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class TripWeatherServiceTest {

    @Mock private WaterDetectionService waterDetectionService;
    @Mock private OpenMeteoService openMeteoService;

    @InjectMocks
    private TripWeatherService tripWeatherService;

    private final GeometryFactory factory = new GeometryFactory();


    @Test
    void buildGridRequests_shouldCreateUniqueRequests_whenPointsAreInSameCell() {
        TrackPoint p1 = createPoint(52.2345, 21.0123, "2023-05-10T10:00:00Z");
        TrackPoint p2 = createPoint(52.2111, 21.0456, "2023-05-10T11:00:00Z");
        TrackPoint p3 = createPoint(50.0647, 19.9450, "2023-05-10T12:00:00Z");

        Map<String, List<TrackPoint>> grouped = tripWeatherService.groupTrackPointsByGrid(List.of(p1, p2, p3));
        Map<String, GridReq> gridRequests = tripWeatherService.buildGridRequests(grouped);

        assertThat(gridRequests).hasSize(2);
        assertThat(gridRequests).containsKey("2023-05-10_52.2_21.0");
        assertThat(gridRequests).containsKey("2023-05-10_50.1_19.9");
    }

    @Test
    void groupTrackPointsByGrid_shouldReturnEmptyMap_whenListIsEmpty() {
        Map<String, List<TrackPoint>> result = tripWeatherService.groupTrackPointsByGrid(List.of());
        assertThat(result).isEmpty();
    }

    @Test
    void buildGridRequests_shouldReturnEmptyMap_whenListIsEmpty() {
        Map<String, GridReq> result = tripWeatherService.buildGridRequests(Map.of());
        assertThat(result).isEmpty();
    }


    @Test
    void stripMarineDataFromWeather_shouldSetMarineFieldsToNull_whenCalled() {
        Weather weather = new Weather();
        weather.setTemp(20.0);
        weather.setWaveHeight(2.5);
        weather.setWavePeriod(5.0);
        weather.setWaveDirection(180);
        weather.setWindWaveHeight(1.5);
        weather.setWindWavePeriod(4.0);
        weather.setSwellWaveHeight(1.0);
        weather.setSwellWavePeriod(6.0);
        weather.setOceanCurrentVelocity(1.2);
        weather.setOceanCurrentDirection(90);
        weather.setSeaTemperature(15.0);

        tripWeatherService.stripMarineDataFromWeather(weather);

        assertThat(weather.getTemp()).isEqualTo(20.0);

        assertThat(weather.getWaveHeight()).isNull();
        assertThat(weather.getWavePeriod()).isNull();
        assertThat(weather.getWaveDirection()).isNull();
        assertThat(weather.getWindWaveHeight()).isNull();
        assertThat(weather.getWindWavePeriod()).isNull();
        assertThat(weather.getSwellWaveHeight()).isNull();
        assertThat(weather.getSwellWavePeriod()).isNull();
        assertThat(weather.getOceanCurrentVelocity()).isNull();
        assertThat(weather.getOceanCurrentDirection()).isNull();
        assertThat(weather.getSeaTemperature()).isNull();
    }


    @Test
    void fetchWeatherDataFromApi_shouldGroupRequests_whenMultipleDatesAreProvided() {
        GridReq req1 = new GridReq("2023-05-10", 52.2, 21.0, "2023-05-10_52.2_21.0");
        GridReq req2 = new GridReq("2023-05-11", 50.1, 19.9, "2023-05-11_50.1_19.9");

        Map<String, GridReq> requests = Map.of(
                req1.cacheKey(), req1,
                req2.cacheKey(), req2
        );
        Map<String, OpenMeteoService.OpenMeteoResponse> cache = new HashMap<>();

        tripWeatherService.fetchWeatherDataFromApi(requests, cache);

        verify(openMeteoService).fetchWeatherBatch(
                eq("2023-05-10"), anyList(), anyList(), anyList(), eq(cache), eq(waterDetectionService)
        );
        verify(openMeteoService).fetchWeatherBatch(
                eq("2023-05-11"), anyList(), anyList(), anyList(), eq(cache), eq(waterDetectionService)
        );
    }

    @Test
    void mapWeatherToTrackPoints_shouldRemoveMarineData_whenPointIsOnLand() {
        TrackPoint pt = createPoint(52.2, 21.0, "2023-05-10T10:00:00Z");
        Trip trip = new Trip();
        List<Weather> weathersToSave = new ArrayList<>();

        OpenMeteoService.OpenMeteoResponse mockResponse = new OpenMeteoService.OpenMeteoResponse();
        Map<String, OpenMeteoService.OpenMeteoResponse> cache = Map.of("2023-05-10_52.2_21.0", mockResponse);

        Weather mockedWeather = new Weather();
        mockedWeather.setTemp(15.0);
        mockedWeather.setWaveHeight(2.5);

        when(waterDetectionService.isWater(52.2, 21.0)).thenReturn(false);
        when(openMeteoService.buildWeatherEntity(eq(trip), eq(52.2), eq(21.0), eq(pt.getTime()), eq(mockResponse)))
                .thenReturn(mockedWeather);

        Map<String, List<TrackPoint>> grouped = tripWeatherService.groupTrackPointsByGrid(List.of(pt));
        tripWeatherService.mapWeatherToTrackPoints(trip, grouped, cache, weathersToSave);

        assertThat(pt.getWeather()).isNotNull();
        assertThat(pt.getWeather().getTemp()).isEqualTo(15.0);

        assertThat(pt.getWeather().getWaveHeight()).isNull();
        assertThat(weathersToSave).hasSize(1);
    }

    @Test
    void mapWeatherToTrackPoints_shouldKeepMarineData_whenPointIsOnWater() {
        TrackPoint pt = createPoint(54.4, 18.5, "2023-05-10T10:00:00Z");
        Trip trip = new Trip();
        List<Weather> weathersToSave = new ArrayList<>();

        OpenMeteoService.OpenMeteoResponse mockResponse = new OpenMeteoService.OpenMeteoResponse();
        Map<String, OpenMeteoService.OpenMeteoResponse> cache = Map.of("2023-05-10_54.4_18.5", mockResponse);

        Weather mockedWeather = new Weather();
        mockedWeather.setTemp(15.0);
        mockedWeather.setWaveHeight(2.5);

        when(waterDetectionService.isWater(54.4, 18.5)).thenReturn(true);
        when(openMeteoService.buildWeatherEntity(eq(trip), eq(54.4), eq(18.5), eq(pt.getTime()), eq(mockResponse)))
                .thenReturn(mockedWeather);

        Map<String, List<TrackPoint>> grouped = tripWeatherService.groupTrackPointsByGrid(List.of(pt));
        tripWeatherService.mapWeatherToTrackPoints(trip, grouped, cache, weathersToSave);

        assertThat(pt.getWeather()).isNotNull();
        assertThat(pt.getWeather().getTemp()).isEqualTo(15.0);

        assertThat(pt.getWeather().getWaveHeight()).isEqualTo(2.5);
    }

    @Test
    void mapWeatherToTrackPoints_shouldLeaveWeatherNull_whenApiDataMissing() {
        TrackPoint pt = createPoint(52.2, 21.0, "2023-05-10T10:00:00Z");
        Trip trip = new Trip();
        List<Weather> weathersToSave = new ArrayList<>();

        Map<String, OpenMeteoService.OpenMeteoResponse> cache = new HashMap<>();

        Map<String, List<TrackPoint>> grouped = tripWeatherService.groupTrackPointsByGrid(List.of(pt));
        tripWeatherService.mapWeatherToTrackPoints(trip, grouped, cache, weathersToSave);

        assertThat(pt.getWeather()).isNull();
        assertThat(weathersToSave).isEmpty();
    }

    private TrackPoint createPoint(double lat, double lon, String timeStr) {
        TrackPoint pt = new TrackPoint();
        pt.setLongitude(lon);
        pt.setLatitude(lat);
        pt.setTime(Instant.parse(timeStr));
        return pt;
    }
}
