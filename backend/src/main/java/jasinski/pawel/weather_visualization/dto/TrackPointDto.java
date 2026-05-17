package jasinski.pawel.weather_visualization.dto;

import jasinski.pawel.weather_visualization.entity.TrackPoint;
import jasinski.pawel.weather_visualization.entity.Weather;

public record TrackPointDto(
        double latitude,
        double longitude,
        double segmentId,
        double timeMs,
        Integer dayPhase,
        Double windSpeed,
        Double temp,
        Double gusts,
        Double dewPoint,
        Double rain,
        Integer humidity,
        Double pressure,
        Integer cloudCover,
        Integer cloudLow,
        Integer cloudMid,
        Integer cloudHigh,
        Integer windDir,
        Double snowfall,
        Double waveHeight,
        Double wavePeriod,
        Integer waveDir,
        Double windWaveH,
        Double windWaveP,
        Double swellWaveH,
        Double swellWaveP,
        Double oceanCurrentVel,
        Double seaTemp,
        Integer oceanCurrentDir,
        Double speed
){
    public TrackPointDto(double latitude, double longitude, double segmentId, double timeMs, Integer dayPhase) {
        this(latitude, longitude, segmentId, timeMs, dayPhase,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public static TrackPointDto fromEntity(TrackPoint p) {
        return fromEntityWithSpeed(p, null);
    }


    public static TrackPointDto fromEntityWithSpeed(TrackPoint p, Double speed) {
        Weather w = p.getWeather();
        return new TrackPointDto(
                p.getLocation() != null ? p.getLocation().getY() : 0.0,
                p.getLocation() != null ? p.getLocation().getX() : 0.0,
                p.getSegmentId() != null ? p.getSegmentId() : 0.0,
                p.getTime() != null ? (double) p.getTime().toEpochMilli() : 0.0,
                4,
                w != null ? w.getWindSpeed() : null,
                w != null ? w.getTemp() : null,
                w != null ? w.getWindGusts() : null,
                w != null ? w.getDewPoint() : null,
                w != null ? w.getRain() : null,
                w != null ? w.getHumidity() : null,
                w != null ? w.getPressure() : null,
                w != null ? w.getCloudCover() : null,
                w != null ? w.getCloudCoverLow() : null,
                w != null ? w.getCloudCoverMid() : null,
                w != null ? w.getCloudCoverHigh() : null,
                w != null ? w.getWindDir() : null,
                w != null ? w.getSnowfall() : null,
                w != null ? w.getWaveHeight() : null,
                w != null ? w.getWavePeriod() : null,
                w != null ? w.getWaveDirection() : null,
                w != null ? w.getWindWaveHeight() : null,
                w != null ? w.getWindWavePeriod() : null,
                w != null ? w.getSwellWaveHeight() : null,
                w != null ? w.getSwellWavePeriod() : null,
                w != null ? w.getOceanCurrentVelocity() : null,
                w != null ? w.getSeaTemperature() : null,
                w != null ? w.getOceanCurrentDirection() : null,
                speed
        );
    }
}


