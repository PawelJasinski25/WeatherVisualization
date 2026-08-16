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
import java.time.temporal.ChronoUnit;
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

    @Autowired
    public ReportService(TrackPointRepository trackPointRepository, GeoNamesService geoNamesService, TripService tripService, RestTemplate restTemplate, WaterDetectionService waterDetectionService){
        this.trackPointRepository = trackPointRepository;
        this.geoNamesService = geoNamesService;
        this.tripService = tripService;
        this.restTemplate = restTemplate;
        this.waterDetectionService = waterDetectionService;
    }

    private TripAnalysisContext analyzeTrip(Long tripId, ZoneId zoneId) {
        List<TrackPoint> allPoints = trackPointRepository.findByTripIdOrderByTimeAsc(tripId);
        if (allPoints.isEmpty()) {
            return new TripAnalysisContext(new ArrayList<>(), new TreeMap<>(), new ArrayList<>());
        }

        Map<LocalDate, DayData> dailyMovements = MovementAnalyzer.analyzeTripTimeline(allPoints, zoneId);
        List<EnrichedSegment> rawSegments = createEnrichedSegments(allPoints, dailyMovements, zoneId);
        List<EnrichedSegment> cleanSegments = removeAnomalies(rawSegments);

        return new TripAnalysisContext(allPoints, dailyMovements, cleanSegments);
    }


    public List<DailySummary> generateDailySummaries(TripAnalysisContext context, ZoneId zoneId) {
        if (context.points().isEmpty()) return new ArrayList<>();

        Map<LocalDate, List<TrackPoint>> pointsByDay = new TreeMap<>();
        for (TrackPoint point : context.points()) {
            LocalDate date = LocalDate.ofInstant(point.getTime(), zoneId);
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
            List<TimelineEvent> eventsForAstro = new ArrayList<>(events);

            LocalDate nextDay = day.plusDays(1);

            DayData nextMovData = context.dailyMovements().get(nextDay);
            if (nextMovData != null && nextMovData.events != null) {
                for (TimelineEvent ev : nextMovData.events) {
                    if (ev.start().atZone(zoneId).getHour() <= 5) {
                        eventsForAstro.add(ev);
                    }
                }
            }
            AstronomyStats astro = AstronomyAnalyzer.calculateSun(pointsInDay, context.points(), eventsForAstro, zoneId);

            List<EnrichedSegment> dailySegments = context.segments().stream()
                    .filter(s -> LocalDate.ofInstant(s.p1().getTime(), zoneId).equals(day))
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

    @Cacheable(value = "reportData", key = "#tripId + '_' + #timezone")
    public TripReportDataDto getTripReportData(Long tripId, String email, String timezone) {
        ZoneId zoneId = ZoneId.of(timezone);

        TripResponseDto trip = tripService.getUserTrips(email).stream()
                .filter(t -> t.id().equals(tripId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Brak uprawnień"));

        TripAnalysisContext context = analyzeTrip(tripId, zoneId);
        List<DailySummary> dailySummaries = generateDailySummaries(context, zoneId);

        return buildReportDataDto(trip, context, dailySummaries, zoneId);
    }

    private TripReportDataDto buildReportDataDto(TripResponseDto trip, TripAnalysisContext context, List<DailySummary> dailySummaries, ZoneId zoneId) {
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
                    return ReportDailySummaryDto.from(summary, reducedSegments, zoneId);
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

            double dur = Math.abs(Duration.between(p1.getTime(), p2.getTime()).toMillis()) / 1000.0;

            if (dur > 0) {
                boolean isMoving = false;
                boolean isDataGap = false;

                LocalDate date = LocalDate.ofInstant(p1.getTime(), zoneId);
                DayData movData = dailyMovements.get(date);

                if (movData != null && movData.events != null) {
                    for (TimelineEvent ev : movData.events) {
                        if (!p1.getTime().isBefore(ev.start()) && p1.getTime().isBefore(ev.end())) {
                            if ("RUCH".equals(ev.type())) {
                                isMoving = true;
                            } else if ("BRAK DANYCH".equals(ev.type())) {
                                isDataGap = true;
                            }
                            break;
                        }
                    }
                }

                boolean isDifferentSegment = p1.getSegmentId() == null || p2.getSegmentId() == null || !p1.getSegmentId().equals(p2.getSegmentId());

                //jeżeli id segmentów się rożni i jest brak danych to prędkość i dystans nie jest liczony
                if (isDataGap && isDifferentSegment) {
                    segments.add(new EnrichedSegment(p1, p2, null, dur, null, false));
                    continue;
                }

                double dist = GeoUtils.calculateDistance(p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude());
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
        return segments;
    }

    private List<EnrichedSegment> removeAnomalies(List<EnrichedSegment> rawSegments) {
        if (rawSegments == null || rawSegments.size() < 3) {
            return rawSegments;
        }

        List<EnrichedSegment> cleanSegments = new ArrayList<>(rawSegments.size());

        List<Double> speeds = new ArrayList<>(rawSegments.size());
        List<Double> durations = new ArrayList<>(rawSegments.size());

        for (EnrichedSegment seg : rawSegments) {
            speeds.add(seg.rawSpeedKmh());
            durations.add(seg.durationSeconds());
        }

        for (int i = 0; i < rawSegments.size(); i++) {
            EnrichedSegment seg = rawSegments.get(i);

            if (seg.isMoving() && SpeedAnalyzer.isAnomaly(speeds, durations, i)) {
                Double fallbackSpeed = (i > 0 && speeds.get(i - 1) != null) ? speeds.get(i - 1) : 0.0;
                Double correctedDist = (fallbackSpeed / 3.6) * seg.durationSeconds();
                speeds.set(i, fallbackSpeed);

                cleanSegments.add(new EnrichedSegment(
                        seg.p1(), seg.p2(), correctedDist, seg.durationSeconds(), fallbackSpeed, seg.isMoving()
                ));
            } else {
                cleanSegments.add(seg);
            }
        }
        return cleanSegments;
    }

    public ReportResource getCsvReportResource(Long tripId, String email, Map<String, String> prefs) {
        if(prefs == null) prefs = new HashMap<>();
        String timeZoneStr = prefs.getOrDefault("timezone", "UTC");
        ZoneId zoneId = ZoneId.of(timeZoneStr);
        TripResponseDto trip = tripService.getUserTrips(email).stream()
                .filter(t -> t.id().equals(tripId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Brak uprawnień"));

        TripAnalysisContext context = analyzeTrip(tripId,zoneId);
        List<DailySummary> summaries = generateDailySummaries(context, zoneId);

        String mainCsvContent = generateSummaryCsv(context, summaries, prefs, zoneId);
        String apiCsvContent = generateApiUsageCsv(context, prefs, zoneId);
        String detailedPointsCsvContent = generateDetailedPointsCsv(context, summaries, prefs, zoneId);

        TripReportDataDto data = buildReportDataDto(trip, context, summaries, zoneId);

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> payload = mapper.convertValue(data, new TypeReference<Map<String, Object>>() {});

        payload.put("preferences", prefs);

        byte[] chartsZipBytes = null;
        try {
            String pythonZipUrl = pythonUrl.replace("/generate-pdf", "/generate-charts-zip");
            chartsZipBytes = restTemplate.postForObject(pythonZipUrl, payload, byte[].class);
        } catch (Exception e) {
            System.err.println("Nie udało się pobrać wykresów: " + e.getMessage());
        }

        byte[] bom = new byte[] { (byte)0xEF, (byte)0xBB, (byte)0xBF };
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        String safeTripName = trip.name().replaceAll("(?i)\\.gpx$", "").replaceAll("[\\\\/:*?\"<>|\\s]", "_");
        String rawDate = prefs.getOrDefault("startDate", LocalDate.ofInstant(trip.startTime(), zoneId).toString());
        String startDateStr = rawDate.replaceAll("[\\\\/:*?\"<>|\\s]", "_");
        String filePrefix = safeTripName + "_" + startDateStr + "_";

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {


            // Główne podsumowanie trasy
            zipOutputStream.putNextEntry(new ZipEntry(filePrefix + "podsumowanie_trasy.csv"));
            zipOutputStream.write(bom);
            zipOutputStream.write(mainCsvContent.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            // Punkty z bazy i zapytania
            zipOutputStream.putNextEntry(new ZipEntry(filePrefix + "punkty.csv"));
            zipOutputStream.write(bom);
            zipOutputStream.write(apiCsvContent.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            // Szczegóły punktów (jeden punkt - jeden wiersz)
            zipOutputStream.putNextEntry(new ZipEntry(filePrefix + "szczegolowe_punkty.csv"));
            zipOutputStream.write(bom);
            zipOutputStream.write(detailedPointsCsvContent.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            //Wykresy
            if (chartsZipBytes != null) {
                try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(chartsZipBytes))) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        zipOutputStream.putNextEntry(new ZipEntry("Wykresy/" + entry.getName()));
                        zis.transferTo(zipOutputStream);
                        zipOutputStream.closeEntry();
                    }
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Błąd podczas generowania pliku ZIP", e);
        }

        String fileName = trip.name().replaceAll("(?i)\\.gpx$", "") + "_raport.zip";

        return new ReportResource(byteArrayOutputStream.toByteArray(), fileName);
    }

    public String generateSummaryCsv(TripAnalysisContext context, List<DailySummary> summaries, Map<String, String> prefs, ZoneId zoneId) {
        StringBuilder csv = new StringBuilder();

        int maxEvents = 0;
        for (DailySummary summary : summaries) {
            if (summary.timelineEvents() != null) {
                maxEvents = Math.max(maxEvents, summary.timelineEvents().size());
            }
        }
        maxEvents = Math.max(1, maxEvents);

        String tempUnit = prefs.getOrDefault("temp", "°C");
        String windUnit = prefs.getOrDefault("wind", "km/h");
        String pressureUnit = prefs.getOrDefault("pressure", "hPa");
        String rainUnit = prefs.getOrDefault("rain", "mm");
        String snowUnit = prefs.getOrDefault("snow", "cm");
        String waveUnit = prefs.getOrDefault("wave", "m");
        String currentsUnit = prefs.getOrDefault("currents", "km/h");

        csv.append("Data;Start;Koniec;Czas w ruchu;Czas na postoju;Czas braku danych;")
                .append("Średnia temperatura (").append(tempUnit).append(");Średnia temperatura w ruchu (").append(tempUnit).append(");")
                .append("Średnia siła wiatru (").append(windUnit).append(");Średnia siła wiatru w ruchu (").append(windUnit).append(");")
                .append("Średni kierunek wiatru (°);Średni kierunek wiatru w ruchu (°);")
                .append("Średnie porywy wiatru (").append(windUnit).append(");Średnie porywy wiatru w ruchu (").append(windUnit).append(");")
                .append("Średni punkt rosy (").append(tempUnit).append(");Średni punkt rosy w ruchu (").append(tempUnit).append(");")
                .append("Suma opadów deszczu (").append(rainUnit).append(");Suma opadów deszczu w ruchu (").append(rainUnit).append(");")
                .append("Suma opadów śniegu (").append(snowUnit).append(");Suma opadów śniegu w ruchu (").append(snowUnit).append(");")
                .append("Średnia wilgotność (%);Średnia wilgotność w ruchu (%);")
                .append("Średnie ciśnienie (").append(pressureUnit).append(");Średnie ciśnienie w ruchu (").append(pressureUnit).append(");")
                .append("Średnie zachmurzenie (%);Średnie zachmurzenie w ruchu (%);Średnie chmury niskie (%);Średnie chmury niskie w ruchu (%);Średnie chmury średnie (%);Średnie chmury średnie w ruchu (%);Średnie chmury wysokie (%);Średnie chmury wysokie w ruchu (%);")
                .append("Średnia wysokość fal (").append(waveUnit).append(");Średnia wysokość fal w ruchu (").append(waveUnit).append(");Średni okres fal (s);Średni okres fal w ruchu (s);Średni kierunek fal (°);Średni kierunek fal w ruchu (°);")
                .append("Średnia wysokość fal wiatrowych (").append(waveUnit).append(");Średnia wysokość fal wiatrowych w ruchu (").append(waveUnit).append(");Średni okres fal wiatrowych (s);Średni okres fal wiatrowych w ruchu (s);")
                .append("Średnia wysokość martwej fali (").append(waveUnit).append(");Średnia wysokość martwej fali w ruchu (").append(waveUnit).append(");Średni okres martwej fali (s);Średni okres martwej fali w ruchu (s);")
                .append("Średnia prędkość prądów (").append(currentsUnit).append(");Średnia prędkość prądów w ruchu (").append(currentsUnit).append(");Średni kierunek prądów (°);Średni kierunek prądów w ruchu (°);")
                .append("Średnia temperatura morza (").append(tempUnit).append(");Średnia temperatura morza w ruchu (").append(tempUnit).append(");")
                .append("Świt astronomiczny;Świt nautyczny;Świt cywilny;Wschód Słońca;Kulminacja Słońca;Zachód Słońca;")
                .append("Zmierzch cywilny;Zmierzch nautyczny;Zmierzch astronomiczny;");

        for (int i = 1; i <= maxEvents; i++) {
            csv.append("Faza ").append(i).append(" Typ;");
            csv.append("Faza ").append(i).append(" Start;");
            csv.append("Faza ").append(i).append(" Koniec;");
            csv.append("Faza ").append(i).append(" Czas trwania;");
            csv.append("Faza ").append(i).append(" Miejsce;");
        }
        csv.append("\n");

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(zoneId);

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

            appendCsv(csv, formatUnit(oWs.avgTemp(), tempUnit, "temp")); appendCsv(csv, formatUnit(mWs.avgTemp(), tempUnit, "temp"));
            appendCsv(csv, formatUnit(oWs.avgWindSpeed(), windUnit, "wind")); appendCsv(csv, formatUnit(mWs.avgWindSpeed(), windUnit, "wind"));
            appendCsv(csv, oWs.avgWindDir()); appendCsv(csv, mWs.avgWindDir());
            appendCsv(csv, formatUnit(oWs.avgWindGusts(), windUnit, "wind")); appendCsv(csv, formatUnit(mWs.avgWindGusts(), windUnit, "wind"));
            appendCsv(csv, formatUnit(oWs.avgDewPoint(), tempUnit, "temp")); appendCsv(csv, formatUnit(mWs.avgDewPoint(), tempUnit, "temp"));

            appendCsv(csv, formatUnit(oWs.sumRain(), rainUnit, "rain")); appendCsv(csv, formatUnit(mWs.sumRain(), rainUnit, "rain"));
            appendCsv(csv, formatUnit(oWs.sumSnowfall(), snowUnit, "snow")); appendCsv(csv, formatUnit(mWs.sumSnowfall(), snowUnit, "snow"));
            appendCsv(csv, oWs.avgHumidity()); appendCsv(csv, mWs.avgHumidity());
            appendCsv(csv, formatUnit(oWs.avgPressure(), pressureUnit, "pressure")); appendCsv(csv, formatUnit(mWs.avgPressure(), pressureUnit, "pressure"));

            appendCsv(csv, oWs.avgCloudCover()); appendCsv(csv, mWs.avgCloudCover());
            appendCsv(csv, oWs.avgCloudCoverLow()); appendCsv(csv, mWs.avgCloudCoverLow());
            appendCsv(csv, oWs.avgCloudCoverMid()); appendCsv(csv, mWs.avgCloudCoverMid());
            appendCsv(csv, oWs.avgCloudCoverHigh()); appendCsv(csv, mWs.avgCloudCoverHigh());

            appendCsv(csv, formatUnit(oWs.avgWaveHeight(), waveUnit, "wave")); appendCsv(csv, formatUnit(mWs.avgWaveHeight(), waveUnit, "wave"));
            appendCsv(csv, oWs.avgWavePeriod()); appendCsv(csv, mWs.avgWavePeriod());
            appendCsv(csv, oWs.avgWaveDirection()); appendCsv(csv, mWs.avgWaveDirection());

            appendCsv(csv, formatUnit(oWs.avgWindWaveHeight(), waveUnit, "wave")); appendCsv(csv, formatUnit(mWs.avgWindWaveHeight(), waveUnit, "wave"));
            appendCsv(csv, oWs.avgWindWavePeriod()); appendCsv(csv, mWs.avgWindWavePeriod());
            appendCsv(csv, formatUnit(oWs.avgSwellWaveHeight(), waveUnit, "wave")); appendCsv(csv, formatUnit(mWs.avgSwellWaveHeight(), waveUnit, "wave"));
            appendCsv(csv, oWs.avgSwellWavePeriod()); appendCsv(csv, mWs.avgSwellWavePeriod());

            appendCsv(csv, formatUnit(oWs.avgOceanCurrentVelocity(), currentsUnit, "currents")); appendCsv(csv, formatUnit(mWs.avgOceanCurrentVelocity(), currentsUnit, "currents"));
            appendCsv(csv, oWs.avgOceanCurrentDirection()); appendCsv(csv, mWs.avgOceanCurrentDirection());
            appendCsv(csv, formatUnit(oWs.avgSeaTemperature(), tempUnit, "temp")); appendCsv(csv, formatUnit(mWs.avgSeaTemperature(), tempUnit, "temp"));

            AstronomyStats astro = summary.astroStats();
            appendAstroTime(csv, astro.astronomicalDawn(), astro.astronomicalDawnPt(),zoneId);
            appendAstroTime(csv, astro.nauticalDawn(), astro.nauticalDawnPt(),zoneId);
            appendAstroTime(csv, astro.civilDawn(), astro.civilDawnPt(),zoneId);
            appendAstroTime(csv, astro.sunrise(), astro.sunrisePt(),zoneId);
            appendAstroTime(csv, astro.solarNoon(), astro.noonPt(),zoneId);
            appendAstroTime(csv, astro.sunset(), astro.sunsetPt(),zoneId);
            appendAstroTime(csv, astro.civilDusk(), astro.civilDuskPt(),zoneId);
            appendAstroTime(csv, astro.nauticalDusk(), astro.nauticalDuskPt(),zoneId);
            appendAstroTime(csv, astro.astronomicalDusk(), astro.astronomicalDuskPt(),zoneId);

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

    public String generateApiUsageCsv(TripAnalysisContext context, Map<String, String> prefs, ZoneId zoneId) {
        StringBuilder csv = new StringBuilder();
        DateTimeFormatter fullTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(zoneId);

        class ApiUsageStats {
            Instant firstPointTime;
            Instant lastPointTime;
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

        String tempUnit = prefs.getOrDefault("temp", "°C");
        String windUnit = prefs.getOrDefault("wind", "km/h");
        String pressureUnit = prefs.getOrDefault("pressure", "hPa");
        String rainUnit = prefs.getOrDefault("rain", "mm");
        String snowUnit = prefs.getOrDefault("snow", "cm");
        String waveUnit = prefs.getOrDefault("wave", "m");
        String currentsUnit = prefs.getOrDefault("currents", "km/h");

        csv.append("Data punktu z bazy;Szerokość;Długość;Szerokość zaokrąglona;Długość zaokrąglona;Źródło danych;Open-Meteo Historical API (0/1);Open-Meteo Marine API (0/1);Dopasowane punkty;Dopasowane punkty od;Dopasowane punkty do;");
        csv.append("Temperatura (").append(tempUnit).append(");Prędkość wiatru (").append(windUnit).append(");Kierunek wiatru (°);Punkt rosy (").append(tempUnit).append(");Porywy wiatru (").append(windUnit).append(");Opady deszczu (").append(rainUnit).append(");Opady śniegu (").append(snowUnit).append(");Wilgotność (%);Ciśnienie (").append(pressureUnit).append(");Zachmurzenie ogólne (%);Chmury niskie (%);Chmury średnie (%);Chmury wysokie (%);Wysokość fali (").append(waveUnit).append(");Okres fali (s);Kierunek fali (°);Wysokość fal wiatrowych (").append(waveUnit).append(");Okres fal wiatrowych (s);Wysokość martwej fali (").append(waveUnit).append(");Okres martwej fali (s);Prędkość prądów (").append(currentsUnit).append(");Kierunek prądów (°);Temperatura morza (").append(tempUnit).append(");Kod pogody\n");

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

            Weather w = stats.weather;
            if (w != null) {
                appendCsv(csv, formatUnit(w.getTemp(), tempUnit, "temp"));
                appendCsv(csv, formatUnit(w.getWindSpeed(), windUnit, "wind"));
                appendCsv(csv, w.getWindDir());
                appendCsv(csv, formatUnit(w.getDewPoint(), tempUnit, "temp"));
                appendCsv(csv, formatUnit(w.getWindGusts(), windUnit, "wind"));
                appendCsv(csv, formatUnit(w.getRain(), rainUnit, "rain"));
                appendCsv(csv, formatUnit(w.getSnowfall(), snowUnit, "snow"));
                appendCsv(csv, w.getHumidity());
                appendCsv(csv, formatUnit(w.getPressure(), pressureUnit, "pressure"));
                appendCsv(csv, w.getCloudCover());
                appendCsv(csv, w.getCloudCoverLow());
                appendCsv(csv, w.getCloudCoverMid());
                appendCsv(csv, w.getCloudCoverHigh());
                appendCsv(csv, formatUnit(w.getWaveHeight(), waveUnit, "wave"));
                appendCsv(csv, w.getWavePeriod());
                appendCsv(csv, w.getWaveDirection());
                appendCsv(csv, formatUnit(w.getWindWaveHeight(), waveUnit, "wave"));
                appendCsv(csv, w.getWindWavePeriod());
                appendCsv(csv, formatUnit(w.getSwellWaveHeight(), waveUnit, "wave"));
                appendCsv(csv, w.getSwellWavePeriod());
                appendCsv(csv, formatUnit(w.getOceanCurrentVelocity(), currentsUnit, "currents"));
                appendCsv(csv, w.getOceanCurrentDirection());
                appendCsv(csv, formatUnit(w.getSeaTemperature(), tempUnit, "temp"));
                appendCsv(csv, w.getWeatherCode());
            } else {
                for (int i = 0; i < 24; i++) csv.append("--;");
            }
            csv.append("\n");
        }
        return csv.toString();
    }

    public String generateDetailedPointsCsv(TripAnalysisContext context, List<DailySummary> summaries, Map<String, String> prefs, ZoneId zoneId) {

        StringBuilder csv = new StringBuilder();
        DateTimeFormatter fullTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(zoneId);

        String tempUnit = prefs.getOrDefault("temp", "°C");
        String windUnit = prefs.getOrDefault("wind", "km/h");
        String pressureUnit = prefs.getOrDefault("pressure", "hPa");
        String rainUnit = prefs.getOrDefault("rain", "mm");
        String snowUnit = prefs.getOrDefault("snow", "cm");
        String waveUnit = prefs.getOrDefault("wave", "m");
        String currentsUnit = prefs.getOrDefault("currents", "km/h");
        String distanceUnit = prefs.getOrDefault("distance", "NM");
        String speedUnit = prefs.getOrDefault("speed", "kt");

        csv.append("ID punktu;Czas punktu;Szerokość punktu;Długość punktu;Stan ruchu;Miejsce postoju;Szerokość zaokr. o 0,1°;Długość zaokr. o 0,1°;Czy współrzędne z zapytania API;Odchylenie od współrzędnych z zapytania (km);Czas z zapytania API;Różnica od czasu z zapytania API (min);Akwen morski;");
        csv.append("Czas od poprzedniego punktu (min);Odległość od poprzedniego punktu (").append(distanceUnit).append(");Kurs (°);Prędkość z GPX (").append(speedUnit).append(");Prędkość wyliczona (").append(speedUnit).append(");");
        csv.append("Temperatura (").append(tempUnit).append(");Prędkość wiatru (").append(windUnit).append(");Kierunek wiatru (°);Punkt rosy (").append(tempUnit).append(");Porywy wiatru (").append(windUnit).append(");Opady deszczu (").append(rainUnit).append(");Opady śniegu (").append(snowUnit).append(");Wilgotność (%);Ciśnienie (").append(pressureUnit).append(");Zachmurzenie ogólne (%);Chmury niskie (%);Chmury średnie (%);Chmury wysokie (%);Wysokość fali (").append(waveUnit).append(");Okres fali (s);Kierunek fali (°);Wysokość fal wiatrowych (").append(waveUnit).append(");Okres fal wiatrowych (s);Wysokość martwej fali (").append(waveUnit).append(");Okres martwej fali (s);Prędkość prądów (").append(currentsUnit).append(");Kierunek prądów (°);Temperatura morza (").append(tempUnit).append(");Kod pogody\n");

        Map<String, TrackPoint> gridRepresentatives = new HashMap<>();
        List<TrackPoint> points = context.points();
        int pointSeq = 1;

        Map<LocalDate, List<TimelineEvent>> eventsByDay = new HashMap<>();
        for (DailySummary ds : summaries) {
            eventsByDay.put(ds.date(), ds.timelineEvents() != null ? ds.timelineEvents() : new ArrayList<>());
        }

        for (int i = 0; i < points.size(); i++) {
            TrackPoint pt = points.get(i);

            String dateStr = pt.getTime().toString().substring(0, 10);
            double gridLat = Math.round(pt.getLatitude() * 10.0) / 10.0;
            double gridLon = Math.round(pt.getLongitude() * 10.0) / 10.0;
            String gridKey = dateStr + "_" + gridLat + "_" + gridLon;

            TrackPoint rep = gridRepresentatives.get(gridKey);
            boolean isRep = false;
            if (rep == null) {
                rep = pt;
                gridRepresentatives.put(gridKey, rep);
                isRep = true;
            }

            double offsetKm = GeoUtils.calculateDistance(pt.getLatitude(), pt.getLongitude(), rep.getLatitude(), rep.getLongitude()) / 1000.0;

            String movementStatus = "POSTÓJ";
            String placeName = "--";
            if (pt.getTime() != null) {
                LocalDate localDate = LocalDate.ofInstant(pt.getTime(), zoneId);
                List<TimelineEvent> dailyEvents = eventsByDay.get(localDate);
                if (dailyEvents != null) {
                    for (TimelineEvent ev : dailyEvents) {
                        if (!pt.getTime().isBefore(ev.start()) && !pt.getTime().isAfter(ev.end())) {
                            if (!"BRAK DANYCH".equals(ev.type())) {
                                movementStatus = ev.type();

                                // Jeśli jesteśmy na postoju, bierzemy gotową nazwę!
                                if ("POSTÓJ".equals(movementStatus) && ev.placeName() != null && !ev.placeName().isEmpty()) {
                                    placeName = ev.placeName();
                                }
                                break;
                            }
                        }
                    }
                }
            }

            String apiTimeStr = "--";
            String timeDiffMinStr = "--";
            Weather w = pt.getWeather();

            if (pt.getTime() != null) {
                Instant apiTime = pt.getTime().plus(30, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.HOURS);
                apiTimeStr = fullTimeFormatter.format(apiTime);
                double diffMin = Math.abs(Duration.between(pt.getTime(), apiTime).getSeconds()) / 60.0;
                timeDiffMinStr = String.format(Locale.US, "%.1f", diffMin);
            }

            String timeIntervalMinStr = "--";
            String distanceMmStr = "--";
            String courseDegStr = "--";

            String speedGpxStr = "--";
            if(pt.getSpeed() != null) {
                speedGpxStr = formatUnit(pt.getSpeed(), speedUnit, "speed");
            }

            String speedCalcKnStr = "--";

            if (i > 0) {
                TrackPoint prev = points.get(i - 1);
                TrackPoint current = pt;

                if (prev.getTime() != null && current.getTime() != null) {
                    double secondsBetween = Math.abs(Duration.between(prev.getTime(), current.getTime()).getSeconds());
                    double minutesBetween = secondsBetween / 60.0;
                    timeIntervalMinStr = String.format(Locale.US, "%.1f", minutesBetween);

                    double distanceMeters = GeoUtils.calculateDistance(prev.getLatitude(), prev.getLongitude(), current.getLatitude(), current.getLongitude());
                    distanceMmStr = formatUnit(distanceMeters, distanceUnit, "distance");

                    if (secondsBetween > 0) {
                        double hoursBetween = secondsBetween / 3600.0;
                        double speedKmhCalc = (distanceMeters / 1000.0) / hoursBetween;
                        speedCalcKnStr = formatUnit(speedKmhCalc, speedUnit, "speed");
                    }
                }

                double lat1 = Math.toRadians(prev.getLatitude());
                double lon1 = Math.toRadians(prev.getLongitude());
                double lat2 = Math.toRadians(current.getLatitude());
                double lon2 = Math.toRadians(current.getLongitude());

                double dLon = lon2 - lon1;
                double y = Math.sin(dLon) * Math.cos(lat2);
                double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
                double azimuth = Math.toDegrees(Math.atan2(y, x));
                double course = (azimuth + 360) % 360;
                courseDegStr = String.format(Locale.US, "%.1f", course);
            }

            boolean isWater = waterDetectionService.isWater(pt.getLatitude(), pt.getLongitude());

            appendCsv(csv, pt.getId() != null ? pt.getId() : pointSeq);
            appendCsv(csv, pt.getTime() != null ? fullTimeFormatter.format(pt.getTime()) : "--");
            appendCsv(csv, String.format(Locale.US, "%.6f", pt.getLatitude()));
            appendCsv(csv, String.format(Locale.US, "%.6f", pt.getLongitude()));
            appendCsv(csv, movementStatus);
            appendCsv(csv, placeName);
            appendCsv(csv, String.format(Locale.US, "%.1f", gridLat));
            appendCsv(csv, String.format(Locale.US, "%.1f", gridLon));
            appendCsv(csv, isRep ? "TAK" : "NIE");
            appendCsv(csv, String.format(Locale.US, "%.2f", offsetKm));
            appendCsv(csv, apiTimeStr);
            appendCsv(csv, timeDiffMinStr);
            appendCsv(csv, isWater ? "TAK" : "NIE");

            appendCsv(csv, timeIntervalMinStr);
            appendCsv(csv, distanceMmStr);
            appendCsv(csv, courseDegStr);
            appendCsv(csv, speedGpxStr);
            appendCsv(csv, speedCalcKnStr);

            if (w != null) {
                appendCsv(csv, formatUnit(w.getTemp(), tempUnit, "temp"));
                appendCsv(csv, formatUnit(w.getWindSpeed(), windUnit, "wind"));
                appendCsv(csv, w.getWindDir());
                appendCsv(csv, formatUnit(w.getDewPoint(), tempUnit, "temp"));
                appendCsv(csv, formatUnit(w.getWindGusts(), windUnit, "wind"));
                appendCsv(csv, formatUnit(w.getRain(), rainUnit, "rain"));
                appendCsv(csv, formatUnit(w.getSnowfall(), snowUnit, "snow"));
                appendCsv(csv, w.getHumidity());
                appendCsv(csv, formatUnit(w.getPressure(), pressureUnit, "pressure"));
                appendCsv(csv, w.getCloudCover());
                appendCsv(csv, w.getCloudCoverLow());
                appendCsv(csv, w.getCloudCoverMid());
                appendCsv(csv, w.getCloudCoverHigh());
                appendCsv(csv, formatUnit(w.getWaveHeight(), waveUnit, "wave"));
                appendCsv(csv, w.getWavePeriod());
                appendCsv(csv, w.getWaveDirection());
                appendCsv(csv, formatUnit(w.getWindWaveHeight(), waveUnit, "wave"));
                appendCsv(csv, w.getWindWavePeriod());
                appendCsv(csv, formatUnit(w.getSwellWaveHeight(), waveUnit, "wave"));
                appendCsv(csv, w.getSwellWavePeriod());
                appendCsv(csv, formatUnit(w.getOceanCurrentVelocity(), currentsUnit, "currents"));
                appendCsv(csv, w.getOceanCurrentDirection());
                appendCsv(csv, formatUnit(w.getSeaTemperature(), tempUnit, "temp"));
                appendCsv(csv, w.getWeatherCode());
            } else {
                for (int i_csv = 0; i_csv < 24; i_csv++) {
                    csv.append("--;");
                }
            }
            csv.append("\n");
            pointSeq++;
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

        String timeZoneStr = "UTC";
        if (formData != null && formData.containsKey("preferences")) {
            Map<String, Object> prefs = (Map<String, Object>) formData.get("preferences");
            if (prefs != null && prefs.containsKey("timezone")) {
                timeZoneStr = (String) prefs.get("timezone");
            }
        } else if (formData != null && formData.containsKey("timezone")) {
            timeZoneStr = (String) formData.get("timezone");
        }

        TripReportDataDto data = getTripReportData(tripId, email,timeZoneStr);

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

    private void appendAstroTime(StringBuilder csv, Instant time, TrackPoint point, ZoneId zoneId){
        if (time == null || point == null) {
            appendCsv(csv, "--:--");
        } else {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");
            appendCsv(csv, fmt.format(time.atZone(zoneId)));
        }
    }

    private String formatUnit(Double value, String targetUnit, String category) {
        if (value == null) return "--";
        double res = value;
        try {
            switch (category) {
                case "temp":
                    if ("°F".equals(targetUnit)) res = (value * 9.0 / 5.0) + 32;
                    break;
                case "wind":
                case "currents":
                case "speed":
                    if ("kt".equals(targetUnit)) res = value / 1.852;
                    else if ("m/s".equals(targetUnit)) res = value / 3.6;
                    else if ("mph".equals(targetUnit)) res = value / 1.609344;
                    else if ("bft".equals(targetUnit)) return String.valueOf(getBft(value));
                    break;
                case "distance":
                    if ("km".equals(targetUnit)) res = value / 1000.0;
                    else if ("NM".equals(targetUnit) || "MM".equals(targetUnit)) res = value / 1852.0;
                    else if ("mi".equals(targetUnit)) res = value / 1609.344;
                    break;
                case "pressure":
                    if ("inHg".equals(targetUnit)) res = value / 33.863886666667;
                    else if ("mmHg".equals(targetUnit)) res = value * 0.75006157584566;
                    break;
                case "wave":
                    if ("ft".equals(targetUnit)) res = value * 3.280839895;
                    break;
                case "rain":
                    if ("inch".equals(targetUnit)) res = value / 25.4;
                    break;
                case "snow":
                    if ("mm".equals(targetUnit)) res = value * 10.0;
                    else if ("inch".equals(targetUnit)) res = value / 2.54;
                    break;
            }
            return String.format(Locale.US, "%.2f", res);
        } catch (Exception e) {
            return "--";
        }
    }

    private int getBft(double kmh) {
        if (kmh < 2) return 0;
        if (kmh <= 5) return 1;
        if (kmh <= 11) return 2;
        if (kmh <= 19) return 3;
        if (kmh <= 28) return 4;
        if (kmh <= 38) return 5;
        if (kmh <= 49) return 6;
        if (kmh <= 61) return 7;
        if (kmh <= 74) return 8;
        if (kmh <= 88) return 9;
        if (kmh <= 102) return 10;
        if (kmh <= 117) return 11;
        return 12;
    }
}