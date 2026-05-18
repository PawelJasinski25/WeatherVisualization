package jasinski.pawel.weather_visualization.service;

import io.jenetics.jpx.*;
import jakarta.transaction.Transactional;
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
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.locationtech.jts.geom.Point;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;


import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final TrackPointRepository trackPointRepository;
    private final UserRepository userRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private final WeatherRepository weatherRepository;
    private final OpenMeteoService openMeteoService;

    public TripService(TripRepository tripRepository, TrackPointRepository trackPointRepository, UserRepository userRepository, WeatherRepository weatherRepository, OpenMeteoService openMeteoService) {
        this.tripRepository = tripRepository;
        this.trackPointRepository = trackPointRepository;
        this.userRepository = userRepository;
        this.weatherRepository = weatherRepository;
        this.openMeteoService = openMeteoService;
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

        Optional<Trip> existingTrip = tripRepository.findByFileHashAndUser_Email(fileHash, email);
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

        List<Weather> tripWeatherList = new ArrayList<>();

        Map<String, OpenMeteoService.OpenMeteoResponse> dailyWeatherCache = new HashMap<>();
        Map<String, Weather> savedWeatherCache = new HashMap<>();
        Set<String> processedGridHours = new HashSet<>();

        Instant tripStart = null;
        Instant tripEnd = null;

        for(Track track : gpx.getTracks()) {
            for(TrackSegment segment : track.getSegments()) {
                currentSegmentId++;
                for(WayPoint gpxPoint : segment.getPoints()) {
                    if(gpxPoint.getTime().isEmpty()) continue;

                    Instant ptTime = gpxPoint.getTime().get();
                    if (tripStart == null || ptTime.isBefore(tripStart)) tripStart = ptTime;
                    if (tripEnd == null || ptTime.isAfter(tripEnd)) tripEnd = ptTime;

                    TrackPoint trackPoint = new TrackPoint();
                    trackPoint.setTrip(savedTrip);
                    trackPoint.setTime(gpxPoint.getTime().get());
                    trackPoint.setSegmentId(currentSegmentId);

                    Double extractedSpeed = null;
                    if (gpxPoint.getExtensions().isPresent()) {
                        Document extensionsDoc = gpxPoint.getExtensions().get();
                        NodeList naviSpeedNodes = extensionsDoc.getElementsByTagName("*");
                        for (int i = 0; i < naviSpeedNodes.getLength(); i++) {
                            Node node = naviSpeedNodes.item(i);
                            String localName = node.getLocalName();
                            String fullName = node.getNodeName();

                            if ("navionics_speed".equals(localName) || fullName.endsWith("navionics_speed")) {
                                try {
                                    double speed = Double.parseDouble(node.getTextContent());
                                    extractedSpeed = speed * 3.6; // Konwersja na km/h
                                    break;
                                } catch (Exception ignored) {}
                            } else if ("speed".equals(localName) || fullName.endsWith(":speed")) {
                                try {
                                    double ms = Double.parseDouble(node.getTextContent());
                                    extractedSpeed = ms * 3.6;
                                    break;
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                    trackPoint.setSpeed(extractedSpeed);

                    if(gpxPoint.getElevation().isPresent()){
                        trackPoint.setElevation(gpxPoint.getElevation().get().doubleValue());
                    } else {
                        trackPoint.setElevation(0.0);
                    }

                    double latitude = gpxPoint.getLatitude().doubleValue();
                    double longitude = gpxPoint.getLongitude().doubleValue();
                    Point geomPoint = geometryFactory.createPoint(new Coordinate(longitude, latitude));
                    trackPoint.setLocation(geomPoint);


                    ptTime = gpxPoint.getTime().get();
                    String currentHourKey = ptTime.toString().substring(0, 13); // np. "2023-07-15T14"

                    // Tworzymy siatkę ~11km (zaokrąglanie do 1 miejsca po przecinku)
                    double gridLat = Math.round(latitude * 10.0) / 10.0;
                    double gridLon = Math.round(longitude * 10.0) / 10.0;

                    // Klucz: Godzina + Zaokrąglone współrzędne
                    String gridHourKey = currentHourKey + "_" + gridLat + "_" + gridLon;

                    Weather currentPointWeather = savedWeatherCache.get(gridHourKey);

                    if (currentPointWeather == null && !processedGridHours.contains(gridHourKey)) {
                        processedGridHours.add(gridHourKey);

                        Weather fetchedWeather = openMeteoService.fetchWeather(savedTrip, latitude, longitude, ptTime, dailyWeatherCache);

                        if (fetchedWeather != null) {
                            currentPointWeather = weatherRepository.save(fetchedWeather);
                            savedWeatherCache.put(gridHourKey, currentPointWeather);
                        }
                    }

                    if (currentPointWeather != null) {
                        trackPoint.setWeather(currentPointWeather);
                    }


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

        savedTrip.setStartTime(tripStart);
        savedTrip.setEndTime(tripEnd);
        tripRepository.save(savedTrip);

        System.out.println("KONIEC! Łącznie zapisano: " + counter + " punktów w " + currentSegmentId + " segmentach.");

        return savedTrip.getId();
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

                    double distanceGap = jasinski.pawel.weather_visualization.utils.GeoUtils.calculateDistance(
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
                .map(t -> new jasinski.pawel.weather_visualization.dto.TripResponseDto(
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