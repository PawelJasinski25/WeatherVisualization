package jasinski.pawel.weather_visualization.dto;

import java.time.Instant;
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


    public static ReportDailySummaryDto from(DailySummary summary, List<EnrichedSegment> reducedSegments, ZoneId zoneId) {
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
                buildObservedAstroEvents(summary.astroStats(), zoneId),
                summary.timelineEvents()
        );
    }

    private static String formatTime(Instant instant, ZoneId zoneId) {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(instant.atZone(zoneId));
    }

    private static Map<String, String> buildObservedAstroEvents(AstronomyStats astro, ZoneId zoneId) {
        if (astro == null) return null;

        Map<String, String> events = new LinkedHashMap<>();

        if (astro.astronomicalDawnPt() != null && astro.astronomicalDawn() != null)
            events.put("Świt astronomiczny", formatTime(astro.astronomicalDawn(), zoneId));
        if (astro.nauticalDawnPt() != null && astro.nauticalDawn() != null)
            events.put("Świt nautyczny", formatTime(astro.nauticalDawn(), zoneId));
        if (astro.civilDawnPt() != null && astro.civilDawn() != null)
            events.put("Świt cywilny", formatTime(astro.civilDawn(), zoneId));
        if (astro.sunrisePt() != null && astro.sunrise() != null)
            events.put("Wschód Słońca", formatTime(astro.sunrise(), zoneId));
        if (astro.noonPt() != null && astro.solarNoon() != null)
            events.put("Kulminacja Słońca", formatTime(astro.solarNoon(), zoneId));
        if (astro.sunsetPt() != null && astro.sunset() != null)
            events.put("Zachód Słońca", formatTime(astro.sunset(), zoneId));
        if (astro.civilDuskPt() != null && astro.civilDusk() != null)
            events.put("Zmierzch cywilny", formatTime(astro.civilDusk(), zoneId));
        if (astro.nauticalDuskPt() != null && astro.nauticalDusk() != null)
            events.put("Zmierzch nautyczny", formatTime(astro.nauticalDusk(), zoneId));
        if (astro.astronomicalDuskPt() != null && astro.astronomicalDusk() != null)
            events.put("Zmierzch astronomiczny", formatTime(astro.astronomicalDusk(), zoneId));
        if (astro.moonRisePt() != null && astro.moonRise() != null)
            events.put("Wschód Księżyca", formatTime(astro.moonRise(), zoneId));
        if (astro.moonSetPt() != null && astro.moonSet() != null)
            events.put("Zachód Księżyca", formatTime(astro.moonSet(), zoneId));

        return events.isEmpty() ? null : events;
    }
}