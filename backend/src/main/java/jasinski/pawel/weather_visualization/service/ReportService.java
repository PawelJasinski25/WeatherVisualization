package jasinski.pawel.weather_visualization.service;
import jasinski.pawel.weather_visualization.dto.*;
import jasinski.pawel.weather_visualization.entity.TrackPoint;
import jasinski.pawel.weather_visualization.entity.Weather;
import jasinski.pawel.weather_visualization.repository.TrackPointRepository;
import jasinski.pawel.weather_visualization.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
public class ReportService {
    private final TrackPointRepository trackPointRepository;
    private final GeoNamesService geoNamesService;
    private final TripService tripService;
    private final RestTemplate restTemplate;
    private final WaterDetectionService waterDetectionService;
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Warsaw");

    @Autowired
    public ReportService(TrackPointRepository trackPointRepository, GeoNamesService geoNamesService, TripService tripService, RestTemplate restTemplate, WaterDetectionService waterDetectionService){
        this.trackPointRepository = trackPointRepository;
        this.geoNamesService = geoNamesService;
        this.tripService = tripService;
        this.restTemplate = restTemplate;
        this.waterDetectionService = waterDetectionService;
    }

    private TripAnalysisContext analyzeTrip(Long tripId) {
        List<TrackPoint> allPoints = trackPointRepository.findByTripIdOrderByTimeAsc(tripId);
        if (allPoints.isEmpty()) {
            return new TripAnalysisContext(new ArrayList<>(), new TreeMap<>(), new ArrayList<>());
        }

        Map<LocalDate, DayData> dailyMovements = MovementAnalyzer.analyzeTripTimeline(allPoints, DEFAULT_ZONE);
        List<EnrichedSegment> allSegments = createEnrichedSegments(allPoints, dailyMovements, DEFAULT_ZONE);

        return new TripAnalysisContext(allPoints, dailyMovements, allSegments);
    }


    public List<DailySummary> generateDailySummaries(TripAnalysisContext context) {
        if (context.points().isEmpty()) return new ArrayList<>();

        Map<LocalDate, List<TrackPoint>> pointsByDay = new TreeMap<>();
        for (TrackPoint point : context.points()) {
            LocalDate date = LocalDate.ofInstant(point.getTime(), DEFAULT_ZONE);
            pointsByDay.computeIfAbsent(date, k -> new ArrayList<>()).add(point);
        }

        Set<LocalDate> allDates = new TreeSet<>();
        allDates.addAll(context.dailyMovements().keySet());
        allDates.addAll(pointsByDay.keySet());

        List<DailySummary> summaries = new ArrayList<>();

        for (LocalDate day : allDates) {
            List<TrackPoint> pointsInDay = pointsByDay.getOrDefault(day, new ArrayList<>());
            DayData movData = context.dailyMovements().get(day);

            DayMovementStats stats = movData != null ?
                    new DayMovementStats(movData.movingSeconds, movData.stoppedSeconds, movData.gapSeconds) :
                    new DayMovementStats(0, 0, 0);

            List<TimelineEvent> events = new ArrayList<>();
            if (movData != null && movData.events != null) {
                for (TimelineEvent ev : movData.events) {
                    String placeName = null;
                    if ("POSTÓJ".equals(ev.type()) && ev.lat() != 0.0 && ev.lon() != 0.0) {
                        placeName = geoNamesService.getPlaceName(ev.lat(), ev.lon());
                    }
                    events.add(new TimelineEvent(ev.type(), ev.start(), ev.end(), ev.lat(), ev.lon(), placeName));
                }
            }

            //dodaje do analizy dodatkowe 6h z kolejnego dnia zeby zjawiska po północy zostały dobrze zinterpretowane
            List<TrackPoint> pointsForAstro = new ArrayList<>(pointsInDay);
            List<TimelineEvent> eventsForAstro = new ArrayList<>(events);

            LocalDate nextDay = day.plusDays(1);
            if (pointsByDay.containsKey(nextDay)) {
                for (TrackPoint p : pointsByDay.get(nextDay)) {
                    if (p.getTime().atZone(DEFAULT_ZONE).getHour() <= 5) {
                        pointsForAstro.add(p);
                    } else {
                        break;
                    }
                }
            }

            DayData nextMovData = context.dailyMovements().get(nextDay);
            if (nextMovData != null && nextMovData.events != null) {
                for (TimelineEvent ev : nextMovData.events) {
                    if (ev.start().atZone(DEFAULT_ZONE).getHour() <= 5) {
                        eventsForAstro.add(ev);
                    }
                }
            }
            AstronomyStats astro = AstronomyAnalyzer.calculateSun(pointsForAstro, context.points(), eventsForAstro, DEFAULT_ZONE);

            List<EnrichedSegment> dailySegments = context.segments().stream()
                    .filter(s -> LocalDate.ofInstant(s.p1().getTime(), DEFAULT_ZONE).equals(day))
                    .toList();

            List<EnrichedSegment> dailyMovingSegments = dailySegments.stream()
                    .filter(EnrichedSegment::isMoving)
                    .toList();

            SpeedStats speedStats = SpeedAnalyzer.calculateSpeed(dailySegments);
            WeatherStats overallWeatherStats = WeatherAnalyzer.analyzeWeather(dailySegments);
            WeatherStats movingWeatherStats = WeatherAnalyzer.analyzeWeather(dailyMovingSegments);

            summaries.add(new DailySummary(day, dailySegments, stats, overallWeatherStats, movingWeatherStats, speedStats, astro, events));
        }

        return summaries;
    }

