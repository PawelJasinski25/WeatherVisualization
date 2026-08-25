package jasinski.pawel.weather_visualization.utils;

import jasinski.pawel.weather_visualization.dto.DayData;
import jasinski.pawel.weather_visualization.dto.TimelineEvent;
import jasinski.pawel.weather_visualization.entity.TrackPoint;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MovementAnalyzerTest {

    private final GeometryFactory factory = new GeometryFactory();
    private final ZoneId zoneId = ZoneId.of("UTC");


    @Test
    void analyzeTripTimeline_shouldReturnEmptyMap_whenPointsAreNullOrEmpty() {
        assertThat(MovementAnalyzer.analyzeTripTimeline(null, zoneId)).isEmpty();
        assertThat(MovementAnalyzer.analyzeTripTimeline(List.of(), zoneId)).isEmpty();
    }



    @Test
    void analyzeTripTimeline_shouldDetectContinuousMovement_whenSpeedIsAboveThreshold() {

        List<TrackPoint> points = new ArrayList<>();
        points.add(createPoint(52.0000, 21.0000, "2023-05-10T10:00:00Z", 5.0));
        points.add(createPoint(52.0005, 21.0000, "2023-05-10T10:00:30Z", 5.4));
        points.add(createPoint(52.0010, 21.0000, "2023-05-10T10:01:00Z", 4.6));
        points.add(createPoint(52.0015, 21.0000, "2023-05-10T10:01:30Z", 5.8));
        points.add(createPoint(52.0020, 21.0000, "2023-05-10T10:02:00Z", 6.0));

        Map<LocalDate, DayData> result = MovementAnalyzer.analyzeTripTimeline(points, zoneId);

        assertThat(result).hasSize(1);
        DayData dayData = result.get(LocalDate.of(2023, 5, 10));

        assertThat(dayData.events).hasSize(1);
        TimelineEvent event = dayData.events.get(0);

        assertThat(event.type()).isEqualTo("RUCH");
        assertThat(event.start()).isEqualTo(Instant.parse("2023-05-10T10:00:00Z"));
        assertThat(event.end()).isEqualTo(Instant.parse("2023-05-10T10:02:00Z"));
    }


    @Test
    void analyzeTripTimeline_shouldDetectContinuousStop_whenSpeedIsBelowThreshold() {

        List<TrackPoint> points = new ArrayList<>();
        points.add(createPoint(52.22970, 21.01220, "2023-05-10T10:00:00Z", 0.0));
        points.add(createPoint(52.22971, 21.01221, "2023-05-10T10:00:30Z", 0.2));
        points.add(createPoint(52.22969, 21.01219, "2023-05-10T10:01:00Z", 0.1));
        points.add(createPoint(52.22970, 21.01222, "2023-05-10T10:01:30Z", 0.0));
        points.add(createPoint(52.22972, 21.01220, "2023-05-10T10:02:00Z", 0.1));

        Map<LocalDate, DayData> result = MovementAnalyzer.analyzeTripTimeline(points, zoneId);

        DayData dayData = result.get(LocalDate.of(2023, 5, 10));
        assertThat(dayData.events).hasSize(1);
        assertThat(dayData.events.get(0).type()).isEqualTo("POSTÓJ");
    }


    @Test
    void analyzeTripTimeline_shouldIgnoreStop_whenDurationIsBelowThreshold() {
        List<TrackPoint> points = new ArrayList<>();

        // Ruch przez 90s
        points.add(createPoint(52.22010, 21.00110, "2023-05-10T10:00:00Z", 12.4));
        points.add(createPoint(52.22150, 21.00280, "2023-05-10T10:00:45Z", 13.8));
        points.add(createPoint(52.22290, 21.00420, "2023-05-10T10:01:30Z", 11.9));

        // Spadek prędkości przez 30s
        points.add(createPoint(52.22292, 21.00422, "2023-05-10T10:02:00Z", 0.1));

        // Ruch przez 90s
        points.add(createPoint(52.22440, 21.00590, "2023-05-10T10:02:45Z", 14.1));
        points.add(createPoint(52.22590, 21.00750, "2023-05-10T10:03:30Z", 13.2));

        Map<LocalDate, DayData> result = MovementAnalyzer.analyzeTripTimeline(points, zoneId);

        DayData dayData = result.get(LocalDate.of(2023, 5, 10));
        assertThat(dayData.events).hasSize(1);
        assertThat(dayData.events.getFirst().type()).isEqualTo("RUCH");
        assertThat(dayData.events.getFirst().durationSeconds()).isEqualTo(210);
    }

    @Test
    void analyzeTripTimeline_shouldNotResumeMovement_whenDistanceIsBelowThreshold() {
        List<TrackPoint> points = new ArrayList<>();

        // Postój > 60s)
        points.add(createPoint(52.000, 21.000, "2023-05-10T10:00:00Z", 0.0));
        points.add(createPoint(52.000, 21.000, "2023-05-10T10:01:10Z", 0.0));

        // Rozpoczęcie ruchu ale za mały dystans
        points.add(createPoint(52.0001, 21.0001, "2023-05-10T10:01:20Z", 5.0));

        // Dystans >80m
        points.add(createPoint(52.010, 21.010, "2023-05-10T10:02:00Z", 20.0));

        Map<LocalDate, DayData> result = MovementAnalyzer.analyzeTripTimeline(points, zoneId);

        DayData dayData = result.get(LocalDate.of(2023, 5, 10));

        assertThat(dayData.events).hasSize(2);
        assertThat(dayData.events.get(0).type()).isEqualTo("POSTÓJ");
        assertThat(dayData.events.get(1).type()).isEqualTo("RUCH");
    }


    @Test
    void aanalyzeTripTimeline_shouldDetectGap_whenTimeAndDistanceExceedThresholds() {

        List<TrackPoint> points = new ArrayList<>();
        points.add(createPoint(52.22970, 21.01220, "2023-05-10T10:00:00Z", 15.0));

        points.add(createPoint(52.07410, 21.02890, "2023-05-10T12:00:00Z", 14.2));

        Map<LocalDate, DayData> result = MovementAnalyzer.analyzeTripTimeline(points, zoneId);

        DayData dayData = result.get(LocalDate.of(2023, 5, 10));
        assertThat(dayData.events).hasSize(1);
        assertThat(dayData.events.get(0).type()).isEqualTo("BRAK DANYCH");
    }



    @Test
    void analyzeTripTimeline_shouldSplitEventsAtMidnight_whenPointsSpanAcrossTwoDays() {

        List<TrackPoint> points = new ArrayList<>();
        points.add(createPoint(52.21000, 21.00000, "2023-05-10T23:40:00Z", 18.2));
        points.add(createPoint(52.22500, 21.01500, "2023-05-10T23:55:00Z", 17.5));
        points.add(createPoint(52.25000, 21.04000, "2023-05-11T00:20:00Z", 18.0));

        Map<LocalDate, DayData> result = MovementAnalyzer.analyzeTripTimeline(points, zoneId);


        assertThat(result).hasSize(2);

        DayData day1 = result.get(LocalDate.of(2023, 5, 10));
        assertThat(day1.events).hasSize(1);
        assertThat(day1.events.get(0).end()).isEqualTo(Instant.parse("2023-05-11T00:00:00Z"));

        DayData day2 = result.get(LocalDate.of(2023, 5, 11));
        assertThat(day2.events).hasSize(1);
        assertThat(day2.events.get(0).start()).isEqualTo(Instant.parse("2023-05-11T00:00:00Z"));
    }

    private TrackPoint createPoint(double lat, double lon, String instantStr, double speed) {
        TrackPoint tp = new TrackPoint();
        tp.setLatitude(lat);
        tp.setLongitude(lon);
        tp.setTime(Instant.parse(instantStr));
        tp.setSpeed(speed);
        return tp;
    }
}
