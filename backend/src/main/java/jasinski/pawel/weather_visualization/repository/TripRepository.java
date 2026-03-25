package jasinski.pawel.weather_visualization.repository;
import jasinski.pawel.weather_visualization.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findByUser_Email(String email);
    Optional<Trip> findByFileHashAndUser_Email(String fileHash, String email);
}
