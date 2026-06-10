package jasinski.pawel.weather_visualization.repository;

import jasinski.pawel.weather_visualization.entity.TrackPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TrackPointRepository extends JpaRepository<TrackPoint, Long> {
    List<TrackPoint> findByTripIdOrderByTimeAsc(Long id);

    @Modifying
    @Query("DELETE FROM TrackPoint t WHERE t.trip.id = :tripId")
    void deleteAllByTripId(@Param("tripId") Long tripId);
}
