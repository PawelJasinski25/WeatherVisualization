package jasinski.pawel.weather_visualization.service;

import jasinski.pawel.weather_visualization.dto.ReportResource;
import jasinski.pawel.weather_visualization.dto.TripAnalysisContext;
import jasinski.pawel.weather_visualization.dto.TripReportDataDto;
import jasinski.pawel.weather_visualization.dto.TripResponseDto;
import jasinski.pawel.weather_visualization.entity.TrackPoint;
import jasinski.pawel.weather_visualization.entity.Weather;
import jasinski.pawel.weather_visualization.repository.TrackPointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private TrackPointRepository trackPointRepository;
    @Mock private GeoNamesService geoNamesService;
    @Mock private TripService tripService;
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private ReportService reportService;

    private final GeometryFactory factory = new GeometryFactory();
    private final String userEmail = "test@example.com";
    private TripResponseDto mockTripDto;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(reportService, "pythonUrl", "http://python-api/pdf");

        mockTripDto = new TripResponseDto(
                100L, "Trasa w Tatry.gpx", "hash123",
                Instant.parse("2023-05-10T10:00:00Z"), Instant.parse("2023-05-10T12:00:00Z")
        );
    }


    @Test
    void getTripReportData_shouldThrowException_whenUserLacksAccessToTrip() {
        when(tripService.getUserTrips(userEmail)).thenReturn(List.of());

        assertThatThrownBy(() -> reportService.getTripReportData(100L, userEmail))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }


    @Test
    void getTripReportData_shouldAnalyzePointsAndCalculateAveragesCorrectly() {
        when(tripService.getUserTrips(userEmail)).thenReturn(List.of(mockTripDto));

        TrackPoint p1 = createPoint(52.2, 21.0, "2023-05-10T10:00:00Z", 1, 15.0);
        TrackPoint p2 = createPoint(52.201, 21.001, "2023-05-10T10:01:00Z", 1, 16.0);
        TrackPoint p3 = createPoint(52.202, 21.002, "2023-05-10T10:02:00Z", 1, 16.0);

        when(trackPointRepository.findByTripIdOrderByTimeAsc(100L)).thenReturn(List.of(p1, p2, p3));

        TripReportDataDto result = reportService.getTripReportData(100L, userEmail);

        assertThat(result.overallWeather().avgTemp()).isEqualTo(15.5);
    }

    @Test
    void getTripReportData_shouldGroupMultipleDaysCorrectly() {
        when(tripService.getUserTrips(userEmail)).thenReturn(List.of(mockTripDto));

        TrackPoint p1 = createPoint(52.2, 21.0, "2023-05-10T10:00:00Z", 1, 15.0);
        TrackPoint p2 = createPoint(52.2, 21.0, "2023-05-11T10:00:00Z", 1, 15.0);

        when(trackPointRepository.findByTripIdOrderByTimeAsc(100L)).thenReturn(List.of(p1, p2));

        TripReportDataDto result = reportService.getTripReportData(100L, userEmail);

        assertThat(result.dailySummaries()).hasSize(2);
        assertThat(result.dailySummaries().get(0).date().toString()).isEqualTo("2023-05-10");
        assertThat(result.dailySummaries().get(1).date().toString()).isEqualTo("2023-05-11");
    }

    @Test
    void getTripReportData_shouldUseGeoNamesService_forStoppedEvents() {
        when(tripService.getUserTrips(userEmail)).thenReturn(List.of(mockTripDto));

        TrackPoint p1 = createPoint(52.2, 21.0, "2023-05-10T10:00:00Z", 1, 15.0);
        TrackPoint p2 = createPoint(52.2, 21.0, "2023-05-10T12:00:00Z", 1, 15.0);

        when(trackPointRepository.findByTripIdOrderByTimeAsc(100L)).thenReturn(List.of(p1, p2));

        when(geoNamesService.getPlaceName(52.2, 21.0)).thenReturn("Warszawa");

        TripReportDataDto result = reportService.getTripReportData(100L, userEmail);

        verify(geoNamesService).getPlaceName(52.2, 21.0);
        boolean containsWarsaw = result.dailySummaries().get(0).timelineEvents().stream()
                .anyMatch(ev -> "Warszawa".equals(ev.placeName()));
        assertThat(containsWarsaw).isTrue();
    }


    @Test
    void generateCsv_shouldContainHeadersAndTripData() {

        TrackPoint p1 = createPoint(52.2, 21.0, "2023-05-10T10:00:00Z", 1, 15.0);
        TrackPoint p2 = createPoint(52.201, 21.001, "2023-05-10T10:01:00Z", 1, 16.0);

        when(trackPointRepository.findByTripIdOrderByTimeAsc(100L)).thenReturn(List.of(p1, p2));
        TripAnalysisContext context = ReflectionTestUtils.invokeMethod(reportService, "analyzeTrip", 100L);

        String csvContent = reportService.generateSummaryCsv(context);

        assertThat(csvContent).contains("Data;Start;Koniec;Czas w ruchu;Czas na postoju");
        assertThat(csvContent).contains("Średnia temperatura (°C)");
        assertThat(csvContent).contains("2023-05-10");
    }

    @Test
    void getCsvReportResource_shouldFormatFileNameProperly() {
        TripResponseDto tripWithoutGpx = new TripResponseDto(
                100L, "Trasa rowerowa", "hash123", Instant.now(), Instant.now()
        );
        when(tripService.getUserTrips(userEmail)).thenReturn(List.of(tripWithoutGpx));
        when(trackPointRepository.findByTripIdOrderByTimeAsc(100L)).thenReturn(List.of());

        ReportResource resource = reportService.getCsvReportResource(100L, userEmail);

        assertThat(resource.fileName()).isEqualTo("Trasa rowerowa.csv");

        assertThat(resource.content()[0]).isEqualTo((byte) 0xEF);
    }


    @Test
    void generatePdfReport_shouldCallPythonServiceAndReturnBytes() {
        when(tripService.getUserTrips(userEmail)).thenReturn(List.of(mockTripDto));
        when(trackPointRepository.findByTripIdOrderByTimeAsc(100L)).thenReturn(List.of());

        byte[] expectedPdfBytes = "Example pdf".getBytes();
        when(restTemplate.postForObject(
                eq("http://python-api/pdf"),
                any(Map.class),
                eq(byte[].class)
        )).thenReturn(expectedPdfBytes);
        ReportResource resource = reportService.generatePdfReportResource(100L, userEmail, Map.of("theme", "dark"));

        assertThat(resource.content()).isEqualTo(expectedPdfBytes);
        assertThat(resource.fileName()).isEqualTo("Trasa w Tatry_raport.pdf");
    }

    private TrackPoint createPoint(double lat, double lon, String timeStr, int segmentId, double temp) {
        TrackPoint pt = new TrackPoint();
        pt.setLongitude(lon);
        pt.setLatitude(lat);
        pt.setTime(Instant.parse(timeStr));
        pt.setSegmentId(segmentId);

        Weather w = new Weather();
        w.setTemp(temp);
        pt.setWeather(w);

        return pt;
    }
}
