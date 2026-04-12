package jasinski.pawel.weather_visualization.utils;

import jasinski.pawel.weather_visualization.dto.WeatherStats;
import jasinski.pawel.weather_visualization.entity.TrackPoint;
import jasinski.pawel.weather_visualization.entity.Weather;

import java.util.List;

public class WeatherAnalyzer {

    public static WeatherStats analyzeWeather(List<TrackPoint> points) {
        if (points == null || points.isEmpty()) {
            return new WeatherStats();
        }

        Averager temp = new Averager(), windSpeed = new Averager(), dewPoint = new Averager(), windGusts = new Averager();
        Averager humidity = new Averager(), pressure = new Averager();
        Averager cloud = new Averager(), cloudLow = new Averager(), cloudMid = new Averager(), cloudHigh = new Averager();
        Averager waveH = new Averager(), waveP = new Averager(), windWaveH = new Averager(), windWaveP = new Averager();
        Averager swellH = new Averager(), swellP = new Averager(), oceanCurVel = new Averager(), seaTemp = new Averager();

        Summer rain = new Summer(), snowfall = new Summer();
        AngleAverager windDir = new AngleAverager(), waveDir = new AngleAverager(), oceanDir = new AngleAverager();

        for (TrackPoint p : points) {
            Weather w = p.getWeather();
            if (w == null) continue;

            temp.add(w.getTemp()); windSpeed.add(w.getWindSpeed()); dewPoint.add(w.getDewPoint()); windGusts.add(w.getWindGusts());
            humidity.add(w.getHumidity()); pressure.add(w.getPressure());
            cloud.add(w.getCloudCover()); cloudLow.add(w.getCloudCoverLow()); cloudMid.add(w.getCloudCoverMid()); cloudHigh.add(w.getCloudCoverHigh());
            waveH.add(w.getWaveHeight()); waveP.add(w.getWavePeriod()); windWaveH.add(w.getWindWaveHeight()); windWaveP.add(w.getWindWavePeriod());
            swellH.add(w.getSwellWaveHeight()); swellP.add(w.getSwellWavePeriod()); oceanCurVel.add(w.getOceanCurrentVelocity()); seaTemp.add(w.getSeaTemperature());

            rain.add(w.getRain()); snowfall.add(w.getSnowfall());

            windDir.add(w.getWindDir()); waveDir.add(w.getWaveDirection()); oceanDir.add(w.getOceanCurrentDirection());
        }

        return new WeatherStats(
                temp.get(), windSpeed.get(), windDir.get(), dewPoint.get(), windGusts.get(),
                rain.get(), snowfall.get(), humidity.get(), pressure.get(),
                cloud.get(), cloudLow.get(), cloudMid.get(), cloudHigh.get(),
                waveH.get(), waveP.get(), waveDir.get(), windWaveH.get(), windWaveP.get(),
                swellH.get(), swellP.get(), oceanCurVel.get(), oceanDir.get(), seaTemp.get()
        );
    }


    private static class Averager {
        private double sum = 0; private int count = 0;
        public void add(Number val) {
            if (val != null) {
                sum += val.doubleValue();
                count++;
            }
        }
        public Double get() { return count == 0 ? null : Math.round((sum / count) * 100.0) / 100.0; }
    }

    private static class Summer {
        private double sum = 0;
        public void add(Number val) {
            if (val != null) {
                sum += val.doubleValue();
            }
        }
        public Double get() { return Math.round(sum * 100.0) / 100.0; }
    }

    private static class AngleAverager {
        private double sinSum = 0, cosSum = 0; private int count = 0;
        public void add(Number angle) {
            if (angle != null) {
                sinSum += Math.sin(Math.toRadians(angle.doubleValue()));
                cosSum += Math.cos(Math.toRadians(angle.doubleValue()));
                count++;
            }
        }
        public Integer get() {
            if (count == 0 || (sinSum == 0 && cosSum == 0)) return null;
            double angle = Math.toDegrees(Math.atan2(sinSum / count, cosSum / count));
            return (int) Math.round(angle < 0 ? angle + 360 : angle);
        }
    }
}