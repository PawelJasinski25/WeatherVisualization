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
    void analyzeTripTimeline_shouldDetectContinuousMovement() {

        List<TrackPoint> points = new ArrayList<>();
        points.add(createPoint(52.0000, 21.0000, "2023-05-10T10:00:00Z"));
        points.add(createPoint(52.0005, 21.0000, "2023-05-10T10:00:30Z"));
        points.add(createPoint(52.0010, 21.0000, "2023-05-10T10:01:00Z"));
        points.add(createPoint(52.0015, 21.0000, "2023-05-10T10:01:30Z"));
        points.add(createPoint(52.0020, 21.0000, "2023-05-10T10:02:00Z"));

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
    void analyzeTripTimeline_shouldDetectContinuousStop() {

        List<TrackPoint> points = new ArrayList<>();
        points.add(createPoint(52.0, 21.0, "2023-05-10T10:00:00Z"));
        points.add(createPoint(52.0, 21.0, "2023-05-10T10:00:30Z"));
        points.add(createPoint(52.0, 21.0, "2023-05-10T10:01:00Z"));
        points.add(createPoint(52.0, 21.0, "2023-05-10T10:01:30Z"));
        points.add(createPoint(52.0, 21.0, "2023-05-10T10:02:00Z"));

        Map<LocalDate, DayData> result = MovementAnalyzer.analyzeTripTimeline(points, zoneId);

        DayData dayData = result.get(LocalDate.of(2023, 5, 10));
        assertThat(dayData.events).hasSize(1);
        assertThat(dayData.events.get(0).type()).isEqualTo("POSTÓJ");
    }


    @Test
    void analyzeTripTimeline_shouldDebounceMicroStop_andMergeIntoMovement() {
        List<TrackPoint> points = new ArrayList<>();

        //Ruch przez 90s
        points.add(createPoint(52.000, 21.0, "2023-05-10T10:00:00Z"));
        points.add(createPoint(52.005, 21.0, "2023-05-10T10:00:45Z"));
        points.add(createPoint(52.010, 21.0, "2023-05-10T10:01:30Z"));

        //Postój przez 30s
        points.add(createPoint(52.010, 21.0, "2023-05-10T10:02:00Z"));

        //Ruch przez 90s
        points.add(createPoint(52.015, 21.0, "2023-05-10T10:02:45Z"));
        points.add(createPoint(52.020, 21.0, "2023-05-10T10:03:30Z"));


        Map<LocalDate, DayData> result = MovementAnalyzer.analyzeTripTimeline(points, zoneId);


        DayData dayData = result.get(LocalDate.of(2023, 5, 10));
        assertThat(dayData.events).hasSize(1);
        assertThat(dayData.events.get(0).type()).isEqualTo("RUCH");
        assertThat(dayData.events.get(0).durationSeconds()).isEqualTo(210);
    }


    @Test
    void analyzeTripTimeline_shouldDetectGap_whenTimeAndDistanceAreHuge() {

        List<TrackPoint> points = new ArrayList<>();
        points.add(createPoint(52.0, 21.0, "2023-05-10T10:00:00Z"));

        //Przeskok o ponad 10 km i dokładnie 2 godziny w przód
        points.add(createPoint(52.1, 21.0, "2023-05-10T12:00:00Z"));

        Map<LocalDate, DayData> result = MovementAnalyzer.analyzeTripTimeline(points, zoneId);

        DayData dayData = result.get(LocalDate.of(2023, 5, 10));
        assertThat(dayData.events).hasSize(1);
        assertThat(dayData.events.get(0).type()).isEqualTo("BRAK DANYCH");
    }



    @Test
    void analyzeTripTimeline_shouldSplitEventsAtMidnight() {

        List<TrackPoint> points = new ArrayList<>();
        points.add(createPoint(52.000, 21.0, "2023-05-10T23:40:00Z"));
        points.add(createPoint(52.010, 21.0, "2023-05-10T23:55:00Z"));
        points.add(createPoint(52.020, 21.0, "2023-05-11T00:20:00Z"));

        Map<LocalDate, DayData> result = MovementAnalyzer.analyzeTripTimeline(points, zoneId);


        assertThat(result).hasSize(2);

        DayData day1 = result.get(LocalDate.of(2023, 5, 10));
        assertThat(day1.events).hasSize(1);
        assertThat(day1.events.get(0).end()).isEqualTo(Instant.parse("2023-05-11T00:00:00Z"));

        DayData day2 = result.get(LocalDate.of(2023, 5, 11));
        assertThat(day2.events).hasSize(1);
        assertThat(day2.events.get(0).start()).isEqualTo(Instant.parse("2023-05-11T00:00:00Z"));
    }

    private TrackPoint createPoint(double lat, double lng, String instantStr) {
        TrackPoint tp = new TrackPoint();
        Point location = factory.createPoint(new Coordinate(lng, lat));
        tp.setLocation(location);
        tp.setTime(Instant.parse(instantStr));
        return tp;
    }
}
