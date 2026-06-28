package jasinski.pawel.weather_visualization.service;

import jasinski.pawel.weather_visualization.entity.TrackPoint;
import jasinski.pawel.weather_visualization.entity.Trip;
import jasinski.pawel.weather_visualization.entity.User;
import jasinski.pawel.weather_visualization.entity.Weather;
import jasinski.pawel.weather_visualization.repository.TrackPointRepository;
import jasinski.pawel.weather_visualization.repository.TripRepository;
import jasinski.pawel.weather_visualization.repository.WeatherRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TripPersistenceServiceTest {

    @Mock private TripRepository tripRepository;
    @Mock private TrackPointRepository trackPointRepository;
    @Mock private WeatherRepository weatherRepository;

    @InjectMocks
    private TripPersistenceService tripPersistenceService;


    @Test
    void createAndSaveTripHeader_shouldCreateCorrectTripAndSaveIt() {
        User user = new User();
        user.setEmail("email@example.com");

        tripPersistenceService.createAndSaveTripHeader("Przykładowa trasa", user, "hash123");

        verify(tripRepository).save(argThat(trip ->
                trip.getName().equals("Przykładowa trasa") &&
                        trip.getUser().equals(user) &&
                        trip.getFileHash().equals("hash123")
        ));
    }


    @Test
    void saveAllDataToDatabase_shouldSaveAllCollectionsAndSetTripTimes() {
        Trip trip = new Trip();

        TrackPoint p1 = new TrackPoint();
        p1.setTime(Instant.parse("2023-01-01T10:00:00Z"));
        TrackPoint p2 = new TrackPoint();
        p2.setTime(Instant.parse("2023-01-01T11:00:00Z"));
        TrackPoint p3 = new TrackPoint();
        p3.setTime(Instant.parse("2023-01-01T12:00:00Z"));

        List<TrackPoint> points = List.of(p1, p2, p3);
        List<Weather> weathers = List.of(new Weather());

        tripPersistenceService.saveAllDataToDatabase(trip, points, weathers);

        verify(weatherRepository).saveAll(weathers);
        verify(trackPointRepository).saveAll(points);

        verify(tripRepository).save(argThat(savedTrip ->
                savedTrip.getStartTime().equals(Instant.parse("2023-01-01T10:00:00Z")) &&
                        savedTrip.getEndTime().equals(Instant.parse("2023-01-01T12:00:00Z"))
        ));
    }

    @Test
    void saveAllDataToDatabase_shouldNotUpdateTrip_whenTrackPointsListIsEmpty() {
        Trip trip = new Trip();
        List<TrackPoint> emptyPoints = List.of();
        List<Weather> weathers = List.of(new Weather());

        tripPersistenceService.saveAllDataToDatabase(trip, emptyPoints, weathers);

        verify(weatherRepository).saveAll(weathers);
        verify(trackPointRepository).saveAll(emptyPoints);

        assertThat(trip.getStartTime()).isNull();
        assertThat(trip.getEndTime()).isNull();
        verify(tripRepository, never()).save(any());
    }

    @Test
    void saveAllDataToDatabase_shouldRelyOnListOrderForStartAndEndTimes() {
        Trip trip = new Trip();

        TrackPoint firstInList = new TrackPoint();
        firstInList.setTime(Instant.parse("2023-01-01T12:00:00Z"));

        TrackPoint middleInList = new TrackPoint();
        middleInList.setTime(Instant.parse("2023-01-01T11:00:00Z"));

        TrackPoint lastInList = new TrackPoint();
        lastInList.setTime(Instant.parse("2023-01-01T10:00:00Z"));

        List<TrackPoint> unsortedPoints = List.of(firstInList, middleInList, lastInList);

        tripPersistenceService.saveAllDataToDatabase(trip, unsortedPoints, List.of());

        verify(tripRepository).save(argThat(savedTrip ->
                savedTrip.getStartTime().equals(Instant.parse("2023-01-01T12:00:00Z")) &&
                        savedTrip.getEndTime().equals(Instant.parse("2023-01-01T10:00:00Z"))
        ));
    }

    @Test
    void saveAllDataToDatabase_shouldThrowNullPointerException_whenTrackPointsListIsNull() {
        Trip trip = new Trip();
        List<Weather> emptyWeathers = List.of();

        assertThatThrownBy(() -> tripPersistenceService.saveAllDataToDatabase(trip, null, emptyWeathers))
                .isInstanceOf(NullPointerException.class);
    }
}
