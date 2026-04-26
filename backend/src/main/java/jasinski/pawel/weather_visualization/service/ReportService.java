package jasinski.pawel.weather_visualization.service;

import jasinski.pawel.weather_visualization.dto.*;
import jasinski.pawel.weather_visualization.entity.TrackPoint;
import jasinski.pawel.weather_visualization.entity.Trip;
import jasinski.pawel.weather_visualization.repository.TrackPointRepository;
import jasinski.pawel.weather_visualization.utils.AstronomyAnalyzer;
import jasinski.pawel.weather_visualization.utils.MovementAnalyzer;
import jasinski.pawel.weather_visualization.utils.TimelineChartGenerator;
import jasinski.pawel.weather_visualization.utils.WeatherAnalyzer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ReportService {
    private final TrackPointRepository trackPointRepository;
    private final GeoNamesService geoNamesService;
    private final TripService tripService;

    public ReportService(TrackPointRepository trackPointRepository, GeoNamesService geoNamesService, TripService tripService){
        this.trackPointRepository = trackPointRepository;
        this.geoNamesService = geoNamesService;
        this.tripService = tripService;
    }

    public ReportResource getCsvReportResource(Long tripId, String email) {

        Trip trip = tripService.getUserTrips(email).stream()
                .filter(t -> t.getId().equals(tripId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Brak uprawnień"));

        String csvContent = generateCsv(tripId);
        byte[] csvBytes = csvContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        byte[] bom = new byte[] { (byte)0xEF, (byte)0xBB, (byte)0xBF };
        byte[] finalBytes = new byte[bom.length + csvBytes.length];
        System.arraycopy(bom, 0, finalBytes, 0, bom.length);
        System.arraycopy(csvBytes, 0, finalBytes, bom.length, csvBytes.length);

        String fileName = trip.getName().replaceAll("(?i)\\.gpx$", "") + ".csv";

        return new ReportResource(finalBytes, fileName);
    }

    public List<DailySummary> generateDailySummaries(Long tripId) {
        List<TrackPoint> allPoints = trackPointRepository.findByTripIdOrderByTimeAsc(tripId);
        if (allPoints.isEmpty()) return new ArrayList<>();

        ZoneId zoneId = ZoneId.of("Europe/Warsaw");

        Map<LocalDate,DayData> dailyMovements = MovementAnalyzer.analyzeTripTimeline(allPoints, zoneId);

        Map<LocalDate, List<TrackPoint>> pointsByDay = new TreeMap<>();
        for (TrackPoint point : allPoints) {
            LocalDate date = LocalDate.ofInstant(point.getTime(), zoneId);
            pointsByDay.computeIfAbsent(date, k -> new ArrayList<>()).add(point);
        }

        Set<LocalDate> allDates = new TreeSet<>();
        allDates.addAll(dailyMovements.keySet());
        allDates.addAll(pointsByDay.keySet());

        List<DailySummary> summaries = new ArrayList<>();

        for (LocalDate day : allDates) {
            List<TrackPoint> pointsInDay = pointsByDay.getOrDefault(day, new ArrayList<>());
            DayData movData = dailyMovements.get(day);

            DayMovementStats stats = movData != null ?
                    new DayMovementStats(movData.movingSeconds, movData.stoppedSeconds, movData.gapSeconds) :
                    new DayMovementStats(0, 0, 0);

            List<TimelineEvent> events = movData != null ? movData.events : new ArrayList<>();
            AstronomyStats astro = AstronomyAnalyzer.calculateSun(pointsInDay, events, zoneId);

            // Filtrowanie punktów tylko podczs ruchu
            List<TrackPoint> movingPointsOnly = new ArrayList<>();
            if (movData != null && movData.events != null) {
                for (TrackPoint p : pointsInDay) {
                    boolean isMoving = false;
                    for (TimelineEvent ev : movData.events) {
                        if ("RUCH".equals(ev.type()) && !p.getTime().isBefore(ev.start()) && !p.getTime().isAfter(ev.end())) {
                            isMoving = true;
                            break;
                        }
                    }
                    if (isMoving) {
                        movingPointsOnly.add(p);
                    }
                }
            }

            WeatherStats overallWeatherStats = WeatherAnalyzer.analyzeWeather(pointsInDay);     // Z całego dnia
            WeatherStats movingWeatherStats = WeatherAnalyzer.analyzeWeather(movingPointsOnly); // Tylko z momentów ruchu

            summaries.add(new DailySummary(day, pointsInDay, stats, overallWeatherStats, movingWeatherStats, astro, events));
        }

        return summaries;
    }

    private void appendCsv(StringBuilder sb, Object value) {
        sb.append(value != null ? value : "--").append(";");
    }

    public String generateCsv(Long tripId) {
        List<DailySummary> summaries = generateDailySummaries(tripId);
        StringBuilder csv = new StringBuilder();

        int maxEvents = 0;
        for (DailySummary summary : summaries) {
            if (summary.timelineEvents() != null) {
                maxEvents = Math.max(maxEvents, summary.timelineEvents().size());
            }
        }
        maxEvents = Math.max(1, maxEvents);

        csv.append("Data;Start;Koniec;Czas w ruchu;Czas na postoju;Czas braku danych;")
                .append("Średnia temperatura (°C);Średnia temperatura w ruchu (°C);Średnia siła wiatru (km/h);Średnia siła wiatru w ruchu (km/h);Średni kierunek wiatru (deg);Średni kierunek wiatru w ruchu (deg);Średnie porywy wiatru (km/h);Średnie porywy wiatru w ruchu (km/h);Średni punkt rosy (°C);Średni punkt rosy w ruchu (°C);")
                .append("Suma opadów deszczu (mm);Suma opadów deszczu w ruchu (mm);Suma opadów śniegu (cm);Suma opadów śniegu w ruchu (cm);Średnia wilgotność (%);Średnia wilgotność w ruchu (%);Średnie ciśnienie (hPa);Średnie ciśnienie w ruchu (hPa);")
                .append("Średnie zachmurzenie (%);Średnie zachmurzenie w ruchu (%);Średnie chmury niskie (%);Średnie chmury niskie w ruchu (%);Średnie chmury średnie (%);Średnie chmury średnie w ruchu (%);Średnie chmury wysokie (%);Średnie chmury wysokie w ruchu (%);")
                .append("Średnia wysokość fal (m);Średnia wysokość fal w ruchu (m);Średni okres fal (s);Średni okres fal w ruchu (s);Średni kierunek fal (deg);Średni kierunek fal w ruchu (deg);")
                .append("Średnia wysokość fal wiatrowych (m);Średnia wysokość fal wiatrowych w ruchu (m);Średni okres fal wiatrowych (s);Średni okres fal wiatrowych w ruchu (s);Średnia wysokość martwej fali (m);Średnia wysokość martwej fali w ruchu (m);Średni okres martwej fali (s);Średni okres martwej fali w ruchu (s);")
                .append("Średnia prędkość prądów (m/s);Średnia prędkość prądów w ruchu (m/s);Średni kierunek prądów (deg);Średni kierunek prądów w ruchu (deg);Średnia temperatura morza (°C);Średnia temperatura morza w ruchu (°C);")
                .append("Świt astronomiczny;Świt nautyczny;Świt cywilny;Wschód słońca;Kulminacja słońca;Zachód słońca;")
                .append("Zmierzch cywilny;Zmierzch nautyczny;Zmierzch astronomiczny;");

        for (int i = 1; i <= maxEvents; i++) {
            csv.append("Faza ").append(i).append(" Typ;");
            csv.append("Faza ").append(i).append(" Start;");
            csv.append("Faza ").append(i).append(" Koniec;");
            csv.append("Faza ").append(i).append(" Czas trwania;");
            csv.append("Faza ").append(i).append(" Miejsce;");
        }
        csv.append("\n");

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.of("Europe/Warsaw"));

        for (DailySummary summary : summaries) {
            appendCsv(csv, summary.date());

            String dayStart = "--";
            String dayEnd = "--";

            if (summary.timelineEvents() != null && !summary.timelineEvents().isEmpty()) {
                List<TimelineEvent> dailyEvents = summary.timelineEvents();
                boolean movedToday = false;

                for (TimelineEvent ev : dailyEvents) {
                    if ("RUCH".equals(ev.type())) {
                        dayStart = timeFormatter.format(ev.start());
                        movedToday = true;
                        break;
                    }
                }

                if (movedToday) {
                    TimelineEvent lastEvent = dailyEvents.get(dailyEvents.size() - 1);
                    if ("POSTÓJ".equals(lastEvent.type())) {
                        dayEnd = timeFormatter.format(lastEvent.start());
                    } else {
                        dayEnd = timeFormatter.format(lastEvent.end());
                    }
                }
            }
            appendCsv(csv, dayStart);
            appendCsv(csv, dayEnd);

            appendCsv(csv, formatSeconds(summary.movementStats().movingSeconds()));
            appendCsv(csv, formatSeconds(summary.movementStats().stoppedSeconds()));
            appendCsv(csv, formatSeconds(summary.movementStats().gapSeconds()));

            WeatherStats oWs = summary.overallWeatherStats();
            WeatherStats mWs = summary.movingWeatherStats();

            appendCsv(csv, oWs.avgTemp()); appendCsv(csv, mWs.avgTemp());
            appendCsv(csv, oWs.avgWindSpeed()); appendCsv(csv, mWs.avgWindSpeed());
            appendCsv(csv, oWs.avgWindDir()); appendCsv(csv, mWs.avgWindDir());
            appendCsv(csv, oWs.avgWindGusts()); appendCsv(csv, mWs.avgWindGusts());
            appendCsv(csv, oWs.avgDewPoint()); appendCsv(csv, mWs.avgDewPoint());

            appendCsv(csv, oWs.sumRain()); appendCsv(csv, mWs.sumRain());
            appendCsv(csv, oWs.sumSnowfall()); appendCsv(csv, mWs.sumSnowfall());
            appendCsv(csv, oWs.avgHumidity()); appendCsv(csv, mWs.avgHumidity());
            appendCsv(csv, oWs.avgPressure()); appendCsv(csv, mWs.avgPressure());

            appendCsv(csv, oWs.avgCloudCover()); appendCsv(csv, mWs.avgCloudCover());
            appendCsv(csv, oWs.avgCloudCoverLow()); appendCsv(csv, mWs.avgCloudCoverLow());
            appendCsv(csv, oWs.avgCloudCoverMid()); appendCsv(csv, mWs.avgCloudCoverMid());
            appendCsv(csv, oWs.avgCloudCoverHigh()); appendCsv(csv, mWs.avgCloudCoverHigh());

            appendCsv(csv, oWs.avgWaveHeight()); appendCsv(csv, mWs.avgWaveHeight());
            appendCsv(csv, oWs.avgWavePeriod()); appendCsv(csv, mWs.avgWavePeriod());
            appendCsv(csv, oWs.avgWaveDirection()); appendCsv(csv, mWs.avgWaveDirection());

            appendCsv(csv, oWs.avgWindWaveHeight()); appendCsv(csv, mWs.avgWindWaveHeight());
            appendCsv(csv, oWs.avgWindWavePeriod()); appendCsv(csv, mWs.avgWindWavePeriod());
            appendCsv(csv, oWs.avgSwellWaveHeight()); appendCsv(csv, mWs.avgSwellWaveHeight());
            appendCsv(csv, oWs.avgSwellWavePeriod()); appendCsv(csv, mWs.avgSwellWavePeriod());

            appendCsv(csv, oWs.avgOceanCurrentVelocity()); appendCsv(csv, mWs.avgOceanCurrentVelocity());
            appendCsv(csv, oWs.avgOceanCurrentDirection()); appendCsv(csv, mWs.avgOceanCurrentDirection());
            appendCsv(csv, oWs.avgSeaTemperature()); appendCsv(csv, mWs.avgSeaTemperature());


            AstronomyStats astro = summary.astroStats();
            appendAstroTime(csv, astro.astronomicalDawn(), astro.astronomicalDawnPt());
            appendAstroTime(csv, astro.nauticalDawn(), astro.nauticalDawnPt());
            appendAstroTime(csv, astro.civilDawn(), astro.civilDawnPt());
            appendAstroTime(csv, astro.sunrise(), astro.sunrisePt());
            appendAstroTime(csv, astro.solarNoon(), astro.noonPt());
            appendAstroTime(csv, astro.sunset(), astro.sunsetPt());
            appendAstroTime(csv, astro.civilDusk(), astro.civilDuskPt());
            appendAstroTime(csv, astro.nauticalDusk(), astro.nauticalDuskPt());
            appendAstroTime(csv, astro.astronomicalDusk(), astro.astronomicalDuskPt());


            for (int i = 0; i < maxEvents; i++) {
                if (summary.timelineEvents() != null && i < summary.timelineEvents().size()) {
                    TimelineEvent ev = summary.timelineEvents().get(i);

                    appendCsv(csv, ev.type());
                    appendCsv(csv, timeFormatter.format(ev.start()));
                    appendCsv(csv, timeFormatter.format(ev.end()));
                    appendCsv(csv, formatSeconds(ev.durationSeconds()));

                    if ("POSTÓJ".equals(ev.type()) && ev.lat() != 0.0 && ev.lon() != 0.0) {
                        String placeName = geoNamesService.getPlaceName(ev.lat(), ev.lon());
                        appendCsv(csv, placeName);
                    } else {
                        appendCsv(csv, "--");
                    }
                } else {
                    csv.append("--;--;--;--;--;");
                }
            }
            csv.append("\n");
        }

        return csv.toString();
    }

    public byte[] generateAllTimelinesZip(Long tripId) {
        List<DailySummary> summaries = generateDailySummaries(tripId);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            int dayNumber = 1;

            for (DailySummary summary : summaries) {
                byte[] imageBytes = TimelineChartGenerator.generateDailyTimelineChart(
                        summary.date(), summary.timelineEvents(), ZoneId.of("Europe/Warsaw"), geoNamesService
                );

                String fileName = "(" + dayNumber + ") " + dateFormatter.format(summary.date()) + ".png";

                ZipEntry zipEntry = new ZipEntry(fileName);
                zos.putNextEntry(zipEntry);
                zos.write(imageBytes);
                zos.closeEntry();
                dayNumber++;
            }
        } catch (IOException e) {
            throw new RuntimeException("Błąd podczas tworzenia paczki ZIP", e);
        }

        return baos.toByteArray();
    }


    private String formatSeconds(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void appendAstroTime(StringBuilder csv, LocalTime time, TrackPoint point) {
        if (time == null || point == null) {
            appendCsv(csv, "--:--");
        } else {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");
            appendCsv(csv, time.format(fmt));
        }
    }
}