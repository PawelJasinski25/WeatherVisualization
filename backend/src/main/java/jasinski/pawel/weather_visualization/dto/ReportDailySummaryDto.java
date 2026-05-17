package jasinski.pawel.weather_visualization.dto;

import jasinski.pawel.weather_visualization.utils.SpeedAnalyzer;

import java.time.LocalDate;
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


    public static ReportDailySummaryDto from(DailySummary summary) {
        if (summary == null) return null;

        List<TrackPointDto> mappedPoints = new ArrayList<>();

        if (summary.segments() != null && !summary.segments().isEmpty()) {

            for (EnrichedSegment seg : summary.segments()) {
                double speed = (seg.p1().getSpeed() != null) ? seg.p1().getSpeed() : seg.rawSpeedKmh();
                mappedPoints.add(TrackPointDto.fromEntityWithSpeed(seg.p1(), speed));
            }

            EnrichedSegment lastSeg = summary.segments().get(summary.segments().size() - 1);
            double lastSpeed = (lastSeg.p2().getSpeed() != null) ? lastSeg.p2().getSpeed() : lastSeg.rawSpeedKmh();
            mappedPoints.add(TrackPointDto.fromEntityWithSpeed(lastSeg.p2(), lastSpeed));
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

        if (astro.astronomicalDawnPt() != null)
            events.put("Świt astronomiczny", astro.astronomicalDawn().format(TIME_FORMATTER));

        if (astro.nauticalDawnPt() != null)
            events.put("Świt nautyczny", astro.nauticalDawn().format(TIME_FORMATTER));

        if (astro.civilDawnPt() != null)
            events.put("Świt cywilny", astro.civilDawn().format(TIME_FORMATTER));

        if (astro.sunrisePt() != null)
            events.put("Wschód słońca", astro.sunrise().format(TIME_FORMATTER));

        if (astro.noonPt() != null)
            events.put("Kulminacja", astro.solarNoon().format(TIME_FORMATTER));

        if (astro.sunsetPt() != null)
            events.put("Zachód słońca", astro.sunset().format(TIME_FORMATTER));

        if (astro.civilDuskPt() != null)
            events.put("Zmierzch cywilny", astro.civilDusk().format(TIME_FORMATTER));

        if (astro.nauticalDuskPt() != null)
            events.put("Zmierzch nautyczny", astro.nauticalDusk().format(TIME_FORMATTER));

        if (astro.astronomicalDuskPt() != null)
            events.put("Zmierzch astronomiczny", astro.astronomicalDusk().format(TIME_FORMATTER));

        if (astro.moonRisePt() != null)
            events.put("Wschód Księżyca", astro.moonRise().format(TIME_FORMATTER));

        if (astro.moonSetPt() != null)
            events.put("Zachód Księżyca", astro.moonSet().format(TIME_FORMATTER));

        return events.isEmpty() ? null : events;
    }
}