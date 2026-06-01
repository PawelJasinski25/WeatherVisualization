package jasinski.pawel.weather_visualization.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class WaterDetectionService {

    private PreparedGeometry waterGeometry;
    private final GeometryFactory factory = new GeometryFactory();

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getResourceAsStream("/water.geojson")) {
            if (is == null) {
                System.out.println("Brak pliku water.geojson w resources");
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(is);
            List<Polygon> polygons = new ArrayList<>();

            JsonNode features = root.path("features");
            for (JsonNode feature : features) {
                JsonNode geometry = feature.path("geometry");
                String type = geometry.path("type").asText();
                JsonNode coords = geometry.path("coordinates");

                if ("Polygon".equals(type)) {
                    polygons.add(parsePolygon(coords));
                } else if ("MultiPolygon".equals(type)) {
                    for (JsonNode polyNode : coords) {
                        polygons.add(parsePolygon(polyNode));
                    }
                }
            }

            MultiPolygon multiPolygon = factory.createMultiPolygon(polygons.toArray(new Polygon[0]));
            this.waterGeometry = PreparedGeometryFactory.prepare(multiPolygon);

        } catch (Exception e) {
            System.err.println("Błąd ładowania geojson: " + e.getMessage());
        }
    }

    private Polygon parsePolygon(JsonNode coordsNode) {
        JsonNode outerRingNode = coordsNode.get(0);
        Coordinate[] coords = new Coordinate[outerRingNode.size()];
        for (int i = 0; i < outerRingNode.size(); i++) {
            JsonNode pt = outerRingNode.get(i);
            coords[i] = new Coordinate(pt.get(0).asDouble(), pt.get(1).asDouble());
        }
        LinearRing outerRing = factory.createLinearRing(coords);
        return factory.createPolygon(outerRing, null);
    }

    public boolean isWater(double lat, double lon) {
        if (waterGeometry == null)
            return false;
        Point p = factory.createPoint(new Coordinate(lon, lat));
        return waterGeometry.contains(p);
    }
}