package jasinski.pawel.weather_visualization.utils;

import jasinski.pawel.weather_visualization.dto.AstronomyStats;
import jasinski.pawel.weather_visualization.dto.TimelineEvent;
import jasinski.pawel.weather_visualization.entity.TrackPoint;
import org.shredzone.commons.suncalc.MoonTimes;
import org.shredzone.commons.suncalc.SunTimes;
import org.shredzone.commons.suncalc.SunTimes.Twilight;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class AstronomyAnalyzer {

    public record EventPoint(LocalTime time, TrackPoint point) {}
    private static final long TOLERANCE_SECONDS = 30;

    public static AstronomyStats calculateSun(List<TrackPoint> pointsOfDay, List<TimelineEvent> eventsOfDay, ZoneId zoneId) {
        if (pointsOfDay == null || pointsOfDay.isEmpty()) {
            return new AstronomyStats(
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null,null
            );
        }

        TrackPoint middlePoint = pointsOfDay.get(pointsOfDay.size() / 2);
        double lat = middlePoint.getLatitude();
        double lng = middlePoint.getLongitude();
        Instant baseTime = middlePoint.getTime();

        //Liczenie czasu zjawisk
        Instant exactAstroDawn = getRawEventTime(baseTime, lat, lng, zoneId, Twilight.ASTRONOMICAL, true);
        Instant exactNautDawn  = getRawEventTime(baseTime, lat, lng, zoneId, Twilight.NAUTICAL, true);
        Instant exactCivilDawn = getRawEventTime(baseTime, lat, lng, zoneId, Twilight.CIVIL, true);
        Instant exactSunrise   = getRawEventTime(baseTime, lat, lng, zoneId, Twilight.VISUAL, true);
        Instant exactNoon      = getNoonTime(baseTime, lat, lng, zoneId);
        Instant exactSunset    = getRawEventTime(baseTime, lat, lng, zoneId, Twilight.VISUAL, false);
        Instant exactCivilDusk = getRawEventTime(baseTime, lat, lng, zoneId, Twilight.CIVIL, false);
        Instant exactNautDusk  = getRawEventTime(baseTime, lat, lng, zoneId, Twilight.NAUTICAL, false);
        Instant exactAstroDusk = getRawEventTime(baseTime, lat, lng, zoneId, Twilight.ASTRONOMICAL, false);
        Instant exactMoonRise = getRawMoonEventTime(baseTime, lat, lng, zoneId, true);
        Instant exactMoonSet  = getRawMoonEventTime(baseTime, lat, lng, zoneId, false);

        //Znajdywanie punktów zaobserwowanych zjawisk
        EventPoint aDawn = refineEventTime(pointsOfDay, eventsOfDay, exactAstroDawn, zoneId);
        EventPoint nDawn = refineEventTime(pointsOfDay, eventsOfDay, exactNautDawn, zoneId);
        EventPoint cDawn = refineEventTime(pointsOfDay, eventsOfDay, exactCivilDawn, zoneId);
        EventPoint rise  = refineEventTime(pointsOfDay, eventsOfDay, exactSunrise, zoneId);
        EventPoint noon  = refineEventTime(pointsOfDay, eventsOfDay, exactNoon, zoneId);
        EventPoint set   = refineEventTime(pointsOfDay, eventsOfDay, exactSunset, zoneId);
        EventPoint cDusk = refineEventTime(pointsOfDay, eventsOfDay, exactCivilDusk, zoneId);
        EventPoint nDusk = refineEventTime(pointsOfDay, eventsOfDay, exactNautDusk, zoneId);
        EventPoint aDusk = refineEventTime(pointsOfDay, eventsOfDay, exactAstroDusk, zoneId);
        EventPoint moonRiseEp = refineEventTime(pointsOfDay, eventsOfDay, exactMoonRise, zoneId);
        EventPoint moonSetEp  = refineEventTime(pointsOfDay, eventsOfDay, exactMoonSet, zoneId);


        return new AstronomyStats(
                aDawn.time(), nDawn.time(), cDawn.time(),
                rise.time(), noon.time(), set.time(),
                cDusk.time(), nDusk.time(), aDusk.time(),
                moonRiseEp.time(), moonSetEp.time(),

                aDawn.point(), nDawn.point(), cDawn.point(),
                rise.point(), noon.point(), set.point(),
                cDusk.point(), nDusk.point(), aDusk.point(),
                moonRiseEp.point(), moonSetEp.point()
        );
    }

    private static EventPoint refineEventTime(List<TrackPoint> points, List<TimelineEvent> events, Instant exactEventTime, ZoneId zoneId) {
        if (exactEventTime == null) return new EventPoint(null, null);

        LocalTime eventLocalTime = LocalTime.ofInstant(exactEventTime, zoneId);
        boolean isObserved = false;


        if (events != null && !events.isEmpty()) {
            for (TimelineEvent ev : events) {

                if ("BRAK DANYCH".equals(ev.type())) {
                    continue;
                }

                Instant expandedStart = ev.start().minusSeconds(TOLERANCE_SECONDS);
                Instant expandedEnd = ev.end().plusSeconds(TOLERANCE_SECONDS);

                if (!exactEventTime.isBefore(expandedStart) && !exactEventTime.isAfter(expandedEnd)) {
                    isObserved = true;
                    break;
                }
            }
        } else {
            Instant firstPointTime = points.get(0).getTime().minusSeconds(TOLERANCE_SECONDS);
            Instant lastPointTime = points.get(points.size() - 1).getTime().plusSeconds(TOLERANCE_SECONDS);
            if (!exactEventTime.isBefore(firstPointTime) && !exactEventTime.isAfter(lastPointTime)) {
                isObserved = true;
            }
        }

        if (!isObserved) {
            return new EventPoint(eventLocalTime, null);
        }

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


        if (result != null) {
            LocalDate resultDate = result.atZone(zoneId).toLocalDate();
            long daysBetween = Math.abs(ChronoUnit.DAYS.between(targetDate, resultDate));
            if (daysBetween > 1) {
                return null;
            }
        }

        return result;
    }

    private static Instant getRawMoonEventTime(Instant time, double lat, double lng, ZoneId zoneId, boolean isRise) {
        LocalDate targetDate = time.atZone(zoneId).toLocalDate();
        MoonTimes times = MoonTimes.compute()
                .on(targetDate.atStartOfDay(zoneId))
                .at(lat, lng)
                .execute();

        Instant result = isRise ? (times.getRise() != null ? times.getRise().toInstant() : null)
                : (times.getSet() != null ? times.getSet().toInstant() : null);


        if (result != null) {
            LocalDate resultDate = result.atZone(zoneId).toLocalDate();
            long daysBetween = Math.abs(ChronoUnit.DAYS.between(targetDate, resultDate));
            if (daysBetween > 0) {
                return null;
            }
        }

        return result;
    }

    private static Instant getNoonTime(Instant time, double lat, double lng, ZoneId zoneId) {
        SunTimes times = SunTimes.compute()
                .on(time.atZone(zoneId).toLocalDate().atStartOfDay(zoneId))
                .at(lat, lng)
                .execute();
        return times.getNoon() != null ? times.getNoon().toInstant() : null;
    }
}