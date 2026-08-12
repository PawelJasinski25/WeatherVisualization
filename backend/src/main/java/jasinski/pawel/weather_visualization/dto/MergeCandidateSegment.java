package jasinski.pawel.weather_visualization.dto;

import jasinski.pawel.weather_visualization.entity.TrackPoint;

import java.util.List;

public record MergeCandidateSegment(List<TrackPoint> points) {
}