    @Cacheable(value = "reportData", key = "#tripId")
    public TripReportDataDto getTripReportData(Long tripId, String email) {

        TripResponseDto trip = tripService.getUserTrips(email).stream()
                .filter(t -> t.id().equals(tripId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Brak uprawnień"));

        TripAnalysisContext context = analyzeTrip(tripId);
        List<DailySummary> dailySummaries = generateDailySummaries(context);

        String startPort = "";
        String endPort = "";
        List<TrackPoint> points = context.points();

        if (!points.isEmpty()) {
            TrackPoint firstPoint = points.get(0);
            TrackPoint lastPoint = points.get(points.size() - 1);

            String startPlaceName = geoNamesService.getPlaceName(firstPoint.getLatitude(), firstPoint.getLongitude());
            String endPlaceName = geoNamesService.getPlaceName(lastPoint.getLatitude(), lastPoint.getLongitude());

            if (startPlaceName != null) startPort = startPlaceName;
            if (endPlaceName != null) endPort = endPlaceName;
        }

        long totalMoving = 0, totalStopped = 0, totalGap = 0;
        for (DailySummary ds : dailySummaries) {
            totalMoving += ds.movementStats().movingSeconds();
            totalStopped += ds.movementStats().stoppedSeconds();
            totalGap += ds.movementStats().gapSeconds();
        }
        DayMovementStats overallMovement = new DayMovementStats(totalMoving, totalStopped, totalGap);

        List<EnrichedSegment> allMovingSegments = context.segments().stream()
                .filter(EnrichedSegment::isMoving)
                .toList();

        SpeedStats overallSpeed = SpeedAnalyzer.calculateSpeed(context.segments());
        WeatherStats overallWeather = WeatherAnalyzer.analyzeWeather(context.segments());
        WeatherStats overallMovingWeather = WeatherAnalyzer.analyzeWeather(allMovingSegments);

        List<ReportDailySummaryDto> reportDailySummaries = dailySummaries.stream()
                .map(summary -> {
                    List<EnrichedSegment> reducedSegments = reduceSegmentsForChart(summary.segments(), 300);

                    return ReportDailySummaryDto.from(summary, reducedSegments);
                })
                .toList();

        return new TripReportDataDto(trip.name(), overallMovement, overallSpeed, overallWeather, overallMovingWeather, reportDailySummaries, startPort, endPort);
    }

    private List<EnrichedSegment> reduceSegmentsForChart(List<EnrichedSegment> segments, int targetCount) {
        List<EnrichedSegment> reduced = new ArrayList<>();
        if (segments == null || segments.isEmpty()) return reduced;

        int step = Math.max(1, segments.size() / targetCount);

        reduced.add(segments.get(0));

        for (int i = 1; i < segments.size(); i++) {
            EnrichedSegment curr = segments.get(i);
            boolean keep = false;

            if (i == segments.size() - 1) {
                keep = true;
            } else if (curr.isMoving() != segments.get(i + 1).isMoving()) {
                keep = true;
            }

            if (curr.isMoving() != segments.get(i - 1).isMoving()) {
                keep = true;
            }

            if (i % step == 0) {
                keep = true;
            }

            if (keep) {
                reduced.add(curr);
            }
        }
        return reduced;
    }

    private List<EnrichedSegment> createEnrichedSegments(
            List<TrackPoint> points,
            Map<LocalDate, DayData> dailyMovements,
            ZoneId zoneId) {

        List<EnrichedSegment> segments = new ArrayList<>();

        for (int i = 0; i < points.size() - 1; i++) {
            TrackPoint p1 = points.get(i);
            TrackPoint p2 = points.get(i + 1);

            if (p1.getSegmentId() != null && p1.getSegmentId().equals(p2.getSegmentId())) {
                double dist = GeoUtils.calculateDistance(p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude());
                double dur = Math.abs(Duration.between(p1.getTime(), p2.getTime()).toMillis()) / 1000.0;

                if (dur > 0) {
                    boolean isMoving = false;
                    LocalDate date = LocalDate.ofInstant(p1.getTime(), zoneId);
                    DayData movData = dailyMovements.get(date);

                    if (movData != null && movData.events != null) {
                        for (TimelineEvent ev : movData.events) {
                            if ("RUCH".equals(ev.type()) && !p1.getTime().isBefore(ev.start()) && !p1.getTime().isAfter(ev.end())) {
                                isMoving = true;
                                break;
                            }
                        }
                    }

                    double speed = 0.0;

                    if (isMoving) {
                        if (p2.getSpeed() != null && p2.getSpeed() > 0.0) {
                            speed = p2.getSpeed();
                        } else {
                            speed = (dist / dur) * 3.6;
                        }
                    }

                    segments.add(new EnrichedSegment(p1, p2, dist, dur, speed, isMoving));
                }
            }
        }
        return segments;
    }

    public ReportResource getCsvReportResource(Long tripId, String email) {
        TripResponseDto trip = tripService.getUserTrips(email).stream()
                .filter(t -> t.id().equals(tripId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Brak uprawnień"));

        TripAnalysisContext context = analyzeTrip(tripId);

        String mainCsvContent = generateSummaryCsv(context);
        String apiCsvContent = generateApiUsageCsv(context);

        TripReportDataDto data = getTripReportData(tripId, email);
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> payload = mapper.convertValue(data, new TypeReference<Map<String, Object>>() {});

        byte[] chartsZipBytes = null;
        try {
            String pythonZipUrl = pythonUrl.replace("/generate-pdf", "/generate-charts-zip");
            chartsZipBytes = restTemplate.postForObject(pythonZipUrl, payload, byte[].class);
        } catch (Exception e) {
            System.err.println("Nie udało się pobrać wykresów: " + e.getMessage());
        }

        byte[] bom = new byte[] { (byte)0xEF, (byte)0xBB, (byte)0xBF };
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {


            // Główne podsumowanie trasy
            zos.putNextEntry(new ZipEntry("podsumowanie_trasy.csv"));
            zos.write(bom);
            zos.write(mainCsvContent.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // Punkty z bazy i zapytania
            zos.putNextEntry(new ZipEntry("punkty.csv"));
            zos.write(bom);
            zos.write(apiCsvContent.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            //Wykresy
            if (chartsZipBytes != null) {
                try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(chartsZipBytes))) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        zos.putNextEntry(new ZipEntry("Wykresy/" + entry.getName()));
                        zis.transferTo(zos);
                        zos.closeEntry();
                    }
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Błąd podczas generowania pliku ZIP", e);
        }

        String fileName = trip.name().replaceAll("(?i)\\.gpx$", "") + "_raport.zip";

        return new ReportResource(baos.toByteArray(), fileName);
    }

    public String generateSummaryCsv(TripAnalysisContext context) {
        List<DailySummary> summaries = generateDailySummaries(context);
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

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(DEFAULT_ZONE);

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

                    if (ev.placeName() != null && !ev.placeName().isEmpty()) {
                        appendCsv(csv, ev.placeName());
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

    public String generateApiUsageCsv(TripAnalysisContext context) {
        StringBuilder csv = new StringBuilder();
        DateTimeFormatter fullTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(DEFAULT_ZONE);

        class ApiUsageStats {
            java.time.Instant firstPointTime;
            java.time.Instant lastPointTime;
            double gridLat;
            double gridLon;
            double origLat;
            double origLon;
            boolean isWater;
            Weather weather;
            int pointCount = 0;
            boolean isActualHttpApiCall;
        }

        Map<String, ApiUsageStats> usageMap = new LinkedHashMap<>();
        Set<String> executedHttpCalls = new HashSet<>();

        // Odtwarzanie zapytań
        for (TrackPoint pt : context.points()) {
            String dateStr = pt.getTime().toString().substring(0, 10);
            String targetHourStr = pt.getTime().toString().substring(0, 13) + ":00";
            double gridLat = Math.round(pt.getLatitude() * 10.0) / 10.0;
            double gridLon = Math.round(pt.getLongitude() * 10.0) / 10.0;

            String hourlyKey = targetHourStr + "_" + gridLat + "_" + gridLon;
            String dailyGridKey = dateStr + "_" + gridLat + "_" + gridLon;

            ApiUsageStats stats = usageMap.get(hourlyKey);
            if (stats == null) {
                stats = new ApiUsageStats();
                stats.firstPointTime = pt.getTime();
                stats.gridLat = gridLat;
                stats.gridLon = gridLon;
                stats.origLat = pt.getLatitude();
                stats.origLon = pt.getLongitude();
                stats.isWater = waterDetectionService.isWater(pt.getLatitude(), pt.getLongitude());
                stats.weather = pt.getWeather();

                if (!executedHttpCalls.contains(dailyGridKey)) {
                    stats.isActualHttpApiCall = true;
                    executedHttpCalls.add(dailyGridKey);
                } else {
                    stats.isActualHttpApiCall = false;
                }

                usageMap.put(hourlyKey, stats);
            }

            stats.lastPointTime = pt.getTime();
            stats.pointCount++;
        }

        csv.append("Data punktu z bazy;Szerokość;Długość;Szerokość zaokrąglona;Długość zaokrąglona;Źródło danych;Open-Meteo Historical API (0/1);Open-Meteo Marine API (0/1);Dopasowane punkty;Dopasowane punkty od;Dopasowane punkty do;");
        csv.append("Temperatura (°C);Prędkość wiatru (km/h);Kierunek wiatru (°);Punkt rosy (°C);Porywy wiatru (km/h);Opady deszczu (mm);Opady śniegu (cm);Wilgotność (%);Ciśnienie (hPa);Zachmurzenie ogólne (%);Chmury niskie (%);Chmury średnie (%);Chmury wysokie (%);Wysokość fali (m);Okres fali (s);Kierunek fali (deg);Wysokość fal wiatrowych (m);Okres fal wiatrowych (s);Wysokość martwej fali (m);Okres martwej fali (s);Prędkość prądów (km/h);Kierunek prądów (°);Temperatura morza (°C);Kod pogody\n");

        for (ApiUsageStats stats : usageMap.values()) {
            String origLatStr = String.format(Locale.US, "%.5f", stats.origLat);
            String origLonStr = String.format(Locale.US, "%.5f", stats.origLon);
            String gridLatStr = String.format(Locale.US, "%.1f", stats.gridLat);
            String gridLonStr = String.format(Locale.US, "%.1f", stats.gridLon);

            String dateFullStr = fullTimeFormatter.format(stats.firstPointTime);
            String startTimeStr = fullTimeFormatter.format(stats.firstPointTime);
            String endTimeStr = fullTimeFormatter.format(stats.lastPointTime);

            String dataSource = stats.isActualHttpApiCall ?
                    "Zapytanie HTTP (paczka 24h)" :
                    "Cache (paczka 24h)";

            int historicalFlag = stats.isActualHttpApiCall ? 1 : 0;
            int marineFlag = (stats.isActualHttpApiCall && stats.isWater) ? 1 : 0;

            csv.append(dateFullStr).append(";")
                    .append(origLatStr).append(";")
                    .append(origLonStr).append(";")
                    .append(gridLatStr).append(";")
                    .append(gridLonStr).append(";")
                    .append(dataSource).append(";")
                    .append(historicalFlag).append(";")
                    .append(marineFlag).append(";")
                    .append(stats.pointCount).append(";")
                    .append(startTimeStr).append(";")
                    .append(endTimeStr).append(";");

            if (stats.weather != null) {
                appendCsv(csv, stats.weather.getTemp());
                appendCsv(csv, stats.weather.getWindSpeed());
                appendCsv(csv, stats.weather.getWindDir());
                appendCsv(csv, stats.weather.getDewPoint());
                appendCsv(csv, stats.weather.getWindGusts());
                appendCsv(csv, stats.weather.getRain());
                appendCsv(csv, stats.weather.getSnowfall());
                appendCsv(csv, stats.weather.getHumidity());
                appendCsv(csv, stats.weather.getPressure());
                appendCsv(csv, stats.weather.getCloudCover());
                appendCsv(csv, stats.weather.getCloudCoverLow());
                appendCsv(csv, stats.weather.getCloudCoverMid());
                appendCsv(csv, stats.weather.getCloudCoverHigh());
                appendCsv(csv, stats.weather.getWaveHeight());
                appendCsv(csv, stats.weather.getWavePeriod());
                appendCsv(csv, stats.weather.getWaveDirection());
                appendCsv(csv, stats.weather.getWindWaveHeight());
                appendCsv(csv, stats.weather.getWindWavePeriod());
                appendCsv(csv, stats.weather.getSwellWaveHeight());
                appendCsv(csv, stats.weather.getSwellWavePeriod());
                appendCsv(csv, stats.weather.getOceanCurrentVelocity());
                appendCsv(csv, stats.weather.getOceanCurrentDirection());
                appendCsv(csv, stats.weather.getSeaTemperature());
                appendCsv(csv, stats.weather.getWeatherCode());
            } else {
                for (int i = 0; i < 24; i++) {
                    csv.append("--;");
                }
            }
            csv.append("\n");
        }

        return csv.toString();
    }


    @Value("${python.pdf.service.url}")
    private String pythonUrl;
    public ReportResource generatePdfReportResource(Long tripId, String email, Map<String, Object> formData) {

        TripResponseDto trip = tripService.getUserTrips(email).stream()
                .filter(t -> t.id().equals(tripId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Brak uprawnień"));

        TripReportDataDto data = getTripReportData(tripId, email);

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> payload = mapper.convertValue(data, new TypeReference<Map<String, Object>>() {});

        if (formData != null) {
            payload.putAll(formData);
        }

        byte[] pdfContent;
        try {
            pdfContent = restTemplate.postForObject(pythonUrl, payload, byte[].class);
        } catch (Exception e) {
            throw new RuntimeException("Błąd komunikacji z serwisem PDF: " + e.getMessage());
        }

        String fileName = trip.name().replaceAll("(?i)\\.gpx$", "") + "_raport.pdf";

        return new ReportResource(pdfContent, fileName);
    }

    private void appendCsv(StringBuilder sb, Object value) {
        sb.append(value != null ? value : "--").append(";");
    }

    private String formatSeconds(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void appendAstroTime(StringBuilder csv, Instant time, TrackPoint point) {
        if (time == null || point == null) {
            appendCsv(csv, "--:--");
        } else {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");
            appendCsv(csv, fmt.format(time.atZone(DEFAULT_ZONE)));
        }
    }
}