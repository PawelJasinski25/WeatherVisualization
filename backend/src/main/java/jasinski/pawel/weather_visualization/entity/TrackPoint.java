package jasinski.pawel.weather_visualization.entity;

import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;

import java.time.Instant;

@Entity
@Table(name = "track_points")
public class TrackPoint {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tp_seq_gen")
    @SequenceGenerator(
            name = "tp_seq_gen",
            sequenceName = "track_point_sequence",
            allocationSize = 50
    )
    private Long id;

    private Instant time;
    private double elevation;

    @Column(columnDefinition = "geography(Point, 4326)")
    private Point location;

    @ManyToOne
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(name = "segment_id")
    private Integer segmentId;

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

    public double getElevation() {
        return elevation;
    }

    public void setElevation(double elevation) {
        this.elevation = elevation;
    }

    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
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
}
