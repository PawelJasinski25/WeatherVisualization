package jasinski.pawel.weather_visualization.repository;

import jasinski.pawel.weather_visualization.entity.Weather;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WeatherRepository extends JpaRepository<Weather, Long> {

    @Modifying
    @Query("DELETE FROM Weather w WHERE w.trip.id = :tripId")
    void deleteAllByTripId(@Param("tripId") Long tripId);
}
