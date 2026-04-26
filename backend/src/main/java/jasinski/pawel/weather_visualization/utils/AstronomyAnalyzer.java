package jasinski.pawel.weather_visualization.utils;

import jasinski.pawel.weather_visualization.dto.AstronomyStats;
import jasinski.pawel.weather_visualization.dto.TimelineEvent;
import jasinski.pawel.weather_visualization.entity.TrackPoint;
import org.shredzone.commons.suncalc.SunTimes;
import org.shredzone.commons.suncalc.SunTimes.Twilight;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class AstronomyAnalyzer {

    // Kontener łączący czas z fizycznym punktem na trasie (jeśli wystąpił)
    public record EventPoint(LocalTime time, TrackPoint point) {}

    // Tolerancja: 15 minut. Zabezpiecza sytuacje na granicy zdarzeń.
    private static final long TOLERANCE_SECONDS = 15 * 60;

    // ZMIANA: Przyjmujemy List<TimelineEvent> wyliczoną przez MovementAnalyzer
    public static AstronomyStats calculateSun(List<TrackPoint> pointsOfDay, List<TimelineEvent> eventsOfDay, ZoneId zoneId) {
        if (pointsOfDay == null || pointsOfDay.isEmpty()) {
            return new AstronomyStats(
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null
            );
        }

        TrackPoint middlePoint = pointsOfDay.get(pointsOfDay.size() / 2);
        double lat = middlePoint.getLocation().getY();
        double lng = middlePoint.getLocation().getX();
        Instant baseTime = middlePoint.getTime();

        // 1. Wyliczamy DOKŁADNE czasy zjawisk dla tego dnia i miejsca (obliczane od północy danego dnia)
        Instant exactAstroDawn = getRawEventTime(baseTime, lat, lng, zoneId, Twilight.ASTRONOMICAL, true);
        Instant exactNautDawn  = getRawEventTime(baseTime, lat, lng, zoneId, Twilight.NAUTICAL, true);
        Instant exactCivilDawn = getRawEventTime(baseTime, lat, lng, zoneId, Twilight.CIVIL, true);
        Instant exactSunrise   = getRawEventTime(baseTime, lat, lng, zoneId, Twilight.VISUAL, true);
        Instant exactNoon      = getNoonTime(baseTime, lat, lng, zoneId);
        Instant exactSunset    = getRawEventTime(baseTime, lat, lng, zoneId, Twilight.VISUAL, false);
        Instant exactCivilDusk = getRawEventTime(baseTime, lat, lng, zoneId, Twilight.CIVIL, false);
        Instant exactNautDusk  = getRawEventTime(baseTime, lat, lng, zoneId, Twilight.NAUTICAL, false);
        Instant exactAstroDusk = getRawEventTime(baseTime, lat, lng, zoneId, Twilight.ASTRONOMICAL, false);

        // 2. Szukamy punktów GPS w oparciu o okresy z MovementAnalyzera
        EventPoint aDawn = refineEventTime(pointsOfDay, eventsOfDay, exactAstroDawn, zoneId);
        EventPoint nDawn = refineEventTime(pointsOfDay, eventsOfDay, exactNautDawn, zoneId);
        EventPoint cDawn = refineEventTime(pointsOfDay, eventsOfDay, exactCivilDawn, zoneId);
        EventPoint rise  = refineEventTime(pointsOfDay, eventsOfDay, exactSunrise, zoneId);
        EventPoint noon  = refineEventTime(pointsOfDay, eventsOfDay, exactNoon, zoneId);
        EventPoint set   = refineEventTime(pointsOfDay, eventsOfDay, exactSunset, zoneId);
        EventPoint cDusk = refineEventTime(pointsOfDay, eventsOfDay, exactCivilDusk, zoneId);
        EventPoint nDusk = refineEventTime(pointsOfDay, eventsOfDay, exactNautDusk, zoneId);
        EventPoint aDusk = refineEventTime(pointsOfDay, eventsOfDay, exactAstroDusk, zoneId);

        return new AstronomyStats(
                aDawn.time(), nDawn.time(), cDawn.time(),
                rise.time(), noon.time(), set.time(),
                cDusk.time(), nDusk.time(), aDusk.time(),
                aDawn.point(), nDawn.point(), cDawn.point(),
                rise.point(), noon.point(), set.point(),
                cDusk.point(), nDusk.point(), aDusk.point()
        );
    }

    private static EventPoint refineEventTime(List<TrackPoint> points, List<TimelineEvent> events, Instant exactEventTime, ZoneId zoneId) {
        if (exactEventTime == null) return new EventPoint(null, null);

        LocalTime eventLocalTime = LocalTime.ofInstant(exactEventTime, zoneId);
        boolean isObserved = false;

        // LOGIKA: Sprawdzamy czy zjawisko łapie się w JAKIKOLWIEK okres aktywności (RUCH, POSTÓJ, PRZERWA)
        if (events != null && !events.isEmpty()) {
            for (TimelineEvent ev : events) {

                // ALTERNATYWA: Jeśli w tym czasie nie było sygnału GPS, udajemy, że nas tam nie było.
                if ("BRAK DANYCH".equals(ev.type())) {
                    continue; // Przeskakujemy ten okres i szukamy dalej
                }

                Instant expandedStart = ev.start().minusSeconds(TOLERANCE_SECONDS);
                Instant expandedEnd = ev.end().plusSeconds(TOLERANCE_SECONDS);

                if (!exactEventTime.isBefore(expandedStart) && !exactEventTime.isAfter(expandedEnd)) {
                    isObserved = true;
                    break;
                }
            }
        } else {
            // Fallback (awaryjnie, gdyby MovementAnalyzer nie zwrócił zdarzeń)
            Instant firstPointTime = points.get(0).getTime().minusSeconds(TOLERANCE_SECONDS);
            Instant lastPointTime = points.get(points.size() - 1).getTime().plusSeconds(TOLERANCE_SECONDS);
            if (!exactEventTime.isBefore(firstPointTime) && !exactEventTime.isAfter(lastPointTime)) {
                isObserved = true;
            }
        }

        // Jeśli czas nie wpadł w żaden z wyliczonych okresów trasy -> zwracamy brak punktu
        if (!isObserved) {
            return new EventPoint(eventLocalTime, null);
        }

        // Zjawisko wystąpiło w trakcie wycieczki - szukamy najbliższego mu punktu z tych dostępnych
        TrackPoint closestPoint = points.get(0);
        long minDiffMillis = Long.MAX_VALUE;

        for (TrackPoint p : points) {
            long diff = Math.abs(Duration.between(p.getTime(), exactEventTime).toMillis());
            if (diff < minDiffMillis) {
                minDiffMillis = diff;
                closestPoint = p;
            }
        }

        return new EventPoint(eventLocalTime, closestPoint);
    }

    private static Instant getRawEventTime(Instant time, double lat, double lng, ZoneId zoneId, Twilight twilight, boolean isRise) {
        LocalDate targetDate = time.atZone(zoneId).toLocalDate();
        SunTimes times = SunTimes.compute()
                .on(targetDate.atStartOfDay(zoneId))
                .at(lat, lng)
                .twilight(twilight)
                .execute();

        Instant result = isRise ? (times.getRise() != null ? times.getRise().toInstant() : null)
                : (times.getSet() != null ? times.getSet().toInstant() : null);

        // FIX "BIAŁYCH NOCY": Upewniamy się, że zjawisko faktycznie występuje tego dnia (lub blisko północy).
        // Jeśli słońce nie chowa się za horyzont latem, biblioteka wyrzuca datę np. za 2 miesiące. Odrzucamy ją!
        if (result != null) {
            LocalDate resultDate = result.atZone(zoneId).toLocalDate();
            long daysBetween = Math.abs(ChronoUnit.DAYS.between(targetDate, resultDate));
            if (daysBetween > 1) {
                return null; // Zjawisko w ogóle nie występuje o tej porze roku
            }
        }

        return result;
    }

    private static Instant getNoonTime(Instant time, double lat, double lng, ZoneId zoneId) {
        SunTimes times = SunTimes.compute()
                .on(time.atZone(zoneId).toLocalDate().atStartOfDay(zoneId)) // PRZYWRÓCONY FIX
                .at(lat, lng)
                .execute();
        return times.getNoon() != null ? times.getNoon().toInstant() : null;
    }
}