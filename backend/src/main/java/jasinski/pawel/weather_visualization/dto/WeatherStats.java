package jasinski.pawel.weather_visualization.dto;

public record WeatherStats(
        Double avgTemp, Double minTemp, Double maxTemp,
        Double avgWindSpeed, Double minWindSpeed, Double maxWindSpeed,
        Double avgDewPoint, Double minDewPoint, Double maxDewPoint,
        Double avgWindGusts, Double minWindGusts, Double maxWindGusts,
        Double sumRain, Double minRain, Double maxRain,
        Double sumSnowfall, Double minSnowfall, Double maxSnowfall,
        Double avgHumidity, Double minHumidity, Double maxHumidity,
        Double avgPressure, Double minPressure, Double maxPressure,
        Double avgCloudCover, Double minCloudCover, Double maxCloudCover,
        Double avgCloudCoverLow, Double avgCloudCoverMid, Double avgCloudCoverHigh,
        Double avgWaveHeight, Double minWaveHeight, Double maxWaveHeight,
        Double avgWavePeriod, Double minWavePeriod, Double maxWavePeriod,
        Double avgWindWaveHeight, Double avgWindWavePeriod,
        Double avgSwellWaveHeight, Double minSwellWaveHeight, Double maxSwellWaveHeight,
        Double avgSwellWavePeriod, Double minSwellWavePeriod, Double maxSwellWavePeriod,
        Double avgOceanCurrentVelocity, Double minOceanCurrentVelocity, Double maxOceanCurrentVelocity,
        Double avgSeaTemperature, Double minSeaTemperature, Double maxSeaTemperature
) {

    public WeatherStats() {
        this(
                null, null, null,
                null, null, null,
                null, null, null,
                null, null, null,
                null, null, null,
                null, null, null,
                null, null, null,
                null, null, null,
                null, null, null,
                null, null, null,
                null, null, null,
                null, null, null,
                null, null,
                null, null, null,
                null, null, null,
                null, null, null,
                null, null, null
        );
    }
}
