package jasinski.pawel.weather_visualization.dto;

import jasinski.pawel.weather_visualization.entity.TrackPoint;

import java.time.LocalTime;

public record AstronomyStats(
        LocalTime astronomicalDawn,
        LocalTime nauticalDawn,
        LocalTime civilDawn,
        LocalTime sunrise,
        LocalTime solarNoon,
        LocalTime sunset,
        LocalTime civilDusk,
        LocalTime nauticalDusk,
        LocalTime astronomicalDusk,

        TrackPoint astronomicalDawnPt,
        TrackPoint nauticalDawnPt,
        TrackPoint civilDawnPt,
        TrackPoint sunrisePt,
        TrackPoint noonPt,
        TrackPoint sunsetPt,
        TrackPoint civilDuskPt,
        TrackPoint nauticalDuskPt,
        TrackPoint astronomicalDuskPt
) {}