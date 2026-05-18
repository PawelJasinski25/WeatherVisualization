package jasinski.pawel.weather_visualization.repository;

import jasinski.pawel.weather_visualization.entity.TrackPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrackPointRepository extends JpaRepository<TrackPoint, Long> {
    List<TrackPoint> findByTripIdOrderByTimeAsc(Long id);
    void deleteByTripId(Long tripId);
}
