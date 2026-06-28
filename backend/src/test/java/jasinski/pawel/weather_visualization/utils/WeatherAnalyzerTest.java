package jasinski.pawel.weather_visualization.utils;

import jasinski.pawel.weather_visualization.dto.EnrichedSegment;
import jasinski.pawel.weather_visualization.dto.WeatherStats;
import jasinski.pawel.weather_visualization.entity.TrackPoint;
import jasinski.pawel.weather_visualization.entity.Weather;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherAnalyzerTest {


    @Test
    void analyzeWeather_shouldReturnEmptyStats_whenSegmentsAreNullOrEmpty() {
        WeatherStats nullStats = WeatherAnalyzer.analyzeWeather(null);
        WeatherStats emptyStats = WeatherAnalyzer.analyzeWeather(List.of());

        assertThat(nullStats.avgTemp()).isNull();
        assertThat(emptyStats.avgTemp()).isNull();
    }

    @Test
    void analyzeWeather_shouldReturnEmptyStats_whenSegmentsHaveNoWeather() {
        EnrichedSegment segWithoutWeather = createSegment(100.0, null);

        WeatherStats stats = WeatherAnalyzer.analyzeWeather(List.of(segWithoutWeather));

        assertThat(stats.avgTemp()).isNull();
        assertThat(stats.sumRain()).isEqualTo(0.0);
    }


    @Test
    void analyzeWeather_shouldCalculateWeightedAverages_basedOnSegmentDuration() {

        Weather w1 = new Weather(); w1.setTemp(10.0);
        Weather w2 = new Weather(); w2.setTemp(20.0);

        EnrichedSegment seg1 = createSegment(10.0, w1);
        EnrichedSegment seg2 = createSegment(30.0, w2);

        WeatherStats stats = WeatherAnalyzer.analyzeWeather(List.of(seg1, seg2));

        assertThat(stats.avgTemp()).isEqualTo(17.5);
    }

    @Test
    void analyzeWeather_shouldCapWeightAt3600Seconds() {

        Weather w1 = new Weather(); w1.setTemp(10.0);
        Weather w2 = new Weather(); w2.setTemp(20.0);

        EnrichedSegment seg1 = createSegment(5000.0, w1);
        EnrichedSegment seg2 = createSegment(3600.0, w2);

        WeatherStats stats = WeatherAnalyzer.analyzeWeather(List.of(seg1, seg2));

        assertThat(stats.avgTemp()).isEqualTo(15.0);
    }


    @Test
    void analyzeWeather_shouldCalculateCircularMeanForAngles_correctlyHandlingCrossOver() {

        Weather w1 = new Weather(); w1.setWindDir(350);
        Weather w2 = new Weather(); w2.setWindDir(10);

        EnrichedSegment seg1 = createSegment(10.0, w1);
        EnrichedSegment seg2 = createSegment(10.0, w2);

        WeatherStats stats = WeatherAnalyzer.analyzeWeather(List.of(seg1, seg2));

        assertThat(stats.avgWindDir()).isIn(0, 360);
    }


    @Test
    void analyzeWeather_shouldAccumulateRain_basedOnFractionOfHour() {

        Weather w1 = new Weather(); w1.setRain(3.6);
        Weather w2 = new Weather(); w2.setRain(7.2);

        EnrichedSegment seg1 = createSegment(1000.0, w1);
        EnrichedSegment seg2 = createSegment(500.0, w2);

        WeatherStats stats = WeatherAnalyzer.analyzeWeather(List.of(seg1, seg2));

        assertThat(stats.sumRain()).isEqualTo(2.0);
    }


    @Test
    void analyzeWeather_shouldHandleMissingOptionalData_withoutBreakingAverages() {

        Weather w1 = new Weather();
        w1.setTemp(15.0);
        w1.setCloudCover(100);

        Weather w2 = new Weather();
        w2.setTemp(25.0);
        w2.setCloudCover(null);

        EnrichedSegment seg1 = createSegment(10.0, w1);
        EnrichedSegment seg2 = createSegment(10.0, w2);

        WeatherStats stats = WeatherAnalyzer.analyzeWeather(List.of(seg1, seg2));

        assertThat(stats.avgTemp()).isEqualTo(20.0);
        assertThat(stats.avgCloudCover()).isEqualTo(100);
    }

    private EnrichedSegment createSegment(double durationSeconds, Weather weather) {
        TrackPoint p1 = new TrackPoint();
        p1.setWeather(weather);

        TrackPoint p2 = new TrackPoint();

        return new EnrichedSegment(p1, p2, 0.0, durationSeconds, 0.0, true);
    }
}
