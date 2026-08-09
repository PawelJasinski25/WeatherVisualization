package jasinski.pawel.weather_visualization.service;

import jasinski.pawel.weather_visualization.dto.GridReq;
import jasinski.pawel.weather_visualization.entity.TrackPoint;
import jasinski.pawel.weather_visualization.entity.Trip;
import jasinski.pawel.weather_visualization.entity.Weather;
import jasinski.pawel.weather_visualization.utils.GeoUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TripWeatherService {

    private final WaterDetectionService waterDetectionService;
    private final OpenMeteoService openMeteoService;

    public TripWeatherService(WaterDetectionService waterDetectionService, OpenMeteoService openMeteoService){
        this.waterDetectionService = waterDetectionService;
        this.openMeteoService = openMeteoService;
    }

    private String getRoundedDateStr(Instant time) {
        Instant roundedTime = time.plus(30, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.HOURS);
        return roundedTime.toString().substring(0, 10);
    }

    private String getDailyGridCacheKey(String dateStr, double lat, double lon) {
        double gridLat = Math.round(lat * 10.0) / 10.0;
        double gridLon = Math.round(lon * 10.0) / 10.0;
        return dateStr + "_" + gridLat + "_" + gridLon;
    }

    public Map<String, List<TrackPoint>> groupTrackPointsByGrid(List<TrackPoint> points) {
        Map<String, List<TrackPoint>> grouped = new HashMap<>();
        for (TrackPoint pt : points) {
            String dateStr = getRoundedDateStr(pt.getTime());
            String cacheKey = getDailyGridCacheKey(dateStr, pt.getLatitude(), pt.getLongitude());
            grouped.computeIfAbsent(cacheKey, k -> new ArrayList<>()).add(pt);
        }
        return grouped;
    }

    public Map<String, GridReq> buildGridRequests(Map<String, List<TrackPoint>> groupedPoints) {
        Map<String, GridReq> uniqueGridRequests = new HashMap<>();

        for (Map.Entry<String, List<TrackPoint>> entry : groupedPoints.entrySet()) {
            String cacheKey = entry.getKey();
            TrackPoint samplePt = entry.getValue().get(0);
            String dateStr = getRoundedDateStr(samplePt.getTime());

            uniqueGridRequests.put(cacheKey, new GridReq(dateStr, samplePt.getLatitude(), samplePt.getLongitude(), cacheKey));
        }
        return uniqueGridRequests;
    }


    public void fetchWeatherDataFromApi(Map<String, GridReq> uniqueGridRequests, Map<String, OpenMeteoService.OpenMeteoResponse> dailyWeatherCache) {
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

    public void mapWeatherToTrackPoints(Trip savedTrip, Map<String, List<TrackPoint>> groupedPoints, Map<String, OpenMeteoService.OpenMeteoResponse> dailyWeatherCache, List<Weather> weathersToSave) {
        Map<String, Weather> savedWeatherEntitiesCache = new HashMap<>();

        groupedPoints.forEach((gridKey, pointsInGrid) -> {
            OpenMeteoService.OpenMeteoResponse res = dailyWeatherCache.get(gridKey);

            if (res != null) {
                for (TrackPoint pt : pointsInGrid) {
                    boolean isWater = waterDetectionService.isWater(pt.getLatitude(), pt.getLongitude());

                    Instant roundedTime = pt.getTime().plus(30, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.HOURS);
                    String targetHourStr = roundedTime.toString().substring(0, 13) + ":00";

                    String hourCacheKey = targetHourStr + gridKey.substring(10) + (isWater ? "_WATER" : "_LAND");

                    Weather w = savedWeatherEntitiesCache.get(hourCacheKey);
                    if (w == null) {
                        w = openMeteoService.buildWeatherEntity(savedTrip, pt.getLatitude(), pt.getLongitude(), pt.getTime(), res);
                        if (w != null) {
                            if (!isWater) {
                                stripMarineDataFromWeather(w);
                            }
                            weathersToSave.add(w);
                            savedWeatherEntitiesCache.put(hourCacheKey, w);
                        }
                    }
                    pt.setWeather(w);
                }
            }
        });
    }

    public void stripMarineDataFromWeather(Weather w) {
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

    public void fillMarineWeatherGaps(List<TrackPoint> allTrackPoints, List<Weather> weathersToSave) {
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
                            double dist = GeoUtils.calculateDistance(pt.getLatitude(),pt.getLongitude(), neighborPt.getLatitude(), neighborPt.getLongitude());

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
}
