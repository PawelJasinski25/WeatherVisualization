package jasinski.pawel.weather_visualization.dto;

import jasinski.pawel.weather_visualization.entity.TrackPoint;

import java.time.Instant;
import java.time.LocalTime;

public record AstronomyStats(
        Instant astronomicalDawn,
        Instant nauticalDawn,
        Instant civilDawn,
        Instant sunrise,
        Instant solarNoon,
        Instant sunset,
        Instant civilDusk,
        Instant nauticalDusk,
        Instant astronomicalDusk,
        Instant moonRise,
        Instant moonSet,

        TrackPoint astronomicalDawnPt,
        TrackPoint nauticalDawnPt,
        TrackPoint civilDawnPt,
        TrackPoint sunrisePt,
        TrackPoint noonPt,
        TrackPoint sunsetPt,
        TrackPoint civilDuskPt,
        TrackPoint nauticalDuskPt,
        TrackPoint astronomicalDuskPt,
        TrackPoint moonRisePt,
        TrackPoint moonSetPt
) {}