package jasinski.pawel.weather_visualization.repository;
import jasinski.pawel.weather_visualization.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;


public interface TripRepository extends JpaRepository<Trip, Long> {
}
