package jasinski.pawel.weather_visualization.service;

import jasinski.pawel.weather_visualization.entity.Trip;
import jasinski.pawel.weather_visualization.entity.Weather;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OpenMeteoService {

    private final RestTemplate restTemplate = new RestTemplate();

    public int fetchWeatherBatch(String dateStr, List<Double> lats, List<Double> lons, List<String> cacheKeys, Map<String, OpenMeteoResponse> cache, WaterDetectionService waterDetection) {
        String latStr = lats.stream().map(String::valueOf).collect(Collectors.joining(","));
        String lonStr = lons.stream().map(String::valueOf).collect(Collectors.joining(","));

        String weatherUrl = String.format(Locale.US,
                "https://archive-api.open-meteo.com/v1/archive?latitude=%s&longitude=%s&start_date=%s&end_date=%s" +
                        "&hourly=temperature_2m,dew_point_2m,relative_humidity_2m,rain,snowfall,surface_pressure," +
                        "cloud_cover,cloud_cover_low,cloud_cover_mid,cloud_cover_high," +
                        "wind_speed_10m,wind_direction_10m,wind_gusts_10m,weather_code",
                latStr, lonStr, dateStr, dateStr);

        List<Double> marineLats = new ArrayList<>();
        List<Double> marineLons = new ArrayList<>();
        boolean[] isMarineArray = new boolean[lats.size()];

        for (int i = 0; i < lats.size(); i++) {
            boolean isMarine = waterDetection.isWater(lats.get(i), lons.get(i));
            isMarineArray[i] = isMarine;
            if (isMarine) {
                marineLats.add(lats.get(i));
                marineLons.add(lons.get(i));
            }
        }

        int maxRetries = 5;
        int attempts = 0;

        while (attempts < maxRetries) {
            try {
                System.out.println(">>> API CALL (BATCH): Pobieram " + lats.size() + " pkt. (W tym morskich: " + marineLats.size() + ")");

                OpenMeteoResponse[] resArray;
                if (lats.size() == 1) {
                    resArray = new OpenMeteoResponse[]{restTemplate.getForObject(weatherUrl, OpenMeteoResponse.class)};
                } else {
                    resArray = restTemplate.getForObject(weatherUrl, OpenMeteoResponse[].class);
                }

                MarineResponse[] fullMarineResArray = new MarineResponse[lats.size()];

                if (!marineLats.isEmpty() && resArray != null) {
                    String mLatStr = marineLats.stream().map(String::valueOf).collect(Collectors.joining(","));
                    String mLonStr = marineLons.stream().map(String::valueOf).collect(Collectors.joining(","));

                    String marineUrl = String.format(Locale.US,
                            "https://marine-api.open-meteo.com/v1/marine?latitude=%s&longitude=%s&start_date=%s&end_date=%s" +
                                    "&hourly=wave_height,wave_period,wave_direction,wind_wave_height,wind_wave_period,swell_wave_height,swell_wave_period,ocean_current_velocity,ocean_current_direction,sea_surface_temperature",
                            mLatStr, mLonStr, dateStr, dateStr);

                    try {
                        MarineResponse[] fetchedMarine = marineLats.size() == 1
                                ? new MarineResponse[]{restTemplate.getForObject(marineUrl, MarineResponse.class)}
                                : restTemplate.getForObject(marineUrl, MarineResponse[].class);

                        int marineIdx = 0;
                        if (fetchedMarine != null) {
                            for (int i = 0; i < isMarineArray.length; i++) {
                                if (isMarineArray[i] && marineIdx < fetchedMarine.length) {
                                    fullMarineResArray[i] = fetchedMarine[marineIdx];
                                    marineIdx++;
                                }
                            }
                        }
                    } catch (Exception ignored) {
                        System.out.println("--- Błąd pobierania punktów morskich");
                    }
                }

                if (resArray != null) {
                    for (int i = 0; i < resArray.length; i++) {
                        mergeMarine(resArray[i], fullMarineResArray[i]);
                        cache.put(cacheKeys.get(i), resArray[i]);
                    }
                }

                return 1;

            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("429")) {
                    attempts++;

                    if (attempts >= maxRetries) {
                        System.err.println(" Przekroczono maksymalną liczbę ponownych prób dla Open-Meteo.");
                        break;
                    }

                    // Obliczanie czasu oczekiwania: 2s, 4s, 8s, 16s...
                    long baseDelay = (long) Math.pow(2, attempts) * 1000L;
                    long jitter = (long) (Math.random() * 1000);
                    long waitTime = baseDelay + jitter;

                    System.err.println(" LIMIT API (429)! Próba " + attempts + "/" + maxRetries + ". Czekam " + String.format("%.2f", waitTime / 1000.0) + " s...");

                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    System.err.println(" Błąd Open-Meteo: " + e.getMessage());
                    break;
                }
            }
        }
        return 0;
    }

    private void mergeMarine(OpenMeteoResponse res, MarineResponse marineRes) {
        if (res != null && res.hourly != null && marineRes != null && marineRes.hourly != null) {
            res.hourly.wave_height = marineRes.hourly.wave_height;
            res.hourly.wave_period = marineRes.hourly.wave_period;
            res.hourly.wave_direction = marineRes.hourly.wave_direction;
            res.hourly.wind_wave_height = marineRes.hourly.wind_wave_height;
            res.hourly.wind_wave_period = marineRes.hourly.wind_wave_period;
            res.hourly.swell_wave_height = marineRes.hourly.swell_wave_height;
            res.hourly.swell_wave_period = marineRes.hourly.swell_wave_period;
            res.hourly.ocean_current_velocity = marineRes.hourly.ocean_current_velocity;
            res.hourly.ocean_current_direction = marineRes.hourly.ocean_current_direction;
            res.hourly.sea_surface_temperature = marineRes.hourly.sea_surface_temperature;
        }
    }

    public Weather buildWeatherEntity(Trip trip, double lat, double lon, Instant time, OpenMeteoResponse response) {
        String targetHourStr = time.toString().substring(0, 13) + ":00";

        if (response != null && response.hourly != null) {
            int index = response.hourly.time.indexOf(targetHourStr);
            if (index != -1) {
                Weather weather = new Weather();
                weather.setTrip(trip);
                weather.setTime(time);
                weather.setLatitude(lat);
                weather.setLongitude(lon);
                weather.setTemp(response.hourly.temperature_2m.get(index));
                weather.setDewPoint(response.hourly.dew_point_2m.get(index));
                weather.setWindSpeed(response.hourly.wind_speed_10m.get(index));
                weather.setWindDir(response.hourly.wind_direction_10m.get(index));
                weather.setWindGusts(response.hourly.wind_gusts_10m.get(index));
                weather.setRain(response.hourly.rain.get(index));
                weather.setSnowfall(response.hourly.snowfall != null ? response.hourly.snowfall.get(index) : 0.0);
                weather.setHumidity(response.hourly.relative_humidity_2m.get(index));
                weather.setPressure(response.hourly.surface_pressure.get(index));
                weather.setCloudCover(response.hourly.cloud_cover.get(index));
                weather.setCloudCoverLow(response.hourly.cloud_cover_low.get(index));
                weather.setCloudCoverMid(response.hourly.cloud_cover_mid.get(index));
                weather.setCloudCoverHigh(response.hourly.cloud_cover_high.get(index));
                weather.setWeatherCode(response.hourly.weather_code != null ? response.hourly.weather_code.get(index) : null);

                if (response.hourly.wave_height != null && !response.hourly.wave_height.isEmpty()) {
                    weather.setWaveHeight(response.hourly.wave_height.get(index));
                    weather.setWavePeriod(response.hourly.wave_period.get(index));
                    weather.setWaveDirection(response.hourly.wave_direction.get(index));
                    weather.setWindWaveHeight(response.hourly.wind_wave_height != null ? response.hourly.wind_wave_height.get(index) : null);
                    weather.setWindWavePeriod(response.hourly.wind_wave_period != null ? response.hourly.wind_wave_period.get(index) : null);
                    weather.setSwellWaveHeight(response.hourly.swell_wave_height != null ? response.hourly.swell_wave_height.get(index) : null);
                    weather.setSwellWavePeriod(response.hourly.swell_wave_period != null ? response.hourly.swell_wave_period.get(index) : null);
                    weather.setOceanCurrentVelocity(response.hourly.ocean_current_velocity != null ? response.hourly.ocean_current_velocity.get(index) : null);
                    weather.setOceanCurrentDirection(response.hourly.ocean_current_direction != null ? response.hourly.ocean_current_direction.get(index) : null);
                    weather.setSeaTemperature(response.hourly.sea_surface_temperature != null ? response.hourly.sea_surface_temperature.get(index) : null);
                }
                return weather;
            }
        }
        return null;
    }

    public static class OpenMeteoResponse { public HourlyData hourly; }
    public static class HourlyData {
        public List<String> time; public List<Double> temperature_2m; public List<Double> dew_point_2m;
        public List<Integer> relative_humidity_2m; public List<Double> rain; public List<Double> snowfall;
        public List<Double> surface_pressure; public List<Integer> cloud_cover; public List<Integer> cloud_cover_low;
        public List<Integer> cloud_cover_mid; public List<Integer> cloud_cover_high; public List<Double> wind_speed_10m;
        public List<Integer> wind_direction_10m; public List<Double> wind_gusts_10m; public List<Integer> weather_code;
        public List<Double> wave_height; public List<Double> wave_period; public List<Integer> wave_direction;
        public List<Double> wind_wave_height; public List<Double> wind_wave_period; public List<Double> swell_wave_height;
        public List<Double> swell_wave_period; public List<Double> ocean_current_velocity; public List<Integer> ocean_current_direction;
        public List<Double> sea_surface_temperature;
    }

    public static class MarineResponse { public MarineHourlyData hourly; }
    public static class MarineHourlyData {
        public List<Double> wave_height; public List<Double> wave_period; public List<Integer> wave_direction;
        public List<Double> wind_wave_height; public List<Double> wind_wave_period; public List<Double> swell_wave_height;
        public List<Double> swell_wave_period; public List<Double> ocean_current_velocity; public List<Integer> ocean_current_direction;
        public List<Double> sea_surface_temperature;
    }
}