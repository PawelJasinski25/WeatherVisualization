package jasinski.pawel.weather_visualization.controller;

import jasinski.pawel.weather_visualization.dto.TrackPointDto;
import jasinski.pawel.weather_visualization.entity.TrackPoint;
import jasinski.pawel.weather_visualization.entity.Trip;
import jasinski.pawel.weather_visualization.entity.Weather;
import jasinski.pawel.weather_visualization.repository.TrackPointRepository;
import jasinski.pawel.weather_visualization.service.TripService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
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


    @GetMapping("/{id}/coordinates")
    public ResponseEntity<List<TrackPointDto>> getTripCoordinates(@PathVariable Long id) {
        List<TrackPoint> points = trackPointRepository.findByTripIdOrderByTimeAsc(id);

        List<TrackPointDto> coordinates = points.stream()
                .map(this::mapToDto)
                .toList();

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

    private TrackPointDto mapToDto(TrackPoint point) {
        Weather w = point.getWeather();

        double lat = point.getLocation().getY();
        double lon = point.getLocation().getX();
        double segmentId = point.getSegmentId() != null ? point.getSegmentId() : 0.0;
        double timeMs = point.getTime() != null ? (double) point.getTime().toEpochMilli() : 0.0;

        if (w == null) {
            return new TrackPointDto(lat, lon, segmentId, timeMs);
        }

        return new TrackPointDto(
                lat, lon, segmentId, timeMs,
                w.getWindSpeed(), w.getTemp(), w.getWindGusts(), w.getDewPoint(), w.getRain(),
                w.getHumidity(), w.getPressure(), w.getCloudCover(), w.getCloudCoverLow(),
                w.getCloudCoverMid(), w.getCloudCoverHigh(), w.getWindDir(), w.getSnowfall(),
                w.getWaveHeight(), w.getWavePeriod(), w.getWaveDirection(), w.getWindWaveHeight(),
                w.getWindWavePeriod(), w.getSwellWaveHeight(), w.getSwellWavePeriod(),
                w.getOceanCurrentVelocity(), w.getSeaTemperature(), w.getOceanCurrentDirection()
        );
    }

}
