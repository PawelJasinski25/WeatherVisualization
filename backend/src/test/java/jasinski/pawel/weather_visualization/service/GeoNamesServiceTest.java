package jasinski.pawel.weather_visualization.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeoNamesServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private GeoNamesService geoNamesService;

    @Test
    void getPlaceName_shouldReturnPlace_andNotCallOceanApi() {

        GeoNamesService.GeoNamesResponse mockPlaceResponse = new GeoNamesService.GeoNamesResponse(
                List.of(new GeoNamesService.GeoName("Warszawa", "Polska"))
        );

        when(restTemplate.getForObject(contains("findNearbyPlaceNameJSON"), eq(GeoNamesService.GeoNamesResponse.class), eq(52.2), eq(21.0), anyString()))
                .thenReturn(mockPlaceResponse);

        String result = geoNamesService.getPlaceName(52.2, 21.0);

        assertThat(result).isEqualTo("Warszawa (Polska)");

        verify(restTemplate, times(1)).getForObject(contains("findNearbyPlaceNameJSON"), eq(GeoNamesService.GeoNamesResponse.class), eq(52.2), eq(21.0), anyString());
        verify(restTemplate, never()).getForObject(contains("oceanJSON"), any(), anyDouble(), anyDouble(), anyString());
    }

    @Test
    void getPlaceName_shouldFallbackToOcean_whenPlaceIsNotFound() {

        when(restTemplate.getForObject(contains("findNearbyPlaceNameJSON"), eq(GeoNamesService.GeoNamesResponse.class), eq(54.4), eq(18.5), anyString()))
                .thenReturn(null);

        GeoNamesService.OceanResponse mockOceanResponse = new GeoNamesService.OceanResponse(
                new GeoNamesService.Ocean("Morze Bałtyckie")
        );
        when(restTemplate.getForObject(contains("oceanJSON"), eq(GeoNamesService.OceanResponse.class), eq(54.4), eq(18.5), anyString()))
                .thenReturn(mockOceanResponse);

        String result = geoNamesService.getPlaceName(54.4, 18.5);

        assertThat(result).isEqualTo("Morze Bałtyckie");

        verify(restTemplate, times(1)).getForObject(contains("findNearbyPlaceNameJSON"), eq(GeoNamesService.GeoNamesResponse.class), eq(54.4), eq(18.5), anyString());
        verify(restTemplate, times(1)).getForObject(contains("oceanJSON"), eq(GeoNamesService.OceanResponse.class), eq(54.4), eq(18.5), anyString());
    }

    @Test
    void getPlaceName_shouldFallbackToCoordinates_whenApiThrowsException() {
        when(restTemplate.getForObject(contains("findNearbyPlaceNameJSON"), eq(GeoNamesService.GeoNamesResponse.class), eq(52.2), eq(21.0), anyString()))
                .thenThrow(new RuntimeException("API Connection Timeout"));

        String result = geoNamesService.getPlaceName(52.2, 21.0);

        assertThat(result).isEqualTo("52.20000, 21.00000");

        verify(restTemplate, times(1)).getForObject(contains("findNearbyPlaceNameJSON"), eq(GeoNamesService.GeoNamesResponse.class), eq(52.2), eq(21.0), anyString());
        verify(restTemplate, never()).getForObject(contains("oceanJSON"), any(), anyDouble(), anyDouble(), anyString());
    }

    @Test
    void getPlaceName_shouldFallbackToCoordinates_whenBothApisReturnNull() {
        when(restTemplate.getForObject(contains("findNearbyPlaceNameJSON"), eq(GeoNamesService.GeoNamesResponse.class), eq(0.0), eq(0.0), anyString()))
                .thenReturn(null);
        when(restTemplate.getForObject(contains("oceanJSON"), eq(GeoNamesService.OceanResponse.class), eq(0.0), eq(0.0), anyString()))
                .thenReturn(null);

        String result = geoNamesService.getPlaceName(0.0, 0.0);


        assertThat(result).isEqualTo("0.00000, 0.00000");

        verify(restTemplate, times(1)).getForObject(contains("findNearbyPlaceNameJSON"), eq(GeoNamesService.GeoNamesResponse.class), eq(0.0), eq(0.0), anyString());
        verify(restTemplate, times(1)).getForObject(contains("oceanJSON"), eq(GeoNamesService.OceanResponse.class), eq(0.0), eq(0.0), anyString());
    }
}
