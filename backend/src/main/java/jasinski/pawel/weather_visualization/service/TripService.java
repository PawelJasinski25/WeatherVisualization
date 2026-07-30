package jasinski.pawel.weather_visualization.service;

import jasinski.pawel.weather_visualization.dto.GridReq;
import jasinski.pawel.weather_visualization.dto.TripMergeRequestDto;
import jasinski.pawel.weather_visualization.dto.TripResponseDto;
import jasinski.pawel.weather_visualization.dto.UploadTripResponseDto;
import jasinski.pawel.weather_visualization.entity.TrackPoint;
import jasinski.pawel.weather_visualization.entity.Trip;
import jasinski.pawel.weather_visualization.entity.User;
import jasinski.pawel.weather_visualization.entity.Weather;
import jasinski.pawel.weather_visualization.repository.TrackPointRepository;
import jasinski.pawel.weather_visualization.repository.TripRepository;
import jasinski.pawel.weather_visualization.repository.UserRepository;
import jasinski.pawel.weather_visualization.repository.WeatherRepository;
import jasinski.pawel.weather_visualization.utils.GeoUtils;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.Duration;
import java.util.*;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final TrackPointRepository trackPointRepository;
    private final UserRepository userRepository;
    private final WeatherRepository weatherRepository;
    private final WaterDetectionService waterDetectionService;
    private final TripPersistenceService tripPersistenceService;
    private final GpxParserService gpxParserService;
    private final TripWeatherService tripWeatherService;


    public TripService(TripRepository tripRepository, TrackPointRepository trackPointRepository, UserRepository userRepository, WeatherRepository weatherRepository, WaterDetectionService waterDetectionService, TripPersistenceService tripPersistenceService, GpxParserService gpxParserService, TripWeatherService tripWeatherService) {
        this.tripRepository = tripRepository;
        this.trackPointRepository = trackPointRepository;
        this.userRepository = userRepository;
        this.weatherRepository = weatherRepository;
        this.waterDetectionService = waterDetectionService;
        this.tripPersistenceService = tripPersistenceService;
        this.gpxParserService = gpxParserService;
        this.tripWeatherService = tripWeatherService;
    }



    public UploadTripResponseDto processGpxFile(MultipartFile file, String email) throws Exception {
        long startTime = System.currentTimeMillis();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Brak użytkownika"));

        Path tempFile = null;

        try {
            tempFile = Files.createTempFile("upload_", ".gpx");
            String fileHash = saveFileAndCalculateHash(file, tempFile);


            Optional<Trip> existingTrip = tripRepository.findByFileHashAndUser_Email(fileHash, email);
            if (existingTrip.isPresent()) {
                return new UploadTripResponseDto(existingTrip.get().getId(), true, existingTrip.get().getName());
            }

            Trip savedTrip = tripPersistenceService.createAndSaveTripHeader(file.getOriginalFilename(), user, fileHash);

            List<TrackPoint> allTrackPoints = gpxParserService.extractTrackPoints(tempFile, savedTrip);

            System.out.println("Zredukowano punkty do " + allTrackPoints.size());

            // Budowa siatki zapytań o pogodę
            Map<String, GridReq> uniqueGridRequests = tripWeatherService.buildGridRequests(allTrackPoints);

            int totalLocations = uniqueGridRequests.size();
            int marineLocations = (int) uniqueGridRequests.values().stream()
                    .filter(req -> waterDetectionService.isWater(req.lat(), req.lon()))
                    .count();
            int apiLimitUsed = totalLocations + marineLocations;

            // Pobieranie pogody
            Map<String, OpenMeteoService.OpenMeteoResponse> dailyWeatherCache = new HashMap<>();
            tripWeatherService.fetchWeatherDataFromApi(uniqueGridRequests, dailyWeatherCache);

            List<Weather> weathersToSave = new ArrayList<>();
            tripWeatherService.mapWeatherToTrackPoints(savedTrip, allTrackPoints, dailyWeatherCache, weathersToSave);

            tripWeatherService.fillMarineWeatherGaps(allTrackPoints, weathersToSave);

            // Zapis do bazy
            tripPersistenceService.saveAllDataToDatabase(savedTrip, allTrackPoints, weathersToSave);

            printProcessingReport(allTrackPoints.size(), apiLimitUsed, startTime);

            return new UploadTripResponseDto(savedTrip.getId(), false, savedTrip.getName());

        } catch (Exception e){
            throw new IllegalArgumentException("Nie udało się przetworzyć pliku GPX. Upewnij się, że format jest poprawny.", e);
        }
        finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                    System.out.println("Nie udało się usunąć pliku tymczasowego:" + tempFile.toAbsolutePath());
                }
            }
        }
    }

    private String saveFileAndCalculateHash(MultipartFile file, Path tempFile) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream is = file.getInputStream();
             DigestInputStream dis = new DigestInputStream(is, digest);
             java.io.OutputStream os = Files.newOutputStream(tempFile)) {
            dis.transferTo(os);
        }
        return HexFormat.of().formatHex(digest.digest());
    }



    private void printProcessingReport(int pointsCount, int apiLimitUsed, long startTime) {
        long endTime = System.currentTimeMillis();
        double totalSeconds = (endTime - startTime) / 1000.0;

        System.out.println("====== RAPORT Z PRZETWARZANIA GPX ======");
        System.out.println("Zapisano punktów: " + pointsCount);
        System.out.println("Zużyto zapytań API: " + apiLimitUsed);
        System.out.println("Czas operacji: " + String.format(Locale.US, "%.2f", totalSeconds) + " s");
        System.out.println("========================================");
    }


    @Transactional
    public Long mergeTrips(TripMergeRequestDto request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Brak użytkownika"));

        Trip newTrip = new Trip();
        newTrip.setName(request.newTripName());
        newTrip.setUser(user);
        newTrip.setFileHash("merged_" + System.currentTimeMillis());
        Trip savedTrip = tripRepository.save(newTrip);

        List<TrackPoint> allPointsToClone = new ArrayList<>();

        for (TripMergeRequestDto.TripMergeSegment segDto : request.segments()) {
            Trip originalTrip = tripRepository.findById(segDto.tripId())
                    .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono trasy"));

            if (!originalTrip.getUser().getEmail().equals(email)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Brak uprawnień");
            }

            List<TrackPoint> originalPoints = trackPointRepository.findByTripIdOrderByTimeAsc(segDto.tripId());
            if (originalPoints.isEmpty()) continue;

            Instant trimStart = Instant.parse(segDto.trimStartTime());
            Instant trimEnd = Instant.parse(segDto.trimEndTime());

            for (TrackPoint originalPt : originalPoints) {
                if (originalPt.getTime().isBefore(trimStart) || originalPt.getTime().isAfter(trimEnd)) {
                    continue;
                }
                allPointsToClone.add(originalPt);
            }
        }

        if (allPointsToClone.isEmpty()) {
            throw new IllegalStateException("Po przycięciu nie pozostał żaden punkt.");
        }

        allPointsToClone.sort(Comparator.comparing(TrackPoint::getTime));

        //redukcja punktów
        List<TrackPoint> reducedPoints = new ArrayList<>();
        Instant lastAcceptedTime = null;

        long MIN_INTERVAL_SECONDS = 60;

        for (TrackPoint pt : allPointsToClone) {
            if (lastAcceptedTime == null) {
                reducedPoints.add(pt);
                lastAcceptedTime = pt.getTime();
                continue;
            }

            long gap = Duration.between(lastAcceptedTime, pt.getTime()).getSeconds();

            if (gap < MIN_INTERVAL_SECONDS) {
                continue;
            }

            reducedPoints.add(pt);
            lastAcceptedTime = pt.getTime();
        }

        Map<Long, Weather> clonedWeatherMap = new HashMap<>();
        List<TrackPoint> batchPoints = new ArrayList<>();

        int currentNewSegmentId = 1;
        TrackPoint lastProcessedPoint = null;
        Long lastOriginalTripId = null;
        Integer lastOriginalSegmentId = null;

        long MAX_TIME_GAP_SECONDS = 5 * 60;
        double MAX_DISTANCE_METERS = 800.0;

        for (TrackPoint originalPt : reducedPoints) {

            if (lastProcessedPoint != null) {
                boolean isNewTripFile = !originalPt.getTrip().getId().equals(lastOriginalTripId);

                if (isNewTripFile) {
                    long timeGap = java.time.Duration.between(lastProcessedPoint.getTime(), originalPt.getTime()).abs().getSeconds();
                    double distanceGap = GeoUtils.calculateDistance(
                            lastProcessedPoint.getLatitude(), lastProcessedPoint.getLongitude(), originalPt.getLatitude(), originalPt.getLongitude());

                    if (timeGap > MAX_TIME_GAP_SECONDS || distanceGap > MAX_DISTANCE_METERS) {
                        currentNewSegmentId++;
                    }
                } else {
                    if (!originalPt.getSegmentId().equals(lastOriginalSegmentId)) {
                        currentNewSegmentId++;
                    }
                }
            }

            TrackPoint clonedPt = new TrackPoint();
            clonedPt.setTrip(savedTrip);
            clonedPt.setSegmentId(currentNewSegmentId);
            clonedPt.setTime(originalPt.getTime());
            clonedPt.setSpeed(originalPt.getSpeed());
            clonedPt.setLongitude(originalPt.getLongitude());
            clonedPt.setLatitude(originalPt.getLatitude());

            if (originalPt.getWeather() != null) {
                Long originalWeatherId = originalPt.getWeather().getId();
                Weather clonedWeather = clonedWeatherMap.get(originalWeatherId);

                if (clonedWeather == null) {
                    Weather ow = originalPt.getWeather();
                    clonedWeather = new Weather();
                    clonedWeather.setTrip(savedTrip);
                    clonedWeather.setTime(ow.getTime());
                    clonedWeather.setLatitude(ow.getLatitude());
                    clonedWeather.setLongitude(ow.getLongitude());
                    clonedWeather.setTemp(ow.getTemp());
                    clonedWeather.setWindSpeed(ow.getWindSpeed());
                    clonedWeather.setWindDir(ow.getWindDir());
                    clonedWeather.setDewPoint(ow.getDewPoint());
                    clonedWeather.setWindGusts(ow.getWindGusts());
                    clonedWeather.setRain(ow.getRain());
                    clonedWeather.setSnowfall(ow.getSnowfall());
                    clonedWeather.setHumidity(ow.getHumidity());
                    clonedWeather.setPressure(ow.getPressure());
                    clonedWeather.setCloudCover(ow.getCloudCover());
                    clonedWeather.setCloudCoverLow(ow.getCloudCoverLow());
                    clonedWeather.setCloudCoverMid(ow.getCloudCoverMid());
                    clonedWeather.setCloudCoverHigh(ow.getCloudCoverHigh());
                    clonedWeather.setWaveHeight(ow.getWaveHeight());
                    clonedWeather.setWavePeriod(ow.getWavePeriod());
                    clonedWeather.setWaveDirection(ow.getWaveDirection());
                    clonedWeather.setWindWaveHeight(ow.getWindWaveHeight());
                    clonedWeather.setWindWavePeriod(ow.getWindWavePeriod());
                    clonedWeather.setSwellWaveHeight(ow.getSwellWaveHeight());
                    clonedWeather.setSwellWavePeriod(ow.getSwellWavePeriod());
                    clonedWeather.setOceanCurrentVelocity(ow.getOceanCurrentVelocity());
                    clonedWeather.setOceanCurrentDirection(ow.getOceanCurrentDirection());
                    clonedWeather.setSeaTemperature(ow.getSeaTemperature());
                    clonedWeather.setWeatherCode(ow.getWeatherCode());

                    clonedWeather = weatherRepository.save(clonedWeather);
                    clonedWeatherMap.put(originalWeatherId, clonedWeather);
                }
                clonedPt.setWeather(clonedWeather);
            }
            batchPoints.add(clonedPt);

            if (batchPoints.size() >= 1000) {
                trackPointRepository.saveAll(batchPoints);
                batchPoints.clear();
            }

            lastProcessedPoint = originalPt;
            lastOriginalTripId = originalPt.getTrip().getId();
            lastOriginalSegmentId = originalPt.getSegmentId();
        }

        if (!batchPoints.isEmpty()) {
            trackPointRepository.saveAll(batchPoints);
        }

        savedTrip.setStartTime(reducedPoints.get(0).getTime());
        savedTrip.setEndTime(reducedPoints.get(reducedPoints.size() - 1).getTime());
        tripRepository.save(savedTrip);

        return savedTrip.getId();
    }

    public byte[] exportTripToGpx(Long tripId, String email) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono trasy"));

        if (!trip.getUser().getEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Brak uprawnień");
        }

        List<TrackPoint> points = trackPointRepository.findByTripIdOrderByTimeAsc(tripId);
        String cleanTripName = trip.getName().replaceAll("(?i)\\.gpx$", "").trim();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            XMLOutputFactory factory = XMLOutputFactory.newInstance();
            XMLStreamWriter writer = factory.createXMLStreamWriter(baos, "UTF-8");

            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeStartElement("gpx");
            writer.writeAttribute("version", "1.1");
            writer.writeAttribute("creator", "WeatherVisualizationApp");

            writer.writeStartElement("trk");

            writer.writeStartElement("name");
            writer.writeCharacters(cleanTripName);
            writer.writeEndElement();

            Integer currentSegment = null;

            for (TrackPoint pt : points) {
                if (currentSegment == null || !currentSegment.equals(pt.getSegmentId())) {
                    if (currentSegment != null) {
                        writer.writeEndElement();
                    }
                    writer.writeStartElement("trkseg");
                    currentSegment = pt.getSegmentId();
                }

                writer.writeStartElement("trkpt");
                writer.writeAttribute("lat", String.valueOf(pt.getLatitude()));
                writer.writeAttribute("lon", String.valueOf(pt.getLongitude()));

                if (pt.getTime() != null) {
                    writer.writeStartElement("time");
                    writer.writeCharacters(pt.getTime().toString());
                    writer.writeEndElement();
                }

                if (pt.getSpeed() != null) {
                    writer.writeStartElement("speed");
                    writer.writeCharacters(String.valueOf(pt.getSpeed()));
                    writer.writeEndElement();
                }

                writer.writeEndElement();
            }

            if (currentSegment != null) {
                writer.writeEndElement();
            }

            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndDocument();

            writer.flush();
            writer.close();

            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Błąd podczas generowania pliku GPX", e);
        }
    }

    public List<TripResponseDto> getUserTrips(String email){
        return tripRepository.findByUser_Email(email).stream()
                .map(t -> new TripResponseDto(
                        t.getId(), t.getName(), t.getFileHash(), t.getStartTime(), t.getEndTime()
                )).toList();
    }

    @Transactional
    public Trip updateTripName(Long tripId, String newName, String email){
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono trasy"));

        if (!trip.getUser().getEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Brak uprawnień do edycji tej trasy");
        }

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

        trackPointRepository.deleteAllByTripId(tripId);
        weatherRepository.deleteAllByTripId(tripId);
        tripRepository.delete(trip);
    }
}