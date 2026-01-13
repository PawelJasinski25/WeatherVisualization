package jasinski.pawel.weather_visualization.service;
import io.jenetics.jpx.*;
import jakarta.transaction.Transactional;
import jasinski.pawel.weather_visualization.entity.TrackPoint;
import jasinski.pawel.weather_visualization.entity.Trip;
import jasinski.pawel.weather_visualization.entity.User;
import jasinski.pawel.weather_visualization.repository.TrackPointRepository;
import jasinski.pawel.weather_visualization.repository.TripRepository;
import jasinski.pawel.weather_visualization.repository.UserRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.locationtech.jts.geom.Point;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final TrackPointRepository trackPointRepository;
    private final UserRepository userRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public TripService(TripRepository tripRepository, TrackPointRepository trackPointRepository, UserRepository userRepository) {
        this.tripRepository = tripRepository;
        this.trackPointRepository = trackPointRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Long processGpxFile(MultipartFile file, String email) throws IOException {

        //wczytywanie pliku
        InputStream inputStream = file.getInputStream();
        GPX gpx = GPX.Reader.DEFAULT.read(inputStream);
        inputStream.close();

        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Brak użytkownika o podanym adresie email"));

        //zapisywanie wycieczki
        Trip trip = new Trip();
        String tripName = file.getOriginalFilename();
        if (gpx.getMetadata().isPresent() && gpx.getMetadata().get().getName().isPresent()) {
            tripName = gpx.getMetadata().get().getName().get();
        }
        trip.setName(tripName);
        trip.setUser(user);
        Trip savedTrip = tripRepository.save(trip);


        //przetwarzanie punktów

        List<TrackPoint> batchPoints = new ArrayList<>();
        int counter = 0;

        for(Track track : gpx.getTracks()) {
            for(TrackSegment segment : track.getSegments()) {
                for(WayPoint gpxPoint : segment.getPoints()) {
                    if(gpxPoint.getTime().isEmpty()){
                        continue;
                    }
                    TrackPoint trackPoint = new TrackPoint();
                    trackPoint.setTrip(savedTrip);
                    trackPoint.setTime(gpxPoint.getTime().get());

                    if(gpxPoint.getElevation().isPresent()){
                        trackPoint.setElevation(gpxPoint.getElevation().get().doubleValue());
                    }
                    else {
                        trackPoint.setElevation(0.0);
                    }

                    double latitude = gpxPoint.getLatitude().doubleValue();
                    double longitude = gpxPoint.getLongitude().doubleValue();
                    Point geomPoint = geometryFactory.createPoint(new Coordinate(longitude, latitude));
                    trackPoint.setLocation(geomPoint);

                    batchPoints.add(trackPoint);
                    counter++;

                    if (batchPoints.size() >= 1000) {
                        trackPointRepository.saveAll(batchPoints);
                        trackPointRepository.flush();
                        batchPoints.clear();
                    }
                }

            }

        }

        if (!batchPoints.isEmpty()) {
            trackPointRepository.saveAll(batchPoints);
        }
        System.out.println("KONIEC! Łącznie zapisano: " + counter + " punktów.");

        return savedTrip.getId();

    }


}
