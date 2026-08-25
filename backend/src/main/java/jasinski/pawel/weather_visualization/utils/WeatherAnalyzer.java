package jasinski.pawel.weather_visualization.utils;

import jasinski.pawel.weather_visualization.dto.EnrichedSegment;
import jasinski.pawel.weather_visualization.dto.WeatherStats;
import jasinski.pawel.weather_visualization.entity.Weather;
import java.util.List;

public class WeatherAnalyzer {

    public static WeatherStats analyzeWeather(List<EnrichedSegment> segments) {
        if (segments == null || segments.isEmpty()) {
            return new WeatherStats();
        }

        MetricTracker temperature = new MetricTracker();
        MetricTracker windSpeed = new MetricTracker();
        MetricTracker dewPoint = new MetricTracker();
        MetricTracker windGusts = new MetricTracker();
        MetricTracker humidity = new MetricTracker();
        MetricTracker pressure = new MetricTracker();

        MetricTracker cloudCover = new MetricTracker();
        MetricTracker cloudCoverLow = new MetricTracker();
        MetricTracker cloudCoverMid = new MetricTracker();
        MetricTracker cloudCoverHigh = new MetricTracker();

        MetricTracker waveHeight = new MetricTracker();
        MetricTracker wavePeriod = new MetricTracker();
        MetricTracker windWaveHeight = new MetricTracker();
        MetricTracker windWavePeriod = new MetricTracker();
        MetricTracker swellWaveHeight = new MetricTracker();
        MetricTracker swellWavePeriod = new MetricTracker();

        MetricTracker oceanCurrentVelocity = new MetricTracker();
        MetricTracker seaTemperature = new MetricTracker();

        PrecipitationTracker rain = new PrecipitationTracker();
        PrecipitationTracker snowfall = new PrecipitationTracker();

        for (EnrichedSegment seg : segments) {
            Weather w = seg.p1().getWeather();
            if (w == null) continue;

            long durationInSeconds = (long) seg.durationSeconds();
            if (durationInSeconds > 3600) {
                durationInSeconds = 3600;
            }

            temperature.add(w.getTemp(), durationInSeconds);
            windSpeed.add(w.getWindSpeed(), durationInSeconds);
            dewPoint.add(w.getDewPoint(), durationInSeconds);
            windGusts.add(w.getWindGusts(), durationInSeconds);
            humidity.add(w.getHumidity(), durationInSeconds);
            pressure.add(w.getPressure(), durationInSeconds);

            cloudCover.add(w.getCloudCover(), durationInSeconds);
            cloudCoverLow.add(w.getCloudCoverLow(), durationInSeconds);
            cloudCoverMid.add(w.getCloudCoverMid(), durationInSeconds);
            cloudCoverHigh.add(w.getCloudCoverHigh(), durationInSeconds);

            waveHeight.add(w.getWaveHeight(), durationInSeconds);
            wavePeriod.add(w.getWavePeriod(), durationInSeconds);
            windWaveHeight.add(w.getWindWaveHeight(), durationInSeconds);
            windWavePeriod.add(w.getWindWavePeriod(), durationInSeconds);
            swellWaveHeight.add(w.getSwellWaveHeight(), durationInSeconds);
            swellWavePeriod.add(w.getSwellWavePeriod(), durationInSeconds);

            oceanCurrentVelocity.add(w.getOceanCurrentVelocity() != null ? w.getOceanCurrentVelocity() / 3.6 : null, durationInSeconds);
            seaTemperature.add(w.getSeaTemperature(), durationInSeconds);

            rain.add(w.getRain(), durationInSeconds);
            snowfall.add(w.getSnowfall(), durationInSeconds);
        }

        return new WeatherStats(
                temperature.getAvg(), temperature.getMin(), temperature.getMax(),
                windSpeed.getAvg(), windSpeed.getMin(), windSpeed.getMax(),
                dewPoint.getAvg(), dewPoint.getMin(), dewPoint.getMax(),
                windGusts.getAvg(), windGusts.getMin(), windGusts.getMax(),
                rain.getSum(), rain.getMin(), rain.getMax(),
                snowfall.getSum(), snowfall.getMin(), snowfall.getMax(),
                humidity.getAvg(), humidity.getMin(), humidity.getMax(),
                pressure.getAvg(), pressure.getMin(), pressure.getMax(),
                cloudCover.getAvg(), cloudCover.getMin(), cloudCover.getMax(),
                cloudCoverLow.getAvg(), cloudCoverMid.getAvg(), cloudCoverHigh.getAvg(),
                waveHeight.getAvg(), waveHeight.getMin(), waveHeight.getMax(),
                wavePeriod.getAvg(), wavePeriod.getMin(), wavePeriod.getMax(),
                windWaveHeight.getAvg(), windWavePeriod.getAvg(),
                swellWaveHeight.getAvg(), swellWaveHeight.getMin(), swellWaveHeight.getMax(),
                swellWavePeriod.getAvg(), swellWavePeriod.getMin(), swellWavePeriod.getMax(),
                oceanCurrentVelocity.getAvg(), oceanCurrentVelocity.getMin(), oceanCurrentVelocity.getMax(),
                seaTemperature.getAvg(), seaTemperature.getMin(), seaTemperature.getMax()
        );
    }


    private static class MetricTracker {
        private double weightedSum = 0.0;
        private long totalSeconds = 0;
        private Double min = null;
        private Double max = null;

        public void add(Number incomingValue, long durationInSeconds) {
            if (incomingValue != null) {
                double metricValue = incomingValue.doubleValue();

                if (durationInSeconds > 0) {
                    weightedSum += metricValue * durationInSeconds;
                    totalSeconds += durationInSeconds;
                }

                if (min == null || metricValue < min) min = metricValue;
                if (max == null || metricValue > max) max = metricValue;
            }
        }

        public Double getAvg() {
            return totalSeconds == 0 ? null : Math.round((weightedSum / totalSeconds) * 100.0) / 100.0;
        }

        public Double getMin() {
            return min != null ? Math.round(min * 100.0) / 100.0 : null;
        }

        public Double getMax() {
            return max != null ? Math.round(max * 100.0) / 100.0 : null;
        }
    }

    private static class PrecipitationTracker {
        private double accumulatedSum = 0.0;
        private Double min = null;
        private Double max = null;

        public void add(Number hourlyPrecipitation, long durationInSeconds) {
            if (hourlyPrecipitation != null) {
                double rateValue = hourlyPrecipitation.doubleValue();

                if (durationInSeconds > 0) {
                    double fractionOfHour = durationInSeconds / 3600.0;
                    accumulatedSum += rateValue * fractionOfHour;
                }

                if (min == null || rateValue < min) min = rateValue;
                if (max == null || rateValue > max) max = rateValue;
            }
        }

        public Double getSum() {
            return Math.round(accumulatedSum * 100.0) / 100.0;
        }

        public Double getMin() {
            return min != null ? Math.round(min * 100.0) / 100.0 : null;
        }

        public Double getMax() {
            return max != null ? Math.round(max * 100.0) / 100.0 : null;
        }
    }
}