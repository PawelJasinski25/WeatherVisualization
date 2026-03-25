package jasinski.pawel.weather_visualization.controller;

import jasinski.pawel.weather_visualization.entity.TrackPoint;
import jasinski.pawel.weather_visualization.entity.Trip;
import jasinski.pawel.weather_visualization.repository.TrackPointRepository;
import jasinski.pawel.weather_visualization.service.TripService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;
    private final TrackPointRepository trackPointRepository;

    public TripController(TripService tripService, TrackPointRepository trackPointRepository) {
        this.tripService = tripService;
        this.trackPointRepository = trackPointRepository;
    }
//STARA WERSJA DOBRA
//    @GetMapping("/{id}/coordinates")
//    public ResponseEntity<List<double[]>> getTripCoordinates(@PathVariable Long id){
//
//        List<TrackPoint> points = trackPointRepository.findByTripIdOrderByTimeAsc(id);
//        List<double[]> coordinates = new ArrayList<>();
//
//        for (TrackPoint point : points) {
//            double latitude = point.getLocation().getY();
//            double longitude = point.getLocation().getX();
//
//            double[] pair = new double[]{latitude, longitude};
//            coordinates.add(pair);
//        }
//
//        return ResponseEntity.ok(coordinates);
//
//    }


    //NOWA WERSJA POGLADOWA
    @GetMapping("/{id}/coordinates")
    public ResponseEntity<List<double[]>> getTripCoordinates(@PathVariable Long id){

        List<TrackPoint> points = trackPointRepository.findByTripIdOrderByTimeAsc(id);
        List<double[]> coordinates = new ArrayList<>();

        for (TrackPoint point : points) {
            double latitude = point.getLocation().getY();
            double longitude = point.getLocation().getX();

            // Zabezpieczenie na wypadek starszych tras z bazy, gdzie segmentId może być null
            double segment = point.getSegmentId() != null ? point.getSegmentId() : 0.0;

            // Zostawiam czas, jeśli potrzebujesz go w przyszłości
            double timeMs = point.getTime() != null ? (double) point.getTime().toEpochMilli() : 0.0;

            // Zwracamy tablicę: [0: latitude, 1: longitude, 2: segmentId, 3: timeMs]
            double[] row = new double[]{latitude, longitude, segment, timeMs};

            coordinates.add(row);
        }

        return ResponseEntity.ok(coordinates);
    }

    @PostMapping("/upload")
    public ResponseEntity<Long> uploadGpxFile(@RequestParam("file") MultipartFile file, Authentication authentication) throws IOException, NoSuchAlgorithmException {
            String email = authentication.getName();
            Long newTripId = tripService.processGpxFile(file, email);
            return ResponseEntity.ok(newTripId);
    }


    @GetMapping
    public ResponseEntity<List<Trip>> getUserTrips(Authentication authentication){
        String email = authentication.getName();
        return ResponseEntity.ok(tripService.getUserTrips(email));
    }

    @PutMapping("/{id}/name")
    public ResponseEntity<Trip> updateTripName(@PathVariable Long id, @RequestBody String newName, Authentication authentication) {
            String email = authentication.getName();
            newName = newName.replace("\"", "");
            Trip updatedTrip = tripService.updateTripName(id, newName, email);
            return ResponseEntity.ok(updatedTrip);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrip(@PathVariable Long id, Authentication authentication) {
            String email = authentication.getName();
            tripService.deleteTrip(id, email);
            return ResponseEntity.ok().build();
    }

}
