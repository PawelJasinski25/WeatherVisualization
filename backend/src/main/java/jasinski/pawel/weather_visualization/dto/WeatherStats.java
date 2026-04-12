package jasinski.pawel.weather_visualization.dto;

public record WeatherStats(
        Double avgTemp, Double avgWindSpeed, Integer avgWindDir, Double avgDewPoint, Double avgWindGusts,
        Double sumRain, Double sumSnowfall,
        Double avgHumidity, Double avgPressure,
        Double avgCloudCover, Double avgCloudCoverLow, Double avgCloudCoverMid, Double avgCloudCoverHigh,
        Double avgWaveHeight, Double avgWavePeriod, Integer avgWaveDirection,
        Double avgWindWaveHeight, Double avgWindWavePeriod,
        Double avgSwellWaveHeight, Double avgSwellWavePeriod,
        Double avgOceanCurrentVelocity, Integer avgOceanCurrentDirection, Double avgSeaTemperature
) {
    public WeatherStats() {
        this(
                null, null, null, null, null,
                0.0, 0.0,
                null, null,
                null, null, null, null,
                null, null, null,
                null, null,
                null, null,
                null, null, null
        );
    }
}
