package jasinski.pawel.weather_visualization.service;

import jasinski.pawel.weather_visualization.dto.*;
import jasinski.pawel.weather_visualization.entity.TrackPoint;
import jasinski.pawel.weather_visualization.repository.TrackPointRepository;
import jasinski.pawel.weather_visualization.utils.AstronomyAnalyzer;
import jasinski.pawel.weather_visualization.utils.MovementAnalyzer;
import jasinski.pawel.weather_visualization.utils.WeatherAnalyzer;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class ReportService {
    private final TrackPointRepository trackPointRepository;
    private final GeoNamesService geoNamesService;

    public ReportService(TrackPointRepository trackPointRepository, GeoNamesService geoNamesService){
        this.trackPointRepository = trackPointRepository;
        this.geoNamesService = geoNamesService;
    }

    public List<DailySummary> generateDailySummaries(Long tripId){
        List<TrackPoint> allPoints = trackPointRepository.findByTripIdOrderByTimeAsc(tripId);
        ZoneId zoneId = ZoneId.of("Europe/Warsaw");

        Map<LocalDate,List<TrackPoint>> pointsByDay = new TreeMap<>();

        for(TrackPoint point : allPoints){
            LocalDate date = LocalDate.ofInstant(point.getTime(), zoneId);
            pointsByDay.computeIfAbsent(date, k -> new ArrayList<>());
            pointsByDay.get(date).add(point);
        }

        List<DailySummary> summaries = new ArrayList<>();

        for (Map.Entry<LocalDate, List<TrackPoint>> entry : pointsByDay.entrySet()) {
            LocalDate day = entry.getKey();
            List<TrackPoint> pointsInDay = entry.getValue();

            List<StopPlace> stops = MovementAnalyzer.findStops(pointsInDay);
            DayMovementStats stats = MovementAnalyzer.analyzeDay(pointsInDay, stops);
            AstronomyStats astro = AstronomyAnalyzer.calculateSun(pointsInDay, zoneId);
            WeatherStats weatherStats = WeatherAnalyzer.analyzeWeather(pointsInDay);

            summaries.add(new DailySummary(day, pointsInDay, stats, weatherStats, astro, stops));
        }

        return summaries;
    }

    private void appendCsv(StringBuilder sb, Object value) {
        sb.append(value != null ? value : "--").append(";");
    }

    public String generateCsv(Long tripId) {
        List<DailySummary> summaries = generateDailySummaries(tripId);
        StringBuilder csv = new StringBuilder();

        // Szukamy maksymalnej liczby postojów w jakimkolwiek dniu tej trasy
        int maxStops = 0;
        for (DailySummary summary : summaries) {
            if (summary.stops() != null) {
                maxStops = Math.max(maxStops, summary.stops().size());
            }
        }
        maxStops = Math.max(1, maxStops);


        csv.append("Data;Start;Koniec;Czas w ruchu;Czas na postoju;")
                .append("Średnia temperatura (°C);Średnia siła wiatru (km/h);Średni kierunek wiatru (deg);Średnie porywy wiatru (km/h);Średni punkt rosy (°C);")
                .append("Suma deszczu (mm);Suma śniegu (cm);Średnia wilgotność (%);Średnie ciśnienie (hPa);")
                .append("Średnie zachmurzenie (%);Średnie chmury niskie (%);Średnie chmury średnie (%);Średnie chmury wysokie (%);")
                .append("Średnia wysokość fal (m);Średni okres fal (s);Średni kierunek fal (deg);")
                .append("Średnia wysokość fal wiatrowych (m);Średni okres fal wiatrowych (s);Średnia wysokość fal martwych (m);Średni okres fal martwych (s);")
                .append("Średnia prędkość prądów (m/s);Średni kierunek prądów (deg);Średnia temperatura morza (°C);")
                .append("Świt astronomiczny;Świt nautyczny;Świt cywilny;Wschód słońca;Zachód słońca;")
                .append("Zmierzch cywilny;Zmierzch nautyczny;Zmierzch astronomiczny;");

        for (int i = 1; i <= maxStops; i++) {
            csv.append("Miejsce postoju ").append(i).append(";Czas postoju ").append(i);
            if (i < maxStops) {
                csv.append(";");
            }
        }
        csv.append("\n");

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.of("Europe/Warsaw"));



        for (DailySummary summary : summaries) {
            appendCsv(csv, summary.date());

            String startTime = "--";
            String endTime = "--";
            if (!summary.points().isEmpty()) {
                startTime = timeFormatter.format(summary.points().get(0).getTime());
                endTime = timeFormatter.format(summary.points().get(summary.points().size() - 1).getTime());
            }
            appendCsv(csv, startTime);
            appendCsv(csv, endTime);

            appendCsv(csv, formatSeconds(summary.movementStats().movingSeconds()));
            appendCsv(csv, formatSeconds(summary.movementStats().stoppedSeconds()));

            WeatherStats ws = summary.weatherStats();
            appendCsv(csv, ws.avgTemp());
            appendCsv(csv, ws.avgWindSpeed());
            appendCsv(csv, ws.avgWindDir());
            appendCsv(csv, ws.avgWindGusts());
            appendCsv(csv, ws.avgDewPoint());

            appendCsv(csv, ws.sumRain());
            appendCsv(csv, ws.sumSnowfall());
            appendCsv(csv, ws.avgHumidity());
            appendCsv(csv, ws.avgPressure());

            appendCsv(csv, ws.avgCloudCover());
            appendCsv(csv, ws.avgCloudCoverLow());
            appendCsv(csv, ws.avgCloudCoverMid());
            appendCsv(csv, ws.avgCloudCoverHigh());

            appendCsv(csv, ws.avgWaveHeight());
            appendCsv(csv, ws.avgWavePeriod());
            appendCsv(csv, ws.avgWaveDirection());

            appendCsv(csv, ws.avgWindWaveHeight());
            appendCsv(csv, ws.avgWindWavePeriod());
            appendCsv(csv, ws.avgSwellWaveHeight());
            appendCsv(csv, ws.avgSwellWavePeriod());

            appendCsv(csv, ws.avgOceanCurrentVelocity());
            appendCsv(csv, ws.avgOceanCurrentDirection());
            appendCsv(csv, ws.avgSeaTemperature());

            AstronomyStats astro = summary.astroStats();
            appendCsv(csv, astro.astronomicalDawn());
            appendCsv(csv, astro.nauticalDawn());
            appendCsv(csv, astro.civilDawn());
            appendCsv(csv, astro.sunrise());
            appendCsv(csv, astro.sunset());
            appendCsv(csv, astro.civilDusk());
            appendCsv(csv, astro.nauticalDusk());
            appendCsv(csv, astro.astronomicalDusk());


            for (int i = 0; i < maxStops; i++) {
                if (summary.stops() != null && i < summary.stops().size()) {
                    StopPlace sp = summary.stops().get(i);
                    String placeName = geoNamesService.getPlaceName(sp.latitude(), sp.longitude());
                    String duration = formatSeconds(sp.durationSeconds());

                    csv.append(placeName).append(";").append(duration);
                } else {
                    csv.append("--").append(";").append("--");
                }

                if (i < maxStops - 1) {
                    csv.append(";");
                }
            }
            csv.append("\n");
        }

        return csv.toString();
    }

    private String formatSeconds(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}