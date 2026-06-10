package jasinski.pawel.weather_visualization.service;

import jasinski.pawel.weather_visualization.entity.TrackPoint;
import jasinski.pawel.weather_visualization.entity.Trip;
import jasinski.pawel.weather_visualization.entity.User;
import jasinski.pawel.weather_visualization.entity.Weather;
import jasinski.pawel.weather_visualization.repository.TrackPointRepository;
import jasinski.pawel.weather_visualization.repository.TripRepository;
import jasinski.pawel.weather_visualization.repository.WeatherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TripPersistenceService {
    private final TripRepository tripRepository;
    private final TrackPointRepository trackPointRepository;
    private final WeatherRepository weatherRepository;

    public TripPersistenceService(TripRepository tripRepository, TrackPointRepository trackPointRepository, WeatherRepository weatherRepository) {
        this.tripRepository = tripRepository;
        this.trackPointRepository = trackPointRepository;
        this.weatherRepository = weatherRepository;
    }


    @Transactional
    public void saveAllDataToDatabase(Trip savedTrip, List<TrackPoint> trackPoints, List<Weather> weathers) {
        weatherRepository.saveAll(weathers);
        trackPointRepository.saveAll(trackPoints);

        if (!trackPoints.isEmpty()) {
            savedTrip.setStartTime(trackPoints.get(0).getTime());
            savedTrip.setEndTime(trackPoints.get(trackPoints.size() - 1).getTime());
            tripRepository.save(savedTrip);
        }
    }

    public Trip createAndSaveTripHeader(String originalFilename, User user, String fileHash) {
        Trip trip = new Trip();
        trip.setName(originalFilename);
        trip.setUser(user);
        trip.setFileHash(fileHash);
        return tripRepository.save(trip);
    }
}
