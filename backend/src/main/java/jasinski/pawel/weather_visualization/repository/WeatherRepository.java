package jasinski.pawel.weather_visualization.repository;

import jasinski.pawel.weather_visualization.entity.Weather;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeatherRepository extends JpaRepository<Weather, Long> {
    void deleteByTripId(Long tripId);
}
