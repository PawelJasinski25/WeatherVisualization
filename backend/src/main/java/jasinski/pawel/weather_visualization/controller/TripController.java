package jasinski.pawel.weather_visualization.controller;

import jasinski.pawel.weather_visualization.entity.TrackPoint;
import jasinski.pawel.weather_visualization.repository.TrackPointRepository;
import jasinski.pawel.weather_visualization.service.TripService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

            // Pobieramy czas punktu z PostGIS w postaci unixowego timestampu (milisekundy)
            double timeMs = (double) point.getTime().toEpochMilli();

            // Zwracamy tablicę [szerokość, długość, czas]
            double[] row = new double[]{latitude, longitude, timeMs};
            coordinates.add(row);
        }

        return ResponseEntity.ok(coordinates);
    }

    @PostMapping("/upload")
    public ResponseEntity<Long> uploadGpxFile(@RequestParam("file") MultipartFile file, Authentication authentication) {
        try{
            String email = authentication.getName();
            Long newTripId = tripService.processGpxFile(file, email);
            return ResponseEntity.ok(newTripId);
        }
        catch (Exception e){
            return ResponseEntity.internalServerError().build();
        }
    }

}
