package jasinski.pawel.weather_visualization.utils;

import jasinski.pawel.weather_visualization.dto.EnrichedSegment;
import jasinski.pawel.weather_visualization.dto.SpeedStats;
import jasinski.pawel.weather_visualization.entity.TrackPoint;
import org.junit.jupiter.api.Test;

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
    void calculateSpeed_shouldPreferTrackPointSpeed_overRawSpeed() {
        EnrichedSegment seg = createSegment(100.0, 10.0, 10.0, 15.5, true);

        SpeedStats stats = SpeedAnalyzer.calculateSpeed(List.of(seg));

        assertThat(stats.maxSpeed()).isEqualTo(15.5);
    }


    @Test
    void calculateSpeed_shouldIgnoreStationarySegments_inAverageSpeed_butKeepInTotalDistance() {
        EnrichedSegment moving = createSegment(100.0, 10.0, 36.0, null, true);
        EnrichedSegment stopped = createSegment(10.0, 60.0, 0.6, null, false);

        SpeedStats stats = SpeedAnalyzer.calculateSpeed(List.of(moving, stopped));

        assertThat(stats.avgSpeed()).isEqualTo(36.0);
        assertThat(stats.distanceKm()).isEqualTo(0.11);
    }

    @Test
    void calculateSpeed_shouldSafelyIgnoreNullDistance_DataGapSegments() {
        EnrichedSegment moving = createSegment(100.0, 10.0, 36.0, null, true);
        EnrichedSegment gap = createSegment(null, 5000.0, null, null, false);

        SpeedStats stats = SpeedAnalyzer.calculateSpeed(List.of(moving, gap));

        assertThat(stats.avgSpeed()).isEqualTo(36.0);
        assertThat(stats.maxSpeed()).isEqualTo(36.0);
        assertThat(stats.distanceKm()).isEqualTo(0.10);
    }


    @Test
    void calculateSpeed_shouldFilterSinglePointSpike() {
        EnrichedSegment s1 = createSegment(10.0, 2.0, 20.0, null, true);
        EnrichedSegment spike = createSegment(10.0, 2.0, 50.0, null, true);
        EnrichedSegment s3 = createSegment(10.0, 2.0, 20.0, null, true);

        SpeedStats stats = SpeedAnalyzer.calculateSpeed(List.of(s1, spike, s3));

        assertThat(stats.maxSpeed()).isEqualTo(20.0);
    }

    @Test
    void calculateSpeed_shouldAcceptValidSuddenAcceleration() {
        EnrichedSegment s1 = createSegment(10.0, 2.0, 20.0, null, true);
        EnrichedSegment s2 = createSegment(10.0, 2.0, 40.0, null, true);
        EnrichedSegment s3 = createSegment(10.0, 2.0, 45.0, null, true);
        EnrichedSegment s4 = createSegment(10.0, 2.0, 45.0, null, true);

        SpeedStats stats = SpeedAnalyzer.calculateSpeed(List.of(s1, s2, s3, s4));

        assertThat(stats.maxSpeed()).isEqualTo(45.0);
    }

    @Test
    void calculateSpeed_shouldNotFilterSpike_whenExactlyOnThreshold() {

        EnrichedSegment s1 = createSegment(10.0, 2.0, 20.0, null, true);
        EnrichedSegment edgeCaseSpike = createSegment(10.0, 2.0, 32.0, null, true);
        EnrichedSegment s3 = createSegment(10.0, 2.0, 20.0, null, true);

        SpeedStats stats = SpeedAnalyzer.calculateSpeed(List.of(s1, edgeCaseSpike, s3));

        assertThat(stats.maxSpeed()).isEqualTo(32.0);
    }

    private EnrichedSegment createSegment(Double distance, double duration, Double rawSpeed, Double p2Speed, boolean isMoving) {
        TrackPoint p1 = new TrackPoint();
        TrackPoint p2 = new TrackPoint();
        p2.setSpeed(p2Speed);

        return new EnrichedSegment(p1, p2, distance, duration, rawSpeed, isMoving);
    }
}