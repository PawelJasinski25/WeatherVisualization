package jasinski.pawel.weather_visualization.utils;

import jasinski.pawel.weather_visualization.dto.WeatherStats;
import jasinski.pawel.weather_visualization.entity.TrackPoint;
import jasinski.pawel.weather_visualization.entity.Weather;

import java.time.Duration;
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

        for (int i = 0; i < points.size(); i++) {
            TrackPoint p = points.get(i);
            Weather w = p.getWeather();
            if (w == null) continue;

            // 1. OBLICZANIE WAGI (CZASU TRWANIA)
            long weightSeconds = 60; // Domyślnie 60s dla ostatniego punktu
            if (i < points.size() - 1) {
                TrackPoint nextP = points.get(i + 1);
                weightSeconds = Math.abs(Duration.between(p.getTime(), nextP.getTime()).getSeconds());

                // Zabezpieczenie przed "Brak Danych" - limitujemy wagę maksymalnie do 1 godziny (3600s)
                // Zapobiega to sytuacji, gdzie 10-godzinna przerwa w GPS całkowicie niszczy średnią
                if (weightSeconds > 3600) {
                    weightSeconds = 3600;
                }
            }

            // 2. PRZEKAZYWANIE DANYCH Z WAGĄ CZASOWĄ
            temp.add(w.getTemp(), weightSeconds);
            windSpeed.add(w.getWindSpeed(), weightSeconds);
            dewPoint.add(w.getDewPoint(), weightSeconds);
            windGusts.add(w.getWindGusts(), weightSeconds);
            humidity.add(w.getHumidity(), weightSeconds);
            pressure.add(w.getPressure(), weightSeconds);
            cloud.add(w.getCloudCover(), weightSeconds);
            cloudLow.add(w.getCloudCoverLow(), weightSeconds);
            cloudMid.add(w.getCloudCoverMid(), weightSeconds);
            cloudHigh.add(w.getCloudCoverHigh(), weightSeconds);
            waveH.add(w.getWaveHeight(), weightSeconds);
            waveP.add(w.getWavePeriod(), weightSeconds);
            windWaveH.add(w.getWindWaveHeight(), weightSeconds);
            windWaveP.add(w.getWindWavePeriod(), weightSeconds);
            swellH.add(w.getSwellWaveHeight(), weightSeconds);
            swellP.add(w.getSwellWavePeriod(), weightSeconds);
            oceanCurVel.add(w.getOceanCurrentVelocity(), weightSeconds);
            seaTemp.add(w.getSeaTemperature(), weightSeconds);

            // Sumatory dla opadów
            rain.add(w.getRain());
            snowfall.add(w.getSnowfall());

            windDir.add(w.getWindDir(), weightSeconds);
            waveDir.add(w.getWaveDirection(), weightSeconds);
            oceanDir.add(w.getOceanCurrentDirection(), weightSeconds);
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
        private double weightedSum = 0;
        private long totalSeconds = 0;

        public void add(Number val, long weightSeconds) {
            if (val != null && weightSeconds > 0) {
                weightedSum += val.doubleValue() * weightSeconds;
                totalSeconds += weightSeconds;
            }
        }
        public Double get() {
            return totalSeconds == 0 ? null : Math.round((weightedSum / totalSeconds) * 100.0) / 100.0;
        }
    }

    private static class AngleAverager {
        private double sinSum = 0, cosSum = 0;
        private long totalSeconds = 0;

        public void add(Number angle, long weightSeconds) {
            if (angle != null && weightSeconds > 0) {
                double rad = Math.toRadians(angle.doubleValue());
                sinSum += Math.sin(rad) * weightSeconds;
                cosSum += Math.cos(rad) * weightSeconds;
                totalSeconds += weightSeconds;
            }
        }
        public Integer get() {
            if (totalSeconds == 0 || (sinSum == 0 && cosSum == 0)) return null;
            double angle = Math.toDegrees(Math.atan2(sinSum / totalSeconds, cosSum / totalSeconds));
            return (int) Math.round(angle < 0 ? angle + 360 : angle);
        }
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
}