package jasinski.pawel.weather_visualization.utils;

import jasinski.pawel.weather_visualization.dto.AstronomyStats;
import jasinski.pawel.weather_visualization.entity.TrackPoint;
import org.shredzone.commons.suncalc.SunTimes;
import org.shredzone.commons.suncalc.SunTimes.Twilight;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AstronomyAnalyzer {

    public static AstronomyStats calculateSun(List<TrackPoint> pointsOfDay, ZoneId zoneId) {
        if (pointsOfDay == null || pointsOfDay.isEmpty()) {
            return new AstronomyStats("--:--", "--:--", "--:--", "--:--", "--:--", "--:--", "--:--", "--:--");
        }

        // Szybkie oszacowanie czasu zjawisk (na podstawie punktu ze środka)
        TrackPoint middlePoint = pointsOfDay.get(pointsOfDay.size() / 2);
        double approxLat = middlePoint.getLocation().getY();
        double approxLng = middlePoint.getLocation().getX();
        Instant baseTime = middlePoint.getTime();

        Instant approxAstroDawn = getRawEventTime(baseTime, approxLat, approxLng, zoneId, Twilight.ASTRONOMICAL, true);
        Instant approxNautDawn  = getRawEventTime(baseTime, approxLat, approxLng, zoneId, Twilight.NAUTICAL, true);
        Instant approxCivilDawn = getRawEventTime(baseTime, approxLat, approxLng, zoneId, Twilight.CIVIL, true);
        Instant approxSunrise   = getRawEventTime(baseTime, approxLat, approxLng, zoneId, Twilight.VISUAL, true);

        Instant approxSunset    = getRawEventTime(baseTime, approxLat, approxLng, zoneId, Twilight.VISUAL, false);
        Instant approxCivilDusk = getRawEventTime(baseTime, approxLat, approxLng, zoneId, Twilight.CIVIL, false);
        Instant approxNautDusk  = getRawEventTime(baseTime, approxLat, approxLng, zoneId, Twilight.NAUTICAL, false);
        Instant approxAstroDusk = getRawEventTime(baseTime, approxLat, approxLng, zoneId, Twilight.ASTRONOMICAL, false);

        // Dostosowanie obliczeń
        String exactAstroDawn = refineEventTime(pointsOfDay, approxAstroDawn, zoneId, Twilight.ASTRONOMICAL, true);
        String exactNautDawn  = refineEventTime(pointsOfDay, approxNautDawn, zoneId, Twilight.NAUTICAL, true);
        String exactCivilDawn = refineEventTime(pointsOfDay, approxCivilDawn, zoneId, Twilight.CIVIL, true);
        String exactSunrise   = refineEventTime(pointsOfDay, approxSunrise, zoneId, Twilight.VISUAL, true);

        String exactSunset    = refineEventTime(pointsOfDay, approxSunset, zoneId, Twilight.VISUAL, false);
        String exactCivilDusk = refineEventTime(pointsOfDay, approxCivilDusk, zoneId, Twilight.CIVIL, false);
        String exactNautDusk  = refineEventTime(pointsOfDay, approxNautDusk, zoneId, Twilight.NAUTICAL, false);
        String exactAstroDusk = refineEventTime(pointsOfDay, approxAstroDusk, zoneId, Twilight.ASTRONOMICAL, false);

        return new AstronomyStats(exactAstroDawn, exactNautDawn, exactCivilDawn, exactSunrise, exactSunset, exactCivilDusk, exactNautDusk, exactAstroDusk);
    }

    private static Instant getRawEventTime(Instant time, double lat, double lng, ZoneId zoneId, Twilight twilight, boolean isRise) {
        SunTimes times = SunTimes.compute().on(time).at(lat, lng).timezone(zoneId).twilight(twilight).execute();
        if (isRise) {
            if (times.getRise() != null) {
                return times.getRise().toInstant();
            }
            else {
                return null;
            }
        }

        else {
            if (times.getSet() != null) {
                return times.getSet().toInstant();
            }
            else {
                return null;
            }
        }
    }

    private static String refineEventTime(List<TrackPoint> points, Instant approxEventTime, ZoneId zoneId, Twilight twilight, boolean isRise) {
        if (approxEventTime == null) return "--:--";

        // Szukamy z trasy punktu GPS, w którym byliśmy fizycznie najbliżej czasu zjawiska
        TrackPoint closestPoint = points.get(0);
        long minDiffMillis = Long.MAX_VALUE;

        for (TrackPoint p : points) {
            long diff = Math.abs(Duration.between(p.getTime(), approxEventTime).toMillis());
            if (diff < minDiffMillis) {
                minDiffMillis = diff;
                closestPoint = p;
            }
        }

        Instant exactEventTime = getRawEventTime(
                closestPoint.getTime(),
                closestPoint.getLocation().getY(),
                closestPoint.getLocation().getX(),
                zoneId, twilight, isRise);

        if (exactEventTime == null) return "--:--";
        return DateTimeFormatter.ofPattern("HH:mm:ss").withZone(zoneId).format(exactEventTime);
    }
}