package jasinski.pawel.weather_visualization.repository;

import jasinski.pawel.weather_visualization.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
