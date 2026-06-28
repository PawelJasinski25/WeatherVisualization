package jasinski.pawel.weather_visualization.utils;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GeoUtilsTest {

    private final GeometryFactory factory = new GeometryFactory();


    @Test
    void calculateDistance_shouldReturnZero_whenAnyPointIsNull() {
        Point p = createPoint(52.0, 21.0);

        assertThat(GeoUtils.calculateDistance(null, null)).isEqualTo(0.0);
        assertThat(GeoUtils.calculateDistance(p, null)).isEqualTo(0.0);
        assertThat(GeoUtils.calculateDistance(null, p)).isEqualTo(0.0);
    }


    @Test
    void calculateDistance_shouldReturnZero_forSameCoordinates() {
        Point p1 = createPoint(52.2297, 21.0122);
        Point p2 = createPoint(52.2297, 21.0122);

        assertThat(GeoUtils.calculateDistance(p1, p2)).isEqualTo(0.0);
    }

    @Test
    void calculateDistance_shouldCalculateCorrectly_alongEquator() {

        Point p1 = createPoint(0.0, 0.0);
        Point p2 = createPoint(0.0, 1.0);

        double distance = GeoUtils.calculateDistance(p1, p2);

        assertThat(distance).isCloseTo(111194.9, within(1.0));
    }

    @Test
    void calculateDistance_shouldCalculateCorrectly_alongMeridian() {
        Point p1 = createPoint(10.0, 20.0);
        Point p2 = createPoint(11.0, 20.0);

        double distance = GeoUtils.calculateDistance(p1, p2);

        assertThat(distance).isCloseTo(111194.9, within(1.0));
    }

    @Test
    void calculateDistance_shouldCalculateCorrectly_betweenRealCities() {
        Point warsaw = createPoint(52.2297, 21.0122);
        Point krakow = createPoint(50.0647, 19.9450);

        double distance = GeoUtils.calculateDistance(warsaw, krakow);

        assertThat(distance).isCloseTo(252000.0, within(2000.0));
    }

    private Point createPoint(double lat, double lng) {
        return factory.createPoint(new Coordinate(lng, lat));
    }
}
