package jasinski.pawel.weather_visualization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class WeatherVisualizationApplication {

	public static void main(String[] args) {
		SpringApplication.run(WeatherVisualizationApplication.class, args);
	}



}
