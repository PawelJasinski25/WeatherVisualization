package jasinski.pawel.weather_visualization.dto;

public record AstronomyStats(
        String astronomicalDawn, // Świt astronomiczny
        String nauticalDawn,     // Świt nautyczny
        String civilDawn,        // Świt cywilny
        String sunrise,          // Wschód słońca
        String sunset,           // Zachód słońca
        String civilDusk,        // Zmierzch cywilny
        String nauticalDusk,     // Zmierzch nautyczny
        String astronomicalDusk  // Zmierzch astronomiczny
) {}