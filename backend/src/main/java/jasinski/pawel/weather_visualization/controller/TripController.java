package jasinski.pawel.weather_visualization.controller;
import jasinski.pawel.weather_visualization.dto.*;
import jasinski.pawel.weather_visualization.entity.Trip;
import jasinski.pawel.weather_visualization.service.GeoNamesService;
import jasinski.pawel.weather_visualization.service.ReportService;
import jasinski.pawel.weather_visualization.service.TripMapService;
import jasinski.pawel.weather_visualization.service.TripService;
import jasinski.pawel.weather_visualization.utils.TimelineChartGenerator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.util.List;


@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;
    private final ReportService reportService;
    private final TripMapService tripMapService;
    private final GeoNamesService geoNamesService;

    public TripController(TripService tripService, TripMapService tripMapService, ReportService reportService, GeoNamesService geoNamesService) {
        this.tripService = tripService;
        this.tripMapService = tripMapService;
        this.reportService = reportService;
        this.geoNamesService = geoNamesService;
    }

    @GetMapping("/{id}/coordinates")
    public ResponseEntity<MapDataResponse> getTripCoordinates(@PathVariable Long id) {
        MapDataResponse response = tripMapService.getTripMapData(id);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/upload")
    public ResponseEntity<Long> uploadGpxFile(@RequestParam("file") MultipartFile file, Authentication authentication) throws IOException, NoSuchAlgorithmException {
        String email = authentication.getName();
        Long newTripId = tripService.processGpxFile(file, email);
        return ResponseEntity.ok(newTripId);
    }


    @GetMapping
    public ResponseEntity<List<Trip>> getUserTrips(Authentication authentication) {
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



    @GetMapping("/{id}/report/csv")
    public ResponseEntity<byte[]> downloadCsvReport(@PathVariable Long id, Authentication authentication) {
        ReportResource report = reportService.getCsvReportResource(id, authentication.getName());

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + report.fileName() + "\"");
        headers.setContentType(MediaType.parseMediaType("text/csv"));

        return ResponseEntity.ok().headers(headers).body(report.content());
    }

    @GetMapping(value = "/{tripId}/timeline/{dayIndex}/png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getDailyTimelineImage(@PathVariable Long tripId, @PathVariable int dayIndex) {
        List<DailySummary> summaries = reportService.generateDailySummaries(tripId);

        if (dayIndex < 0 || dayIndex >= summaries.size()) {
            return ResponseEntity.notFound().build();
        }

        DailySummary summary = summaries.get(dayIndex);

        byte[] imageBytes = TimelineChartGenerator.generateDailyTimelineChart(
                summary.date(), summary.timelineEvents(), ZoneId.of("Europe/Warsaw"), geoNamesService
        );

        return ResponseEntity.ok(imageBytes);
    }

    @GetMapping(value = "/{tripId}/timeline/all/zip", produces = "application/zip")
    public ResponseEntity<byte[]> getAllTimelinesZip(@PathVariable Long tripId) {
        byte[] zipBytes = reportService.generateAllTimelinesZip(tripId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"wykresy_" + tripId + ".zip\"")
                .body(zipBytes);
    }
}
