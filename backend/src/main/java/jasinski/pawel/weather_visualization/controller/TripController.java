package jasinski.pawel.weather_visualization.controller;
import jasinski.pawel.weather_visualization.dto.*;
import jasinski.pawel.weather_visualization.entity.Trip;
import jasinski.pawel.weather_visualization.service.GeoNamesService;
import jasinski.pawel.weather_visualization.service.ReportService;
import jasinski.pawel.weather_visualization.service.TripMapService;
import jasinski.pawel.weather_visualization.service.TripService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;


@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;
    private final ReportService reportService;
    private final TripMapService tripMapService;

    public TripController(TripService tripService, TripMapService tripMapService, ReportService reportService, GeoNamesService geoNamesService) {
        this.tripService = tripService;
        this.tripMapService = tripMapService;
        this.reportService = reportService;
    }

    @GetMapping("/{id}/coordinates")
    public ResponseEntity<MapDataResponse> getTripCoordinates(@PathVariable Long id) {
        MapDataResponse response = tripMapService.getTripMapData(id);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/upload")
    public ResponseEntity<UploadTripResponseDto> uploadGpxFile(@RequestParam("file") MultipartFile file, Authentication authentication) throws Exception {
        String email = authentication.getName();
        UploadTripResponseDto response = tripService.processGpxFile(file, email);
        return ResponseEntity.ok(response);
    }


    @GetMapping
    public ResponseEntity<List<TripResponseDto>> getUserTrips(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(tripService.getUserTrips(email));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateTripName(@PathVariable Long id, @RequestBody java.util.Map<String, String> payload, Authentication authentication) {
        String newName = payload.get("name");
        String email = authentication.getName();

        tripService.updateTripName(id, newName, email);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrip(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        tripService.deleteTrip(id, email);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/report-data")
    public ResponseEntity<TripReportDataDto> getReportData(@PathVariable Long id, Authentication authentication) {

        String email = authentication.getName();
        TripReportDataDto reportData = reportService.getTripReportData(id, email);

        return ResponseEntity.ok(reportData);
    }



    @GetMapping("/{id}/report/csv")
    public ResponseEntity<byte[]> downloadCsvReport(@PathVariable Long id, Authentication authentication) {
        ReportResource report = reportService.getCsvReportResource(id, authentication.getName());


        ContentDisposition contentDisposition = org.springframework.http.ContentDisposition.builder("attachment")
                .filename(report.fileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.valueOf("application/zip"))
                .body(report.content());
    }

    public record PdfDownloadRequest(List<String> modules, java.util.Map<String, Object> reportData) {}

    @PostMapping("/{tripId}/download-pdf")
    public ResponseEntity<byte[]> downloadReport(
            @PathVariable Long tripId,
            @RequestBody PdfDownloadRequest request,
            Authentication authentication) {

        ReportResource report = reportService.generatePdfReportResource(tripId, authentication.getName(), request.reportData());

        ContentDisposition contentDisposition = org.springframework.http.ContentDisposition.builder("attachment")
                .filename(report.fileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(report.content());
    }

    @PostMapping("/merge")
    public ResponseEntity<Long> mergeTrips(@RequestBody TripMergeRequestDto request, Authentication authentication) {
        String email = authentication.getName();
        Long newTripId = tripService.mergeTrips(request, email);
        return ResponseEntity.ok(newTripId);
    }

    @GetMapping("/{id}/export/gpx")
    public ResponseEntity<byte[]> exportGpx(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        byte[] gpxData = tripService.exportTripToGpx(id, email);

        ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                .filename("trasa_" + id + ".gpx", StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.APPLICATION_XML)
                .body(gpxData);
    }
}
