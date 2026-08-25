package jasinski.pawel.weather_visualization.entity;

import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;

import java.time.Instant;

@Entity
@Table(name = "track_points", indexes = {
        @Index(name = "idx_tp_trip_id", columnList = "trip_id"),
        @Index(name = "idx_tp_weather_id", columnList = "weather_id")
})
public class TrackPoint {

    public TrackPoint() {}

    public TrackPoint(TrackPoint source) {
        this.id = source.getId();
        this.time = source.getTime();
        this.speed = source.getSpeed();
        this.latitude = source.getLatitude();
        this.longitude = source.getLongitude();
        this.trip = source.getTrip();
        this.segmentId = source.getSegmentId();
        this.weather = source.getWeather();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tp_seq_gen")
    @SequenceGenerator(
            name = "tp_seq_gen",
            sequenceName = "track_point_sequence",
            allocationSize = 50
    )

    private Long id;
    private Instant time;
    private Double speed;
    private double latitude;
    private double longitude;

    @ManyToOne
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(name = "segment_id")
    private Integer segmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "weather_id")
    private Weather weather;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getTime() {
        return time;
    }

    public void setTime(Instant time) {
        this.time = time;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public Integer getSegmentId() {
        return segmentId;
    }

    public void setSegmentId(Integer segmentId) {
        this.segmentId = segmentId;
    }

    public Weather getWeather() {
        return weather;
    }

    public void setWeather(Weather weather) {
        this.weather = weather;
    }

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }
}
