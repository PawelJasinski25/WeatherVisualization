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
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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

    private String calculateFileHash(byte[] fileBytes) throws NoSuchAlgorithmException {
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(fileBytes);
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    @Transactional
    public Long processGpxFile(MultipartFile file, String email) throws IOException, NoSuchAlgorithmException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Brak użytkownika"));


        byte[] fileBytes = file.getBytes();
        String fileHash = calculateFileHash(fileBytes);


        java.util.Optional<Trip> existingTrip = tripRepository.findByFileHashAndUser_Email(fileHash, email);
        if (existingTrip.isPresent()) {
            System.out.println("Trasa już istnieje! Pomijam przetwarzanie i zwracam ID: " + existingTrip.get().getId());
            return existingTrip.get().getId();
        }

        InputStream inputStream = file.getInputStream();
        GPX gpx = GPX.Reader.DEFAULT.read(inputStream);
        inputStream.close();


        Trip trip = new Trip();
        String tripName = file.getOriginalFilename();
        if (gpx.getMetadata().isPresent() && gpx.getMetadata().get().getName().isPresent()) {
            tripName = gpx.getMetadata().get().getName().get();
        }
        trip.setName(tripName);
        trip.setUser(user);
        trip.setFileHash(fileHash);

        Trip savedTrip = tripRepository.save(trip);

        List<TrackPoint> batchPoints = new ArrayList<>();
        int counter = 0;
        int currentSegmentId = 0;

        for(Track track : gpx.getTracks()) {
            for(TrackSegment segment : track.getSegments()) {
                currentSegmentId++;

                for(WayPoint gpxPoint : segment.getPoints()) {
                    if(gpxPoint.getTime().isEmpty()){
                        continue;
                    }

                    TrackPoint trackPoint = new TrackPoint();
                    trackPoint.setTrip(savedTrip);
                    trackPoint.setTime(gpxPoint.getTime().get());

                    trackPoint.setSegmentId(currentSegmentId);

                    if(gpxPoint.getElevation().isPresent()){
                        trackPoint.setElevation(gpxPoint.getElevation().get().doubleValue());
                    } else {
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
                        batchPoints.clear();
                    }
                }
            }
        }

        if (!batchPoints.isEmpty()) {
            trackPointRepository.saveAll(batchPoints);
        }
        System.out.println("KONIEC! Łącznie zapisano: " + counter + " punktów w " + currentSegmentId + " segmentach.");

        return savedTrip.getId();
    }


    public List<Trip> getUserTrips(String email){
        return tripRepository.findByUser_Email(email);
    }

    @Transactional
    public Trip updateTripName(Long tripId, String newName, String email){
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono trasy"));

        trip.setName(newName);
        return tripRepository.save(trip);
    }

    @Transactional
    public void deleteTrip(Long tripId, String email){
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono trasy"));

        if (!trip.getUser().getEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Brak uprawnień do usunięcia tej trasy");
        }

        trackPointRepository.deleteByTripId(tripId);
        tripRepository.delete(trip);
    }


}