package jasinski.pawel.weather_visualization.utils;

import jasinski.pawel.weather_visualization.dto.EnrichedSegment;
import jasinski.pawel.weather_visualization.dto.SpeedStats;
import jasinski.pawel.weather_visualization.entity.TrackPoint;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpeedAnalyzerTest {


    @Test
    void calculateSpeed_shouldReturnEmptyStats_whenSegmentsAreNullOrEmpty() {
        SpeedStats nullStats = SpeedAnalyzer.calculateSpeed(null);
        SpeedStats emptyStats = SpeedAnalyzer.calculateSpeed(List.of());

        assertThat(nullStats.maxSpeed()).isNull();
        assertThat(nullStats.avgSpeed()).isNull();
        assertThat(nullStats.distanceKm()).isEqualTo(0.0);

        assertThat(emptyStats.maxSpeed()).isNull();
        assertThat(emptyStats.avgSpeed()).isNull();
        assertThat(emptyStats.distanceKm()).isEqualTo(0.0);
    }


    @Test
    void calculateSpeed_shouldCalculateAveragesProperly_whenMoving() {
        EnrichedSegment seg1 = createSegment(100.0, 10.0, 36.0, null, true);
        EnrichedSegment seg2 = createSegment(200.0, 20.0, 36.0, null, true);

        SpeedStats stats = SpeedAnalyzer.calculateSpeed(List.of(seg1, seg2));

        assertThat(stats.distanceKm()).isEqualTo(0.30);
        assertThat(stats.avgSpeed()).isEqualTo(36.00);
        assertThat(stats.maxSpeed()).isEqualTo(36.00);
    }


    @Test
    void calculateSpeed_shouldIgnoreStationarySegments_whenCalculatingAverageSpeed() {
        EnrichedSegment moving = createSegment(100.0, 10.0, 36.0, null, true);
        EnrichedSegment stopped = createSegment(10.0, 60.0, 0.6, null, false);

        SpeedStats stats = SpeedAnalyzer.calculateSpeed(List.of(moving, stopped));

        assertThat(stats.avgSpeed()).isEqualTo(36.0);
        assertThat(stats.distanceKm()).isEqualTo(0.11);
    }

    @Test
    void calculateSpeed_shouldIgnoreNullDistance_whenDataGapOccurs() {
        EnrichedSegment moving = createSegment(100.0, 10.0, 36.0, null, true);
        EnrichedSegment gap = createSegment(null, 5000.0, null, null, false);

        SpeedStats stats = SpeedAnalyzer.calculateSpeed(List.of(moving, gap));

        assertThat(stats.avgSpeed()).isEqualTo(36.0);
        assertThat(stats.maxSpeed()).isEqualTo(36.0);
        assertThat(stats.distanceKm()).isEqualTo(0.10);
    }


    @Test
    void isAnomaly_shouldDetectSpike_whenAccelerationExceedsThreshold() {
        List<Double> speeds = List.of(20.0, 50.0, 20.0);
        List<Double> durations = List.of(1.0, 1.0, 1.0);

        boolean isAnomaly = SpeedAnalyzer.isAnomaly(speeds, durations, 1);

        assertThat(isAnomaly).isTrue();
    }

    @Test
    void removeSpeedAnomalies_shouldReplaceSpeed_whenAnomalyIsDetected() {
        TrackPoint p1 = createPointWithSpeed("2023-01-01T10:00:00Z", 20.0);
        TrackPoint p2 = createPointWithSpeed("2023-01-01T10:00:05Z", 22.0);
        TrackPoint p3 = createPointWithSpeed("2023-01-01T10:00:10Z", 120.0);
        TrackPoint p4 = createPointWithSpeed("2023-01-01T10:00:15Z", 21.0);

        List<TrackPoint> cleaned = SpeedAnalyzer.removeSpeedAnomalies(List.of(p1, p2, p3, p4));

        assertThat(cleaned.get(2).getSpeed()).isEqualTo(22.0);
        assertThat(cleaned.get(3).getSpeed()).isEqualTo(21.0);
    }


    @Test
    void isAnomaly_shouldReturnFalse_whenInsideToleranceWindow() {
        List<Double> speeds = Arrays.asList(
                5.0, 5.0, 5.0,
                50.0, 52.0,
                55.0,
                51.0, 50.0
        );

        List<Double> durations = Arrays.asList(10.0, 10.0, 50.0, 10.0, 10.0, 10.0, 10.0, 10.0);
        boolean isAnomaly = SpeedAnalyzer.isAnomaly(speeds, durations, 5);

        assertThat(isAnomaly).isFalse();
    }

    private EnrichedSegment createSegment(Double distance, double duration, Double rawSpeed, Double p2Speed, boolean isMoving) {
        TrackPoint p1 = new TrackPoint();
        TrackPoint p2 = new TrackPoint();
        p2.setSpeed(p2Speed);

        return new EnrichedSegment(p1, p2, distance, duration, rawSpeed, isMoving);
    }

    private TrackPoint createPointWithSpeed(String time, double speed) {
        TrackPoint pt = new TrackPoint();
        pt.setTime(Instant.parse(time));
        pt.setLatitude(52.0);
        pt.setLongitude(21.0);
        pt.setSpeed(speed);
        return pt;
    }
}