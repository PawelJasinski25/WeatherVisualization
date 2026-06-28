package jasinski.pawel.weather_visualization.service;

import jasinski.pawel.weather_visualization.entity.Trip;
import jasinski.pawel.weather_visualization.entity.Weather;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpenMeteoServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private WaterDetectionService waterDetectionService;

    private OpenMeteoService service;

    @BeforeEach
    void setup() {
        service = new OpenMeteoService();

        ReflectionTestUtils.setField(
                service,
                "restTemplate",
                restTemplate
        );
    }


    @Test
    void buildWeatherEntity_shouldMapAllWeatherFields() {
        OpenMeteoService.OpenMeteoResponse response = new OpenMeteoService.OpenMeteoResponse();
        response.hourly = new OpenMeteoService.HourlyData();

        response.hourly.time = List.of("2024-01-01T10:00");
        response.hourly.temperature_2m = List.of(20.5);
        response.hourly.dew_point_2m = List.of(10.0);
        response.hourly.wind_speed_10m = List.of(15.0);
        response.hourly.relative_humidity_2m = List.of(70);
        response.hourly.rain = List.of(2.5);
        response.hourly.surface_pressure = List.of(1015.0);
        response.hourly.cloud_cover = List.of(50);
        response.hourly.cloud_cover_low = List.of(10);
        response.hourly.cloud_cover_mid = List.of(20);
        response.hourly.cloud_cover_high = List.of(30);
        response.hourly.weather_code = List.of(1);
        response.hourly.wind_direction_10m = List.of(180);
        response.hourly.wind_gusts_10m = List.of(25.0);
        response.hourly.snowfall = List.of(0.0);

        Instant time = Instant.parse("2024-01-01T10:00:00Z");

        Weather result = service.buildWeatherEntity(new Trip(), 52.0, 21.0, time, response);

        assertThat(result).isNotNull();
        assertThat(result.getLatitude()).isEqualTo(52.0);
        assertThat(result.getLongitude()).isEqualTo(21.0);
        assertThat(result.getTemp()).isEqualTo(20.5);
        assertThat(result.getDewPoint()).isEqualTo(10.0);
        assertThat(result.getWindSpeed()).isEqualTo(15.0);
        assertThat(result.getHumidity()).isEqualTo(70);
        assertThat(result.getRain()).isEqualTo(2.5);
        assertThat(result.getPressure()).isEqualTo(1015.0);
        assertThat(result.getWeatherCode()).isEqualTo(1);
    }

    @Test
    void buildWeatherEntity_shouldReturnNull_whenHourDoesNotExist() {
        OpenMeteoService.OpenMeteoResponse response = new OpenMeteoService.OpenMeteoResponse();
        response.hourly = new OpenMeteoService.HourlyData();
        response.hourly.time = List.of("2024-01-01T09:00");

        Weather result = service.buildWeatherEntity(new Trip(), 52, 21, Instant.parse("2024-01-01T10:00:00Z"), response);

        assertThat(result).isNull();
    }

    @Test
    void buildWeatherEntity_shouldReturnNull_whenResponseIsNull() {
        Weather result = service.buildWeatherEntity(new Trip(), 52, 21, Instant.now(), null);

        assertThat(result).isNull();
    }

    @Test
    void fetchWeatherBatch_shouldSaveResponseIntoCache() {
        OpenMeteoService.OpenMeteoResponse response = new OpenMeteoService.OpenMeteoResponse();

        when(waterDetectionService.isWater(anyDouble(), anyDouble())).thenReturn(false);

        when(restTemplate.getForObject(anyString(), eq(OpenMeteoService.OpenMeteoResponse.class)))
                .thenReturn(response);

        Map<String, OpenMeteoService.OpenMeteoResponse> cache = new HashMap<>();

        int result = service.fetchWeatherBatch("2024-01-01", List.of(52.0), List.of(21.0), List.of("key1"), cache, waterDetectionService);

        assertThat(result).isEqualTo(1);
        assertThat(cache).containsEntry("key1", response);
        verify(restTemplate).getForObject(anyString(), eq(OpenMeteoService.OpenMeteoResponse.class));
    }


    @Test
    void fetchWeatherBatch_shouldReturnZero_whenApiReturns500Error() {
        when(waterDetectionService.isWater(anyDouble(), anyDouble())).thenReturn(false);

        when(restTemplate.getForObject(anyString(), eq(OpenMeteoService.OpenMeteoResponse[].class)))
                .thenThrow(new RestClientException("500 Server Error"));

        int result = service.fetchWeatherBatch("2024-01-01", List.of(52.0, 52.1), List.of(21.0, 21.1), List.of("a", "b"), new HashMap<>(), waterDetectionService);

        assertThat(result).isEqualTo(0);
        verify(restTemplate, times(1)).getForObject(anyString(), eq(OpenMeteoService.OpenMeteoResponse[].class));
    }

    @Test
    void fetchWeatherBatch_shouldReturnOne_whenApiReturnsNullResponse() {
        when(waterDetectionService.isWater(anyDouble(), anyDouble())).thenReturn(false);

        when(restTemplate.getForObject(anyString(), eq(OpenMeteoService.OpenMeteoResponse.class)))
                .thenReturn(null);

        int result = service.fetchWeatherBatch("2024-01-01", List.of(52.0), List.of(21.0), List.of("key"), new HashMap<>(), waterDetectionService);

        assertThat(result).isEqualTo(1);
    }


    @Test
    void fetchWeatherBatch_shouldMergeMarineData_whenPointIsWater() {
        OpenMeteoService.OpenMeteoResponse weather = new OpenMeteoService.OpenMeteoResponse();
        weather.hourly = new OpenMeteoService.HourlyData();

        OpenMeteoService.MarineResponse marine = new OpenMeteoService.MarineResponse();
        marine.hourly = new OpenMeteoService.MarineHourlyData();
        marine.hourly.wave_height = List.of(3.5);
        marine.hourly.wave_period = List.of(8.0);
        marine.hourly.sea_surface_temperature = List.of(14.5);

        when(waterDetectionService.isWater(anyDouble(), anyDouble())).thenReturn(true);

        when(restTemplate.getForObject(anyString(), eq(OpenMeteoService.OpenMeteoResponse.class)))
                .thenReturn(weather);

        when(restTemplate.getForObject(anyString(), eq(OpenMeteoService.MarineResponse.class)))
                .thenReturn(marine);

        Map<String, OpenMeteoService.OpenMeteoResponse> cache = new HashMap<>();

        service.fetchWeatherBatch("2024-01-01", List.of(1.0), List.of(2.0), List.of("sea"), cache, waterDetectionService);

        assertThat(cache.get("sea").hourly.wave_height).containsExactly(3.5);
        assertThat(cache.get("sea").hourly.wave_period).containsExactly(8.0);
        assertThat(cache.get("sea").hourly.sea_surface_temperature).containsExactly(14.5);
    }
}