package jasinski.pawel.weather_visualization.service;

import io.jenetics.jpx.*;
import jasinski.pawel.weather_visualization.dto.TripMergeRequestDto;
import jasinski.pawel.weather_visualization.dto.TripResponseDto;
import jasinski.pawel.weather_visualization.entity.TrackPoint;
import jasinski.pawel.weather_visualization.entity.Trip;
import jasinski.pawel.weather_visualization.entity.User;
import jasinski.pawel.weather_visualization.entity.Weather;
import jasinski.pawel.weather_visualization.repository.TrackPointRepository;
import jasinski.pawel.weather_visualization.repository.TripRepository;
import jasinski.pawel.weather_visualization.repository.UserRepository;
import jasinski.pawel.weather_visualization.repository.WeatherRepository;
import jasinski.pawel.weather_visualization.utils.GeoUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final TrackPointRepository trackPointRepository;
    private final UserRepository userRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private final WeatherRepository weatherRepository;
    private final OpenMeteoService openMeteoService;
    private final WaterDetectionService waterDetectionService;

    private record GridReq(String dateStr, double lat, double lon, String cacheKey) {}
    private record GpxParseResult(List<TrackPoint> points, Map<String, GridReq> gridRequests) {}

    public TripService(TripRepository tripRepository, TrackPointRepository trackPointRepository, UserRepository userRepository, WeatherRepository weatherRepository, OpenMeteoService openMeteoService, WaterDetectionService waterDetectionService) {
        this.tripRepository = tripRepository;
        this.trackPointRepository = trackPointRepository;
        this.userRepository = userRepository;
        this.weatherRepository = weatherRepository;
        this.openMeteoService = openMeteoService;
        this.waterDetectionService = waterDetectionService;
    }

    public Long processGpxFile(MultipartFile file, String email) throws IOException, NoSuchAlgorithmException {
        long startTime = System.currentTimeMillis();


        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Brak użytkownika"));

        String fileHash = calculateFileHash(file.getBytes());
        Optional<Trip> existingTrip = tripRepository.findByFileHashAndUser_Email(fileHash, email);
        if (existingTrip.isPresent()) {
            return existingTrip.get().getId();
        }

        Trip savedTrip = createAndSaveTripHeader(file, user, fileHash);

        // Parsowanie gpx
        GpxParseResult parseResult = parseGpxAndBuildGrid(file.getInputStream(), savedTrip);
        List<TrackPoint> allTrackPoints = parseResult.points();
        Map<String, GridReq> uniqueGridRequests = parseResult.gridRequests();

        int totalLocations = uniqueGridRequests.size();
        int marineLocations = (int) uniqueGridRequests.values().stream()
                .filter(req -> waterDetectionService.isWater(req.lat(), req.lon()))
                .count();
        int apiLimitUsed = totalLocations + marineLocations;


        // Pobieranie pogody
        Map<String, OpenMeteoService.OpenMeteoResponse> dailyWeatherCache = new HashMap<>();
        fetchWeatherDataFromApi(uniqueGridRequests, dailyWeatherCache);


        List<Weather> weathersToSave = new ArrayList<>();
        mapWeatherToTrackPoints(savedTrip, allTrackPoints, dailyWeatherCache, weathersToSave);


        fillMarineWeatherGaps(allTrackPoints, weathersToSave);

        // Zapis do bazy
        saveAllDataToDatabase(savedTrip, allTrackPoints, weathersToSave);

        printProcessingReport(allTrackPoints.size(), apiLimitUsed, startTime);

        return savedTrip.getId();
    }


    private Trip createAndSaveTripHeader(MultipartFile file, User user, String fileHash) throws IOException {
        GPX gpx = GPX.Reader.DEFAULT.read(file.getInputStream());
        Trip trip = new Trip();
        String tripName = gpx.getMetadata().flatMap(Metadata::getName).orElse(file.getOriginalFilename());
        trip.setName(tripName);
        trip.setUser(user);
        trip.setFileHash(fileHash);
        return tripRepository.save(trip);
    }

    private GpxParseResult parseGpxAndBuildGrid(InputStream inputStream, Trip savedTrip) throws IOException {
        GPX gpx = GPX.Reader.DEFAULT.read(inputStream);
        List<TrackPoint> allTrackPoints = new ArrayList<>();
        Map<String, GridReq> uniqueGridRequests = new HashMap<>();

        int currentSegmentId = 0;

        for (Track track : gpx.getTracks()) {
            for (TrackSegment segment : track.getSegments()) {
                currentSegmentId++;
                for (WayPoint gpxPoint : segment.getPoints()) {
                    if (gpxPoint.getTime().isEmpty()) continue;

                    Instant ptTime = gpxPoint.getTime().get();
                    TrackPoint trackPoint = new TrackPoint();
                    trackPoint.setTrip(savedTrip);
                    trackPoint.setTime(ptTime);
                    trackPoint.setSegmentId(currentSegmentId);
                    trackPoint.setElevation(gpxPoint.getElevation().map(Number::doubleValue).orElse(0.0));

                    double latitude = gpxPoint.getLatitude().doubleValue();
                    double longitude = gpxPoint.getLongitude().doubleValue();
                    trackPoint.setLocation(geometryFactory.createPoint(new Coordinate(longitude, latitude)));

                    trackPoint.setSpeed(extractSpeedFromExtensions(gpxPoint));
                    allTrackPoints.add(trackPoint);

                    // Tworzymy siatkę ~11km (zaokrąglanie do 1 miejsca po przecinku)
                    String dateStr = ptTime.toString().substring(0, 10);
                    double gridLat = Math.round(latitude * 10.0) / 10.0;
                    double gridLon = Math.round(longitude * 10.0) / 10.0;

                    // Klucz to data i zaokrąglone współrzędne
                    String cacheKey = dateStr + "_" + gridLat + "_" + gridLon;

                    uniqueGridRequests.putIfAbsent(cacheKey, new GridReq(dateStr, latitude, longitude, cacheKey));
                }
            }
        }
        return new GpxParseResult(allTrackPoints, uniqueGridRequests);
    }

    private Double extractSpeedFromExtensions(WayPoint gpxPoint) {
        if (gpxPoint.getExtensions().isEmpty()) return null;

        Document extensionsDoc = gpxPoint.getExtensions().get();
        NodeList naviSpeedNodes = extensionsDoc.getElementsByTagName("*");
        for (int i = 0; i < naviSpeedNodes.getLength(); i++) {
            String name = naviSpeedNodes.item(i).getLocalName();
            if ("navionics_speed".equals(name) || "speed".equals(name)) {
                try {
                    return Double.parseDouble(naviSpeedNodes.item(i).getTextContent()) * 3.6;
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private void fetchWeatherDataFromApi(Map<String, GridReq> uniqueGridRequests, Map<String, OpenMeteoService.OpenMeteoResponse> dailyWeatherCache) {
        int CHUNK_SIZE = 50;

        Map<String, List<GridReq>> groupedByDate = uniqueGridRequests.values().stream()
                .collect(Collectors.groupingBy(GridReq::dateStr));

        for (Map.Entry<String, List<GridReq>> entry : groupedByDate.entrySet()) {
            String dateStr = entry.getKey();
            List<GridReq> requests = entry.getValue();

            for (int i = 0; i < requests.size(); i += CHUNK_SIZE) {
                List<GridReq> chunk = requests.subList(i, Math.min(i + CHUNK_SIZE, requests.size()));
                List<Double> lats = chunk.stream().map(GridReq::lat).toList();
                List<Double> lons = chunk.stream().map(GridReq::lon).toList();
                List<String> keys = chunk.stream().map(GridReq::cacheKey).toList();

                openMeteoService.fetchWeatherBatch(dateStr, lats, lons, keys, dailyWeatherCache, waterDetectionService);
            }
        }
    }

    private void mapWeatherToTrackPoints(Trip savedTrip, List<TrackPoint> allTrackPoints, Map<String, OpenMeteoService.OpenMeteoResponse> dailyWeatherCache, List<Weather> weathersToSave) {
        Map<String, Weather> savedWeatherEntitiesCache = new HashMap<>();

        for (TrackPoint pt : allTrackPoints) {
            String dateStr = pt.getTime().toString().substring(0, 10);
            String targetHourStr = pt.getTime().toString().substring(0, 13) + ":00";

            double gridLat = Math.round(pt.getLocation().getY() * 10.0) / 10.0;
            double gridLon = Math.round(pt.getLocation().getX() * 10.0) / 10.0;

            boolean isWater = waterDetectionService.isWater(pt.getLocation().getY(), pt.getLocation().getX());

            String dayCacheKey = dateStr + "_" + gridLat + "_" + gridLon;
            String hourCacheKey = targetHourStr + "_" + gridLat + "_" + gridLon + (isWater ? "_WATER" : "_LAND");

            Weather w = savedWeatherEntitiesCache.get(hourCacheKey);

            if (w == null) {
                OpenMeteoService.OpenMeteoResponse res = dailyWeatherCache.get(dayCacheKey);
                if (res != null) {
                    w = openMeteoService.buildWeatherEntity(savedTrip, pt.getLocation().getY(), pt.getLocation().getX(), pt.getTime(), res);
                    if (w != null) {
                        if (!isWater) {
                            stripMarineDataFromWeather(w);
                        }
                        weathersToSave.add(w);
                        savedWeatherEntitiesCache.put(hourCacheKey, w);
                    }
                }
            }
            pt.setWeather(w);
        }
    }

    private void stripMarineDataFromWeather(Weather w) {
        w.setWaveHeight(null);
        w.setWavePeriod(null);
        w.setWaveDirection(null);
        w.setWindWaveHeight(null);
        w.setWindWavePeriod(null);
        w.setSwellWaveHeight(null);
        w.setSwellWavePeriod(null);
        w.setOceanCurrentVelocity(null);
        w.setOceanCurrentDirection(null);
        w.setSeaTemperature(null);
    }

    private void fillMarineWeatherGaps(List<TrackPoint> allTrackPoints, List<Weather> weathersToSave) {
        Set<Weather> artificiallyPatched = new HashSet<>();
        int MAX_INDEX_LOOKAROUND = 600;
        long MAX_TIME_GAP_SECONDS = 20 * 60;
        double MAX_DISTANCE_METERS = 2500.0;

        for (int i = 0; i < allTrackPoints.size(); i++) {
            TrackPoint pt = allTrackPoints.get(i);
            Weather w = pt.getWeather();

            if (w != null && w.getWaveHeight() == null) {
                Weather closestMarine = null;
                long minTimeDiff = Long.MAX_VALUE;

                int startIdx = Math.max(0, i - MAX_INDEX_LOOKAROUND);
                int endIdx = Math.min(allTrackPoints.size() - 1, i + MAX_INDEX_LOOKAROUND);

                for (int j = startIdx; j <= endIdx; j++) {
                    if (i == j) continue;

                    TrackPoint neighborPt = allTrackPoints.get(j);
                    Weather neighborWeather = neighborPt.getWeather();

                    if (neighborWeather != null && neighborWeather.getWaveHeight() != null && !artificiallyPatched.contains(neighborWeather)) {
                        long timeDiff = Math.abs(java.time.Duration.between(pt.getTime(), neighborPt.getTime()).getSeconds());

                        if (timeDiff <= MAX_TIME_GAP_SECONDS && timeDiff < minTimeDiff) {
                            double dist = GeoUtils.calculateDistance(pt.getLocation(), neighborPt.getLocation());

                            if (dist <= MAX_DISTANCE_METERS) {
                                closestMarine = neighborWeather;
                                minTimeDiff = timeDiff;
                            }
                        }
                    }
                }

                if (closestMarine != null) {
                    Weather patchedWeather = cloneWeatherAndApplyMarineData(w, closestMarine);
                    weathersToSave.add(patchedWeather);
                    pt.setWeather(patchedWeather);
                    artificiallyPatched.add(patchedWeather);
                }
            }
        }
    }

    private Weather cloneWeatherAndApplyMarineData(Weather baseWeather, Weather marineData) {
        Weather patched = new Weather();
        patched.setTrip(baseWeather.getTrip());
        patched.setTime(baseWeather.getTime());
        patched.setLatitude(baseWeather.getLatitude());
        patched.setLongitude(baseWeather.getLongitude());

        patched.setTemp(baseWeather.getTemp());
        patched.setWindSpeed(baseWeather.getWindSpeed());
        patched.setWindDir(baseWeather.getWindDir());
        patched.setDewPoint(baseWeather.getDewPoint());
        patched.setWindGusts(baseWeather.getWindGusts());
        patched.setRain(baseWeather.getRain());
        patched.setSnowfall(baseWeather.getSnowfall());
        patched.setHumidity(baseWeather.getHumidity());
        patched.setPressure(baseWeather.getPressure());
        patched.setCloudCover(baseWeather.getCloudCover());
        patched.setCloudCoverLow(baseWeather.getCloudCoverLow());
        patched.setCloudCoverMid(baseWeather.getCloudCoverMid());
        patched.setCloudCoverHigh(baseWeather.getCloudCoverHigh());
        patched.setWeatherCode(baseWeather.getWeatherCode());

        patched.setWaveHeight(marineData.getWaveHeight());
        patched.setWavePeriod(marineData.getWavePeriod());
        patched.setWaveDirection(marineData.getWaveDirection());
        patched.setWindWaveHeight(marineData.getWindWaveHeight());
        patched.setWindWavePeriod(marineData.getWindWavePeriod());
        patched.setSwellWaveHeight(marineData.getSwellWaveHeight());
        patched.setSwellWavePeriod(marineData.getSwellWavePeriod());
        patched.setOceanCurrentVelocity(marineData.getOceanCurrentVelocity());
        patched.setOceanCurrentDirection(marineData.getOceanCurrentDirection());
        patched.setSeaTemperature(marineData.getSeaTemperature());

        return patched;
    }

    private void saveAllDataToDatabase(Trip savedTrip, List<TrackPoint> allTrackPoints, List<Weather> weathersToSave) {
        weatherRepository.saveAll(weathersToSave);
        trackPointRepository.saveAll(allTrackPoints);

        if (!allTrackPoints.isEmpty()) {
            savedTrip.setStartTime(allTrackPoints.get(0).getTime());
            savedTrip.setEndTime(allTrackPoints.get(allTrackPoints.size() - 1).getTime());
            tripRepository.save(savedTrip);
        }
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

        allPointsToClone.sort((p1, p2) -> p1.getTime().compareTo(p2.getTime()));

        Map<Long, Weather> clonedWeatherMap = new HashMap<>();
        List<TrackPoint> batchPoints = new ArrayList<>();

        int currentNewSegmentId = 1;
        TrackPoint lastProcessedPoint = null;
        Long lastOriginalTripId = null;
        Integer lastOriginalSegmentId = null;

        long MAX_TIME_GAP_SECONDS = 5 * 60;
        double MAX_DISTANCE_METERS = 800.0;

        for (TrackPoint originalPt : allPointsToClone) {

            if (lastProcessedPoint != null) {
                boolean isNewTripFile = !originalPt.getTrip().getId().equals(lastOriginalTripId);

                if (isNewTripFile) {
                    long timeGap = java.time.Duration.between(lastProcessedPoint.getTime(), originalPt.getTime()).abs().getSeconds();

                    double distanceGap = GeoUtils.calculateDistance(
                            lastProcessedPoint.getLocation(), originalPt.getLocation());

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
            clonedPt.setElevation(originalPt.getElevation());
            clonedPt.setSpeed(originalPt.getSpeed());
            clonedPt.setLocation(originalPt.getLocation());

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

        savedTrip.setStartTime(allPointsToClone.get(0).getTime());
        savedTrip.setEndTime(allPointsToClone.get(allPointsToClone.size() - 1).getTime());
        tripRepository.save(savedTrip);

        return savedTrip.getId();
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

        weatherRepository.deleteByTripId(tripId);
        trackPointRepository.deleteByTripId(tripId);
        tripRepository.delete(trip);
    }
}