package jasinski.pawel.weather_visualization.service;

import jasinski.pawel.weather_visualization.entity.Trip;
import jasinski.pawel.weather_visualization.entity.Weather;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class OpenMeteoService {

    private final RestTemplate restTemplate = new RestTemplate();

    public Weather fetchWeather(Trip trip, double lat, double lon, Instant time, Map<String, OpenMeteoResponse> cache) {
        String dateStr = time.toString().substring(0, 10);
        String targetHourStr = time.toString().substring(0, 13) + ":00";

        double gridLat = Math.round(lat * 10.0) / 10.0;
        double gridLon = Math.round(lon * 10.0) / 10.0;
        String cacheKey = dateStr + "_" + gridLat + "_" + gridLon;

        OpenMeteoResponse response = cache.get(cacheKey);

        if (response == null) {
            String weatherUrl = String.format(Locale.US,
                    "https://archive-api.open-meteo.com/v1/archive?latitude=%f&longitude=%f&start_date=%s&end_date=%s" +
                            "&hourly=temperature_2m,dew_point_2m,relative_humidity_2m,rain,snowfall,surface_pressure," +
                            "cloud_cover,cloud_cover_low,cloud_cover_mid,cloud_cover_high," +
                            "wind_speed_10m,wind_direction_10m,wind_gusts_10m",
                    lat, lon, dateStr, dateStr);


            String marineUrl = String.format(Locale.US,
                    "https://marine-api.open-meteo.com/v1/marine?latitude=%f&longitude=%f&start_date=%s&end_date=%s" +
                            "&hourly=wave_height,wave_period,wave_direction",
                    lat, lon, dateStr, dateStr);

            int maxRetries = 3;
            int attempts = 0;
            boolean fetched = false;

            while (attempts < maxRetries && !fetched) {
                try {
                    System.out.println(">>> API CALL: Pobieram POGODĘ dla: " + lat + ", " + lon);
                    response = restTemplate.getForObject(weatherUrl, OpenMeteoResponse.class);

                    try {
                        MarineResponse marineRes = restTemplate.getForObject(marineUrl, MarineResponse.class);
                        if (marineRes != null && marineRes.hourly != null && response != null && response.hourly != null) {
                            response.hourly.wave_height = marineRes.hourly.wave_height;
                            response.hourly.wave_period = marineRes.hourly.wave_period;
                            response.hourly.wave_direction = marineRes.hourly.wave_direction;
                        }
                    } catch (Exception e) {
                        System.out.println("--- Punkt na lądzie, omijam dane morskie.");
                    }

                    cache.put(cacheKey, response);
                    fetched = true;

                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("429")) {
                        attempts++;
                        System.err.println(" LIMIT API (429)! Oczekuję 60 sekund...");
                        try { Thread.sleep(60000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    } else {
                        System.err.println(" Błąd Open-Meteo: " + e.getMessage());
                        break;
                    }
                }
            }

            if (!fetched) return null;
        }

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

                if (response.hourly.wave_height != null && !response.hourly.wave_height.isEmpty()) {
                    weather.setWaveHeight(response.hourly.wave_height.get(index));
                    weather.setWavePeriod(response.hourly.wave_period.get(index));
                    weather.setWaveDirection(response.hourly.wave_direction.get(index));
                }

                return weather;
            }
        }
        return null;
    }

    public static class OpenMeteoResponse { public HourlyData hourly; }
    public static class HourlyData {
        public List<String> time;
        public List<Double> temperature_2m;
        public List<Double> dew_point_2m;
        public List<Integer> relative_humidity_2m;
        public List<Double> rain;
        public List<Double> snowfall;
        public List<Double> surface_pressure;
        public List<Integer> cloud_cover;
        public List<Integer> cloud_cover_low;
        public List<Integer> cloud_cover_mid;
        public List<Integer> cloud_cover_high;
        public List<Double> wind_speed_10m;
        public List<Integer> wind_direction_10m;
        public List<Double> wind_gusts_10m;
        public List<Double> wave_height;
        public List<Double> wave_period;
        public List<Integer> wave_direction;
    }

    public static class MarineResponse { public MarineHourlyData hourly; }
    public static class MarineHourlyData {
        public List<Double> wave_height;
        public List<Double> wave_period;
        public List<Integer> wave_direction;
    }
}