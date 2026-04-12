package jasinski.pawel.weather_visualization.controller;

import jasinski.pawel.weather_visualization.dto.TrackPointDto;
import jasinski.pawel.weather_visualization.entity.TrackPoint;
import jasinski.pawel.weather_visualization.entity.Trip;
import jasinski.pawel.weather_visualization.entity.Weather;
import jasinski.pawel.weather_visualization.repository.TrackPointRepository;
import jasinski.pawel.weather_visualization.service.ReportService;
import jasinski.pawel.weather_visualization.service.TripService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;


@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;
    private final TrackPointRepository trackPointRepository;
    private final ReportService reportService;

    public TripController(TripService tripService, TrackPointRepository trackPointRepository, ReportService reportService) {
        this.tripService = tripService;
        this.trackPointRepository = trackPointRepository;
        this.reportService = reportService;
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

    @GetMapping("/{id}/report/csv")
    public ResponseEntity<byte[]> downloadCsvReport(@PathVariable Long id, Authentication authentication) {

        String email = authentication.getName();

        boolean isOwner = tripService.getUserTrips(email).stream()
                .anyMatch(trip -> trip.getId().equals(id));

        if (!isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Brak uprawnień do pobrania tego raportu");
        }

        Trip trip = tripService.getUserTrips(email).stream()
                .filter(t -> t.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Brak uprawnień do pobrania tego raportu"));

        String fileName = trip.getName();
        fileName = fileName.replaceAll("(?i)\\.gpx$", "") + ".csv";



        String csvContent = reportService.generateCsv(id);
        byte[] csvBytes = csvContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] bom = new byte[] { (byte)0xEF, (byte)0xBB, (byte)0xBF };
        byte[] finalBytes = new byte[bom.length + csvBytes.length];
        System.arraycopy(bom, 0, finalBytes, 0, bom.length);
        System.arraycopy(csvBytes, 0, finalBytes, bom.length, csvBytes.length);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
        headers.setContentType(MediaType.parseMediaType("text/csv"));

        return ResponseEntity.ok()
                .headers(headers)
                .body(finalBytes);
    }

}
