package jasinski.pawel.weather_visualization.utils;

import jasinski.pawel.weather_visualization.dto.AstronomyStats;
import jasinski.pawel.weather_visualization.dto.TimelineEvent;
import jasinski.pawel.weather_visualization.entity.TrackPoint;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AstronomyAnalyzerTest {

    private final GeometryFactory factory = new GeometryFactory();
    private final ZoneId warsawZone = ZoneId.of("Europe/Warsaw");


    @Test
    void calculateSun_shouldReturnEmptyStats_whenPointsAreNullOrEmpty() {
        AstronomyStats statsEmpty = AstronomyAnalyzer.calculateSun(List.of(), List.of(), null, warsawZone);
        AstronomyStats statsNull = AstronomyAnalyzer.calculateSun(null, null, null, warsawZone);

        assertThat(statsEmpty.sunrise()).isNull();
        assertThat(statsEmpty.sunrisePt()).isNull();

        assertThat(statsNull.sunset()).isNull();
        assertThat(statsNull.sunsetPt()).isNull();
    }

    @Test
    void calculateSun_shouldCalculateEventsAndAssignPoints_whenObserved() {


        TrackPoint morningPoint = createPoint(52.2297, 21.0122, "2023-09-21T04:00:00Z");
        TrackPoint noonPoint    = createPoint(52.2297, 21.0122, "2023-09-21T12:00:00Z");
        TrackPoint eveningPoint = createPoint(52.2297, 21.0122, "2023-09-21T18:00:00Z");

        morningPoint.setSegmentId(1);
        noonPoint.setSegmentId(1);
        eveningPoint.setSegmentId(1);

        List<TrackPoint> pointsOfDay = List.of(morningPoint, noonPoint, eveningPoint);

        AstronomyStats stats = AstronomyAnalyzer.calculateSun(pointsOfDay, pointsOfDay, null, warsawZone);

        assertThat(stats).isNotNull();

        assertThat(stats.sunrisePt()).isNotNull();
        assertThat(stats.sunrisePt().getTime()).isEqualTo(stats.sunrise());

        assertThat(stats.noonPt()).isNotNull();
        assertThat(stats.noonPt().getTime()).isEqualTo(stats.solarNoon());

        assertThat(stats.sunsetPt()).isNotNull();
        assertThat(stats.sunsetPt().getTime()).isEqualTo(stats.sunset());
    }


    @Test
    void calculateSun_shouldReturnTimeButNullPoint_whenEventOutsideObservationWindow() {

        TrackPoint walkStart = createPoint(52.2297, 21.0122, "2023-09-21T09:00:00Z");
        TrackPoint walkEnd = createPoint(52.2297, 21.0122, "2023-09-21T11:00:00Z");

        List<TrackPoint> shortWalk = List.of(walkStart, walkEnd);

        AstronomyStats stats = AstronomyAnalyzer.calculateSun(shortWalk, null, null, warsawZone);

        assertThat(stats.sunrise()).isNotNull();

        assertThat(stats.sunrisePt()).isNull();

        assertThat(stats.solarNoon()).isNotNull();
        assertThat(stats.noonPt()).isNotNull();
    }


    @Test
    void calculateSun_shouldHandlePolarDayGracefully() {

        TrackPoint polarPoint1 = createPoint(78.2232, 15.6267, "2023-06-21T10:00:00Z");
        TrackPoint polarPoint2 = createPoint(78.2232, 15.6267, "2023-06-21T14:00:00Z");

        AstronomyStats stats = AstronomyAnalyzer.calculateSun(List.of(polarPoint1, polarPoint2), null, null, warsawZone);

        assertThat(stats.sunrise()).isNull();
        assertThat(stats.sunrisePt()).isNull();
        assertThat(stats.sunset()).isNull();
        assertThat(stats.sunsetPt()).isNull();

        assertThat(stats.solarNoon()).isNotNull();
    }

    @Test
    void calculateSun_shouldAssignPoint_whenWithinToleranceWindow() {
        TrackPoint point = createPoint(52.2297, 21.0122, "2023-09-21T04:25:00Z");

        TimelineEvent moveEvent = new TimelineEvent(
                "RUCH",
                Instant.parse("2023-09-21T04:10:00Z"),
                Instant.parse("2023-09-21T04:30:00Z"),
                52.2, 21.0, "Warszawa"
        );

        AstronomyStats stats = AstronomyAnalyzer.calculateSun(List.of(point), null, List.of(moveEvent), warsawZone);

        assertThat(stats.sunrisePt()).isNotNull();
        assertThat(stats.sunrisePt()).isSameAs(point);
    }

    private TrackPoint createPoint(double lat, double lon, String instantStr) {
        TrackPoint tp = new TrackPoint();
        tp.setLongitude(lon);
        tp.setLatitude(lat);
        tp.setTime(Instant.parse(instantStr));
        return tp;
    }
}
