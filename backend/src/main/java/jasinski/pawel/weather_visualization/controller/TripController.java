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


    @GetMapping("/{id}/coordinates")
    public ResponseEntity<List<Double[]>> getTripCoordinates(@PathVariable Long id){

        List<TrackPoint> points = trackPointRepository.findByTripIdOrderByTimeAsc(id);
        List<Double[]> coordinates = new ArrayList<>();

        for (TrackPoint point : points) {
            double latitude = point.getLocation().getY();
            double longitude = point.getLocation().getX();
            double segment = point.getSegmentId() != null ? point.getSegmentId() : 0.0;
            double timeMs = point.getTime() != null ? (double) point.getTime().toEpochMilli() : 0.0;


            Double windSpeed = null, temp = null, gusts = null, dewPoint = null;
            Double rain = null, snowfall = null, humidity = null, pressure = null, windDir = null;
            Double cloudCover = null, cloudLow = null, cloudMid = null, cloudHigh = null;
            Double waveHeight = null, wavePeriod = null, waveDir = null;


            if (point.getWeather() != null) {
                jasinski.pawel.weather_visualization.entity.Weather w = point.getWeather();

                windSpeed = w.getWindSpeed();
                temp = w.getTemp();
                gusts = w.getWindGusts();
                dewPoint = w.getDewPoint();
                rain = w.getRain();
                snowfall = w.getSnowfall();
                pressure = w.getPressure();

                humidity = w.getHumidity() != null ? w.getHumidity().doubleValue() : null;
                windDir = w.getWindDir() != null ? w.getWindDir().doubleValue() : null;
                cloudCover = w.getCloudCover() != null ? w.getCloudCover().doubleValue() : null;
                cloudLow = w.getCloudCoverLow() != null ? w.getCloudCoverLow().doubleValue() : null;
                cloudMid = w.getCloudCoverMid() != null ? w.getCloudCoverMid().doubleValue() : null;
                cloudHigh = w.getCloudCoverHigh() != null ? w.getCloudCoverHigh().doubleValue() : null;
                waveHeight = w.getWaveHeight();
                wavePeriod = w.getWavePeriod();
                waveDir = w.getWaveDirection() != null ? w.getWaveDirection().doubleValue() : null;
            }

            Double[] row = new Double[]{
                    latitude,   // 0
                    longitude,  // 1
                    segment,    // 2
                    timeMs,     // 3
                    windSpeed,  // 4
                    temp,       // 5
                    gusts,      // 6
                    dewPoint,   // 7
                    rain,       // 8
                    humidity,   // 9
                    pressure,   // 10
                    cloudCover, // 11
                    cloudLow,   // 12
                    cloudMid,   // 13
                    cloudHigh,  // 14
                    windDir,     // 15
                    snowfall,    //16
                    waveHeight, // 17
                    wavePeriod, // 18
                    waveDir     // 19
            };

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
