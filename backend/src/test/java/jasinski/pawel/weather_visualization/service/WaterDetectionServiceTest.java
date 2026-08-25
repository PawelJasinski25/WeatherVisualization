package jasinski.pawel.weather_visualization.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class WaterDetectionServiceTest {

    @InjectMocks
    private WaterDetectionService waterDetectionService;

    private final GeometryFactory factory = new GeometryFactory();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(waterDetectionService, "waterGeometry", null);
    }

    @Test
    void isWater_shouldReturnFalse_whenGeometryIsNull() {
        boolean result = waterDetectionService.isWater(54.4, 18.5);
        assertThat(result).isFalse();
    }

    @Test
    void isWater_shouldReturnTrue_whenPointIsInsideWater() {
        setupMockGeometry();

        boolean result = waterDetectionService.isWater(5.0, 5.0);
        assertThat(result).isTrue();
    }

    @Test
    void isWater_shouldReturnFalse_whenPointIsOutsideWater() {
        setupMockGeometry();

        boolean result = waterDetectionService.isWater(20.0, 20.0);
        assertThat(result).isFalse();
    }

    @Test
    void isWater_shouldHandleCoordinates_whenPointIsInsidePolygon() {
        setupMockGeometry();

        boolean result = waterDetectionService.isWater(2.0, 2.0);
        assertThat(result).isTrue();
    }

    private void setupMockGeometry() {
        Polygon mockSea = factory.createPolygon(new Coordinate[]{
                new Coordinate(0, 0), new Coordinate(10, 0),
                new Coordinate(10, 10), new Coordinate(0, 10), new Coordinate(0, 0)
        });
        ReflectionTestUtils.setField(waterDetectionService, "waterGeometry", PreparedGeometryFactory.prepare(mockSea));
    }
}
