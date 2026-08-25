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
import java.util.Objects;

public class AstronomyAnalyzer {

    public record EventPoint(Instant time, TrackPoint point) {}
    private static final long TOLERANCE_SECONDS = 30;

    public static AstronomyStats calculateAstronomy(List<TrackPoint> pointsOfDay, List<TrackPoint> allPoints, List<TimelineEvent> eventsOfDay, ZoneId zoneId) {
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

        List<TrackPoint> searchList = (allPoints != null && !allPoints.isEmpty()) ? allPoints : pointsOfDay;

        EventPoint aDawn = resolveSunEvent(baseTime, lat, lng, searchList, eventsOfDay, zoneId, Twilight.ASTRONOMICAL, true);
        EventPoint nDawn = resolveSunEvent(baseTime, lat, lng, searchList, eventsOfDay, zoneId, Twilight.NAUTICAL, true);
        EventPoint cDawn = resolveSunEvent(baseTime, lat, lng, searchList, eventsOfDay, zoneId, Twilight.CIVIL, true);
        EventPoint rise  = resolveSunEvent(baseTime, lat, lng, searchList, eventsOfDay, zoneId, Twilight.VISUAL, true);

        EventPoint noon  = resolveNoonEvent(baseTime, lat, lng, searchList, eventsOfDay, zoneId);

        EventPoint set   = resolveSunEvent(baseTime, lat, lng, searchList, eventsOfDay, zoneId, Twilight.VISUAL, false);
        EventPoint cDusk = resolveSunEvent(baseTime, lat, lng, searchList, eventsOfDay, zoneId, Twilight.CIVIL, false);
        EventPoint nDusk = resolveSunEvent(baseTime, lat, lng, searchList, eventsOfDay, zoneId, Twilight.NAUTICAL, false);
        EventPoint aDusk = resolveSunEvent(baseTime, lat, lng, searchList, eventsOfDay, zoneId, Twilight.ASTRONOMICAL, false);

        EventPoint moonRiseEp = resolveMoonEvent(baseTime, lat, lng, searchList, eventsOfDay, zoneId, true);
        EventPoint moonSetEp  = resolveMoonEvent(baseTime, lat, lng, searchList, eventsOfDay, zoneId, false);

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


    private static EventPoint resolveSunEvent(Instant baseTime, double baseLat, double baseLng, List<TrackPoint> points, List<TimelineEvent> events, ZoneId zoneId, Twilight twilight, boolean isRise) {
        Instant approxTime = calculateSunEventTime(baseTime, baseLat, baseLng, zoneId, twilight, isRise);
        if (approxTime == null)
            return new EventPoint(null, null);

        EventPoint approxEp = resolveObservedPoint(points, events, approxTime);
        if (approxEp.point() == null)
            return new EventPoint(approxTime, null);

        Instant exactTime = calculateSunEventTime(baseTime, approxEp.point().getLatitude(), approxEp.point().getLongitude(), zoneId, twilight, isRise);
        if (exactTime == null)
            exactTime = approxTime;

        return resolveObservedPoint(points, events, exactTime);
    }

    private static EventPoint resolveMoonEvent(Instant baseTime, double baseLat, double baseLng, List<TrackPoint> points, List<TimelineEvent> events, ZoneId zoneId, boolean isRise) {
        Instant approxTime = calculateMoonEventTime(baseTime, baseLat, baseLng, zoneId, isRise);
        if (approxTime == null)
            return new EventPoint(null, null);

        EventPoint approxEp = resolveObservedPoint(points, events, approxTime);
        if (approxEp.point() == null)
            return new EventPoint(approxTime, null);

        Instant exactTime = calculateMoonEventTime(baseTime, approxEp.point().getLatitude(), approxEp.point().getLongitude(), zoneId, isRise);
        if (exactTime == null)
            exactTime = approxTime;

        return resolveObservedPoint(points, events, exactTime);
    }

    private static EventPoint resolveNoonEvent(Instant baseTime, double baseLat, double baseLng, List<TrackPoint> points, List<TimelineEvent> events, ZoneId zoneId) {
        Instant approxTime = calculateNoonTime(baseTime, baseLat, baseLng, zoneId);
        if (approxTime == null)
            return new EventPoint(null, null);

        EventPoint approxEp = resolveObservedPoint(points, events, approxTime);
        if (approxEp.point() == null)
            return new EventPoint(approxTime, null);

        Instant exactTime = calculateNoonTime(baseTime, approxEp.point().getLatitude(), approxEp.point().getLongitude(), zoneId);
        if (exactTime == null)
            exactTime = approxTime;

        return resolveObservedPoint(points, events, exactTime);
    }


    private static EventPoint resolveObservedPoint(List<TrackPoint> points, List<TimelineEvent> events, Instant exactEventTime){
        if (exactEventTime == null || points == null || points.isEmpty())
            return new EventPoint(null, null);

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
            return new EventPoint(exactEventTime, null);
        }

        TrackPoint before = null;
        TrackPoint after = null;

        for (int i = 0; i < points.size() - 1; i++) {
            TrackPoint p1 = points.get(i);
            TrackPoint p2 = points.get(i + 1);

            if (!exactEventTime.isBefore(p1.getTime()) && !exactEventTime.isAfter(p2.getTime())) {
                before = p1;
                after = p2;
                break;
            }
        }

        if (before != null && after != null) {
            if (!Objects.equals(before.getSegmentId(), after.getSegmentId())) {
                double gapDist = jasinski.pawel.weather_visualization.utils.GeoUtils.calculateDistance(
                        before.getLatitude(), before.getLongitude(),
                        after.getLatitude(), after.getLongitude()
                );
                long gapSec = Math.abs(java.time.Duration.between(before.getTime(), after.getTime()).getSeconds());

                if (gapDist >= 2000.0 && gapSec >= 2700) {
                    return new EventPoint(exactEventTime, null);
                }
            }

            long totalDiff = after.getTime().toEpochMilli() - before.getTime().toEpochMilli();
            long targetDiff = exactEventTime.toEpochMilli() - before.getTime().toEpochMilli();
            double ratio = totalDiff == 0 ? 0 : (double) targetDiff / totalDiff;
            double interpLat = before.getLatitude() + (after.getLatitude() - before.getLatitude()) * ratio;
            double interpLng = before.getLongitude() + (after.getLongitude() - before.getLongitude()) * ratio;

            TrackPoint exactPoint = new TrackPoint();
            exactPoint.setLatitude(interpLat);
            exactPoint.setLongitude(interpLng);
            exactPoint.setTime(exactEventTime);
            exactPoint.setSegmentId(before.getSegmentId());
            exactPoint.setWeather(before.getWeather());

            return new EventPoint(exactEventTime, exactPoint);
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

        return new EventPoint(exactEventTime, closestPoint);
    }

    private static Instant calculateSunEventTime(Instant time, double lat, double lng, ZoneId zoneId, Twilight twilight, boolean isRise) {
        LocalDate targetDate = time.atZone(zoneId).toLocalDate();
        ZonedDateTime searchStart = targetDate.atStartOfDay(zoneId);

        SunTimes times = SunTimes.compute()
                .on(searchStart)
                .at(lat, lng)
                .twilight(twilight)
                .execute();

        Instant result = isRise ? (times.getRise() != null ? times.getRise().toInstant() : null)
                : (times.getSet() != null ? times.getSet().toInstant() : null);

        if (result != null && !isRise) {
            SunTimes noonTimes = SunTimes.compute().on(searchStart).at(lat, lng).execute();
            ZonedDateTime noonZdt = noonTimes.getNoon();
            if (noonZdt != null && result.isBefore(noonZdt.toInstant())) {
                times = SunTimes.compute()
                        .on(noonZdt)
                        .at(lat, lng)
                        .twilight(twilight)
                        .execute();
                result = times.getSet() != null ? times.getSet().toInstant() : null;
            }
        }

        if (result != null) {
            LocalDate resultDate = result.atZone(zoneId).toLocalDate();
            long daysBetween = Math.abs(ChronoUnit.DAYS.between(targetDate, resultDate));
            if (daysBetween > 1) {
                return null;
            }
        }
        return result;
    }

    private static Instant calculateMoonEventTime(Instant time, double lat, double lng, ZoneId zoneId, boolean isRise) {
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

    private static Instant calculateNoonTime(Instant time, double lat, double lng, ZoneId zoneId) {
        SunTimes times = SunTimes.compute()
                .on(time.atZone(zoneId).toLocalDate().atStartOfDay(zoneId))
                .at(lat, lng)
                .execute();
        return times.getNoon() != null ? times.getNoon().toInstant() : null;
    }
}