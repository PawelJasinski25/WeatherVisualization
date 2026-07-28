package jasinski.pawel.weather_visualization.dto;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ReportDailySummaryDto(
        LocalDate date,
        List<TrackPointDto> points,
        DayMovementStats movementStats,
        WeatherStats overallWeatherStats,
        WeatherStats movingWeatherStats,
        SpeedStats speedStats,
        Map<String, String> observedAstroEvents,
        List<TimelineEvent> timelineEvents
) {


    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Warsaw");


    public static ReportDailySummaryDto from(DailySummary summary, List<EnrichedSegment> reducedSegments) {
        if (summary == null)
            return null;

        List<TrackPointDto> mappedPoints = new ArrayList<>();

        if (reducedSegments != null && !reducedSegments.isEmpty()) {

            EnrichedSegment firstSeg = reducedSegments.get(0);
            mappedPoints.add(TrackPointDto.fromEntityWithSpeed(firstSeg.p1(), firstSeg.rawSpeedKmh()));

            for (EnrichedSegment seg : reducedSegments) {
                mappedPoints.add(TrackPointDto.fromEntityWithSpeed(seg.p2(), seg.rawSpeedKmh()));
            }
        }

        return new ReportDailySummaryDto(
                summary.date(),
                mappedPoints,
                summary.movementStats(),
                summary.overallWeatherStats(),
                summary.movingWeatherStats(),
                summary.speedStats(),
                buildObservedAstroEvents(summary.astroStats()),
                summary.timelineEvents()
        );
    }

    private static Map<String, String> buildObservedAstroEvents(AstronomyStats astro) {
        if (astro == null) return null;

        Map<String, String> events = new LinkedHashMap<>();

        if (astro.astronomicalDawnPt()!= null && astro.astronomicalDawn() != null)
            events.put("Świt astronomiczny", astro.astronomicalDawn().atZone(DEFAULT_ZONE).toLocalDateTime().toString());
        if (astro.nauticalDawnPt() != null && astro.nauticalDawn() != null)
            events.put("Świt nautyczny", astro.nauticalDawn().atZone(DEFAULT_ZONE).toLocalDateTime().toString());
        if (astro.civilDawnPt() != null && astro.civilDawn() != null)
            events.put("Świt cywilny", astro.civilDawn().atZone(DEFAULT_ZONE).toLocalDateTime().toString());
        if (astro.sunrisePt() != null && astro.sunrise() != null)
            events.put("Wschód słońca", astro.sunrise().atZone(DEFAULT_ZONE).toLocalDateTime().toString());
        if (astro.noonPt() != null && astro.solarNoon() != null)
            events.put("Kulminacja", astro.solarNoon().atZone(DEFAULT_ZONE).toLocalDateTime().toString());
        if (astro.sunsetPt() != null && astro.sunset() != null)
            events.put("Zachód słońca", astro.sunset().atZone(DEFAULT_ZONE).toLocalDateTime().toString());
        if (astro.civilDuskPt() != null && astro.civilDusk() != null)
            events.put("Zmierzch cywilny", astro.civilDusk().atZone(DEFAULT_ZONE).toLocalDateTime().toString());
        if (astro.nauticalDusk() != null)
            events.put("Zmierzch nautyczny", astro.nauticalDusk().atZone(DEFAULT_ZONE).toLocalDateTime().toString());
        if (astro.astronomicalDusk() != null)
            events.put("Zmierzch astronomiczny", astro.astronomicalDusk().atZone(DEFAULT_ZONE).toLocalDateTime().toString());
        if (astro.moonRisePt() != null && astro.moonRise() != null)
            events.put("Wschód Księżyca", astro.moonRise().atZone(DEFAULT_ZONE).toLocalDateTime().toString());
        if (astro.moonSetPt() != null && astro.moonSet() != null)
            events.put("Zachód Księżyca", astro.moonSet().atZone(DEFAULT_ZONE).toLocalDateTime().toString());

        return events.isEmpty() ? null : events;
    }
}