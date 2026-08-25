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
    void calculateDistance_shouldReturnZero_whenCoordinatesAreIdentical() {

        assertThat(GeoUtils.calculateDistance(52.2297, 21.0122, 52.2297, 21.0122)).isEqualTo(0.0);
    }

    @Test
    void calculateDistance_shouldCalculateCorrectly_whenMovingAlongEquator() {

        double distance = GeoUtils.calculateDistance(0.0, 0.0, 0.0, 1.0);
        assertThat(distance).isCloseTo(111194.9, within(1.0));
    }

    @Test
    void calculateDistance_shouldCalculateCorrectly_whenMovingAlongMeridian() {

        double distance = GeoUtils.calculateDistance(10.0, 20.0, 11.0, 20.0);
        assertThat(distance).isCloseTo(111194.9, within(1.0));
    }

    @Test
    void calculateDistance_shouldCalculateCorrectly_whenUsingRealCityCoordinates() {
        double warsaw_lat = 52.2297;
        double warsaw_lon = 21.0122;

        double krakow_lat = 50.0647;
        double krakow_lon = 19.9450;

        double distance = GeoUtils.calculateDistance(warsaw_lat, warsaw_lon, krakow_lat, krakow_lon);

        assertThat(distance).isCloseTo(252000.0, within(2000.0));
    }
}
