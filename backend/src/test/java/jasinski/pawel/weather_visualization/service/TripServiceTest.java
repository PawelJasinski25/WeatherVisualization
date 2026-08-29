package jasinski.pawel.weather_visualization.service;

import jasinski.pawel.weather_visualization.dto.TripMergeRequestDto;
import jasinski.pawel.weather_visualization.dto.TripResponseDto;
import jasinski.pawel.weather_visualization.dto.UploadTripResponseDto;
import jasinski.pawel.weather_visualization.entity.TrackPoint;
import jasinski.pawel.weather_visualization.entity.Trip;
import jasinski.pawel.weather_visualization.entity.User;
import jasinski.pawel.weather_visualization.repository.TrackPointRepository;
import jasinski.pawel.weather_visualization.repository.TripRepository;
import jasinski.pawel.weather_visualization.repository.UserRepository;
import jasinski.pawel.weather_visualization.repository.WeatherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock private TripRepository tripRepository;
    @Mock private TrackPointRepository trackPointRepository;
    @Mock private WeatherRepository weatherRepository;
    @Mock private UserRepository userRepository;
    @Mock private TripPersistenceService tripPersistenceService;
    @Mock private GpxParserService gpxParserService;
    @Mock private TripWeatherService tripWeatherService;
    @Mock private WaterDetectionService waterDetectionService;

    @InjectMocks
    private TripService tripService;

    private User owner;
    private Trip trip;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setEmail("email@example.com");

        trip = new Trip();
        trip.setId(100L);
        trip.setName("Przykładowa wyprawa");
        trip.setFileHash("example hash");
        trip.setUser(owner);
    }

    @Test
    void getUserTrips_shouldReturnMappedDtoList_whenTripsExist() {
        when(tripRepository.findByUser_Email("email@example.com")).thenReturn(List.of(trip));
        List<TripResponseDto> result = tripService.getUserTrips("email@example.com");
        assertThat(result).hasSize(1);
    }

    @Test
    void updateTripName_shouldUpdateAndSave_whenUserIsOwner() {
        when(tripRepository.findById(100L)).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenReturn(trip);

        Trip updatedTrip = tripService.updateTripName(100L, "Nowa wyprawa", "email@example.com");
        assertThat(updatedTrip.getName()).isEqualTo("Nowa wyprawa");
    }

    @Test
    void updateTripName_shouldThrowException_whenUserIsNotOwner() {
        when(tripRepository.findById(100L)).thenReturn(Optional.of(trip));
        assertThatThrownBy(() -> tripService.updateTripName(100L, "Nowa wyprawa 2", "email2@example.com"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void deleteTrip_shouldDeleteEverything_whenUserIsOwner() {
        when(tripRepository.findById(100L)).thenReturn(Optional.of(trip));
        tripService.deleteTrip(100L, "email@example.com");
        verify(trackPointRepository).deleteAllByTripId(100L);
        verify(tripRepository).delete(trip);
    }

    @Test
    void processGpxFile_shouldReturnExistingTrip_whenTripIsDuplicate() throws Exception {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("data".getBytes()));
        when(userRepository.findByEmail("email@example.com")).thenReturn(Optional.of(owner));
        when(tripRepository.findByFileHashAndUser_Email(anyString(), anyString())).thenReturn(Optional.of(trip));

        UploadTripResponseDto response = tripService.processGpxFile(mockFile, "email@example.com");
        assertThat(response.tripId()).isEqualTo(100L);
        assertThat(response.isDuplicate()).isTrue();
        assertThat(response.tripName()).isEqualTo("Przykładowa wyprawa");

        verify(gpxParserService, never()).extractTrackPoints(any(Path.class), any());
    }

    @Test
    void processGpxFile_shouldCreateNewTrip_whenTripIsNew() throws Exception {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("Nowa trasa GPX.gpx");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("data".getBytes()));
        when(userRepository.findByEmail("email@example.com")).thenReturn(Optional.of(owner));
        when(tripRepository.findByFileHashAndUser_Email(anyString(), anyString())).thenReturn(Optional.empty());

        Trip newTrip = new Trip();
        newTrip.setId(200L);
        newTrip.setName("Nowa trasa GPX");

        TrackPoint trackPoint = new TrackPoint();
        trackPoint.setTime(Instant.now());

        when(tripPersistenceService.createAndSaveTripHeader(any(), any(), any())).thenReturn(newTrip);
        when(gpxParserService.extractTrackPoints(any(Path.class), eq(newTrip)))
                .thenReturn(new java.util.ArrayList<>(List.of(new TrackPoint())));

        UploadTripResponseDto response = tripService.processGpxFile(mockFile, "email@example.com");

        assertThat(response.tripId()).isEqualTo(200L);
        assertThat(response.isDuplicate()).isFalse();
        assertThat(response.tripName()).isEqualTo("Nowa trasa GPX");

        verify(gpxParserService).extractTrackPoints(any(Path.class), eq(newTrip));
    }

    @Test
    void mergeTrips_shouldThrowException_whenTryingToMergeSomeoneElsesTrip() {
        TripMergeRequestDto.TripMergeSegment segment = new TripMergeRequestDto.TripMergeSegment(100L, "2023-01-01T10:00:00Z", "2023-01-01T11:00:00Z");
        TripMergeRequestDto request = new TripMergeRequestDto("Połączona trasa", List.of(segment));

        when(userRepository.findByEmail("email2@example.com")).thenReturn(Optional.of(new User()));
        when(tripRepository.findById(100L)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> tripService.mergeTrips(request, "email2@example.com"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void mergeTrips_shouldMergeAndSave_whenRequestIsValid() {
        TripMergeRequestDto.TripMergeSegment segment = new TripMergeRequestDto.TripMergeSegment(100L, "2023-01-01T10:00:00Z", "2023-01-01T11:00:00Z");
        TripMergeRequestDto request = new TripMergeRequestDto("Merged", List.of(segment));

        when(userRepository.findByEmail("email@example.com")).thenReturn(Optional.of(owner));
        when(tripRepository.findById(100L)).thenReturn(Optional.of(trip));

        Trip mergedTrip = new Trip();
        mergedTrip.setId(999L);
        lenient().when(tripRepository.save(any(Trip.class))).thenReturn(mergedTrip);

        TrackPoint mockPoint = new TrackPoint();
        mockPoint.setTime(Instant.parse("2023-01-01T10:30:00Z"));
        mockPoint.setSegmentId(1);
        mockPoint.setTrip(trip);

        when(trackPointRepository.findByTripIdOrderByTimeAsc(100L)).thenReturn(List.of(mockPoint));

        Long newTripId = tripService.mergeTrips(request, "email@example.com");

        assertThat(newTripId).isEqualTo(999L);
        verify(trackPointRepository).saveAll(any());
    }
}