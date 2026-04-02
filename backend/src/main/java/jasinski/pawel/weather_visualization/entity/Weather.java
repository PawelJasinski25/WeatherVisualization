package jasinski.pawel.weather_visualization.entity;


import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "weather")
public class Weather {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    private Instant time;
    private double latitude;
    private double longitude;
    private Double temp;

    @Column(name = "wind_speed")
    private Double windSpeed;

    @Column(name = "wind_dir")
    private Integer windDir;

    @Column(name = "dew_point")
    private Double dewPoint;

    @Column(name = "wind_gusts")
    private Double windGusts;

    private Double rain;

    private Double snowfall;

    private Integer humidity;

    private Double pressure;

    @Column(name = "cloud_cover")
    private Integer cloudCover;

    @Column(name = "cloud_cover_low")
    private Integer cloudCoverLow;

    @Column(name = "cloud_cover_mid")
    private Integer cloudCoverMid;

    @Column(name = "cloud_cover_high")
    private Integer cloudCoverHigh;

    @Column(name = "wave_height")
    private Double waveHeight;

    @Column(name = "wave_period")
    private Double wavePeriod;

    @Column(name = "wave_direction")
    private Integer waveDirection;

    @Column(name = "wind_wave_height")
    private Double windWaveHeight;

    @Column(name = "wind_wave_period")
    private Double windWavePeriod;

    @Column(name = "swell_wave_height")
    private Double swellWaveHeight;

    @Column(name = "swell_wave_period")
    private Double swellWavePeriod;

    @Column(name = "ocean_current_velocity")
    private Double oceanCurrentVelocity;

    @Column(name = "ocean_current_direction")
    private Integer oceanCurrentDirection;

    @Column(name = "sea_temperature")
    private Double seaTemperature;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public Instant getTime() {
        return time;
    }

    public void setTime(Instant time) {
        this.time = time;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Double getTemp() {
        return temp;
    }

    public void setTemp(Double temp) {
        this.temp = temp;
    }

    public Double getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(Double windSpeed) {
        this.windSpeed = windSpeed;
    }

    public Integer getWindDir() {
        return windDir;
    }

    public void setWindDir(Integer windDir) {
        this.windDir = windDir;
    }

    public Double getDewPoint() {
        return dewPoint;
    }

    public void setDewPoint(Double dewPoint) {
        this.dewPoint = dewPoint;
    }

    public Double getWindGusts() {
        return windGusts;
    }

    public void setWindGusts(Double windGusts) {
        this.windGusts = windGusts;
    }

    public Double getRain() {
        return rain;
    }

    public void setRain(Double rain) {
        this.rain = rain;
    }

    public Integer getHumidity() {
        return humidity;
    }

    public void setHumidity(Integer humidity) {
        this.humidity = humidity;
    }

    public Double getPressure() {
        return pressure;
    }

    public void setPressure(Double pressure) {
        this.pressure = pressure;
    }

    public Integer getCloudCover() {
        return cloudCover;
    }

    public void setCloudCover(Integer cloudCover) {
        this.cloudCover = cloudCover;
    }

    public Integer getCloudCoverLow() {
        return cloudCoverLow;
    }

    public void setCloudCoverLow(Integer cloudCoverLow) {
        this.cloudCoverLow = cloudCoverLow;
    }

    public Integer getCloudCoverMid() {
        return cloudCoverMid;
    }

    public void setCloudCoverMid(Integer cloudCoverMid) {
        this.cloudCoverMid = cloudCoverMid;
    }

    public Integer getCloudCoverHigh() {
        return cloudCoverHigh;
    }

    public void setCloudCoverHigh(Integer cloudCoverHigh) {
        this.cloudCoverHigh = cloudCoverHigh;
    }

    public Double getSnowfall() {
        return snowfall;
    }

    public void setSnowfall(Double snowfall) {
        this.snowfall = snowfall;
    }

    public Double getWaveHeight() {
        return waveHeight;
    }

    public void setWaveHeight(Double waveHeight) {
        this.waveHeight = waveHeight;
    }

    public Double getWavePeriod() {
        return wavePeriod;
    }

    public void setWavePeriod(Double wavePeriod) {
        this.wavePeriod = wavePeriod;
    }

    public Integer getWaveDirection() {
        return waveDirection;
    }

    public void setWaveDirection(Integer waveDirection) {
        this.waveDirection = waveDirection;
    }

    public Double getWindWaveHeight() {
        return windWaveHeight;
    }

    public void setWindWaveHeight(Double windWaveHeight) {
        this.windWaveHeight = windWaveHeight;
    }

    public Double getWindWavePeriod() {
        return windWavePeriod;
    }

    public void setWindWavePeriod(Double windWavePeriod) {
        this.windWavePeriod = windWavePeriod;
    }

    public Double getSwellWaveHeight() {
        return swellWaveHeight;
    }

    public void setSwellWaveHeight(Double swellWaveHeight) {
        this.swellWaveHeight = swellWaveHeight;
    }

    public Double getSwellWavePeriod() {
        return swellWavePeriod;
    }

    public void setSwellWavePeriod(Double swellWavePeriod) {
        this.swellWavePeriod = swellWavePeriod;
    }

    public Double getOceanCurrentVelocity() {
        return oceanCurrentVelocity;
    }

    public void setOceanCurrentVelocity(Double oceanCurrentVelocity) {
        this.oceanCurrentVelocity = oceanCurrentVelocity;
    }

    public Integer getOceanCurrentDirection() {
        return oceanCurrentDirection;
    }

    public void setOceanCurrentDirection(Integer oceanCurrentDirection) {
        this.oceanCurrentDirection = oceanCurrentDirection;
    }

    public Double getSeaTemperature() {
        return seaTemperature;
    }

    public void setSeaTemperature(Double seaTemperature) {
        this.seaTemperature = seaTemperature;
    }
}
