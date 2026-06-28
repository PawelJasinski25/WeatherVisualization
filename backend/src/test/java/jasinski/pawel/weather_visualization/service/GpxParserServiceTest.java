package jasinski.pawel.weather_visualization.service;

import jasinski.pawel.weather_visualization.entity.TrackPoint;
import jasinski.pawel.weather_visualization.entity.Trip;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GpxParserServiceTest {

    @InjectMocks
    private GpxParserService gpxParserService;

    private Trip mockTrip;

    @BeforeEach
    void setUp() {
        mockTrip = new Trip();
        mockTrip.setId(1L);
    }

    @Test
    void parseAndFilterGpx_shouldExtractPoints_andApplyFilteringLogic() throws Exception {
        String gpxXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <gpx version="1.1">
                  <trk>
                    <trkseg>
                      <trkpt lat="52.00000" lon="21.00000"><time>2023-01-01T10:00:00Z</time></trkpt>
                      <trkpt lat="52.00001" lon="21.00001"><time>2023-01-01T10:00:00Z</time></trkpt>
                      <trkpt lat="52.00001" lon="21.00001"><time>2023-01-01T10:01:00Z</time></trkpt>
                    </trkseg>
                  </trk>
                </gpx>
                """;

        InputStream is = new ByteArrayInputStream(gpxXml.getBytes(StandardCharsets.UTF_8));

        List<TrackPoint> points = gpxParserService.parseAndFilterGpx(is, mockTrip);

        assertThat(points).hasSize(2);
        assertThat(points.get(0).getTime().toString()).isEqualTo("2023-01-01T10:00:00Z");
        assertThat(points.get(1).getTime().toString()).isEqualTo("2023-01-01T10:01:00Z");
    }

    @Test
    void parseAndFilterGpx_shouldExtractElevationAndSpeed() throws Exception {
        String gpxXml = """
                <gpx><trk><trkseg>
                   <trkpt lat="52.2" lon="21.0">
                     <ele>110.5</ele>
                     <speed>5.0</speed>
                     <time>2023-05-10T10:00:00Z</time>
                   </trkpt>
                </trkseg></trk></gpx>
                """;
        InputStream is = new ByteArrayInputStream(gpxXml.getBytes(StandardCharsets.UTF_8));

        List<TrackPoint> points = gpxParserService.parseAndFilterGpx(is, mockTrip);


        assertThat(points.get(0).getElevation()).isEqualTo(110.5);
        assertThat(points.get(0).getSpeed()).isEqualTo(18.0);
    }

    @Test
    void fixGpxData_shouldRepairUnclosedTags() {
        String validToken = "<trkpt lat=\"1\" lon=\"2\">";
        String invalidToken = "</trkpt>";

        assertThat(gpxParserService.getTagName(validToken)).isEqualTo("trkpt");
        assertThat(gpxParserService.getTagName(invalidToken)).isEqualTo("trkpt");
    }
}
