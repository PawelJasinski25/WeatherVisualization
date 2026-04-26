package jasinski.pawel.weather_visualization.dto;

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
        Integer oceanCurrentDir
){
    public TrackPointDto(double latitude, double longitude, double segmentId, double timeMs, Integer dayPhase) {
        this(latitude, longitude, segmentId, timeMs, dayPhase,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null);
    }
}

