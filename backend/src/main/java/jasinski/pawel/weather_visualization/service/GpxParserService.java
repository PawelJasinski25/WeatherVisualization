package jasinski.pawel.weather_visualization.service;

import jasinski.pawel.weather_visualization.entity.TrackPoint;
import jasinski.pawel.weather_visualization.entity.Trip;
import jasinski.pawel.weather_visualization.utils.GeoUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class GpxParserService {

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    private byte[] fixGpxData(byte[] gpxBytes) {
        String gpxContent = new String(gpxBytes, StandardCharsets.UTF_8);
        StringBuilder repairedXml = new StringBuilder((int) (gpxContent.length() * 1.05));

        Deque<String> openTags = new ArrayDeque<>();

        // Tagi, które nie zawierają innych tagów wewnątrz
        Set<String> leafTags = Set.of("ele", "time", "name", "desc", "sym", "type", "fix", "speed");

        String[] tokens = gpxContent.split("(?=<)");

        for (String token : tokens) {
            if (!token.startsWith("<") || token.startsWith("<?") || token.startsWith("<!")) {
                repairedXml.append(token);
                continue;
            }

            boolean isClosing = token.startsWith("</");
            boolean isSelfClosing = token.endsWith("/>") || token.endsWith("/ >");
            String tagName = getTagName(token);

            if (tagName.isEmpty()) {
                repairedXml.append(token);
                continue;
            }

            if (isClosing) {
                if (openTags.contains(tagName)) {
                    while (!openTags.isEmpty()) {
                        String closedTag = openTags.pop();
                        if (closedTag.equals(tagName)) {
                            repairedXml.append(token);
                            break;
                        } else {
                            repairedXml.append("</").append(closedTag).append(">\n");
                        }
                    }
                }
            } else if (!isSelfClosing) {
                while (!openTags.isEmpty()) {
                    String currentOpenTag = openTags.peek();

                    boolean topIsLeaf = leafTags.contains(currentOpenTag);
                    boolean nestedTrackPoint = tagName.equals("trkpt") && currentOpenTag.equals("trkpt");
                    boolean invalidTrackSegmentNesting = tagName.equals("trkseg") && (currentOpenTag.equals("trkseg") || currentOpenTag.equals("trkpt"));

                    boolean requiresAutoClose = topIsLeaf || nestedTrackPoint || invalidTrackSegmentNesting;

                    if (requiresAutoClose) {
                        repairedXml.append("</").append(openTags.pop()).append(">\n");
                    } else {
                        break;
                    }
                }
                openTags.push(tagName);
                repairedXml.append(token);
            } else {
                repairedXml.append(token);
            }
        }

        while (!openTags.isEmpty()) {
            repairedXml.append("\n</").append(openTags.pop()).append(">");
        }

        return repairedXml.toString().getBytes(StandardCharsets.UTF_8);
    }


    public String getTagName(String token) {
        int start = token.startsWith("</") ? 2 : 1;

        if (token.length() <= start) {
            return "";
        }

        int end = start;
        while (end < token.length()) {
            char c = token.charAt(end);
            if (c == ' ' || c == '>' || c == '/')
                break;
            end++;
        }
        return token.substring(start, end).toLowerCase();
    }

    public List<TrackPoint> extractTrackPoints(Path tempFile, Trip savedTrip) throws Exception {
        try (InputStream is = new FileInputStream(tempFile.toFile())) {
            return parseAndFilterGpx(is, savedTrip);
        } catch (Exception e) {
            System.out.println("Plik GPX uszkodzony. Próbuję naprawić...");
            byte[] rawBytes = Files.readAllBytes(tempFile);
            byte[] repairedBytes = fixGpxData(rawBytes);
            try (InputStream is = new ByteArrayInputStream(repairedBytes)) {
                return parseAndFilterGpx(is, savedTrip);
            }
        }
    }

    public List<TrackPoint> parseAndFilterGpx(InputStream inputStream, Trip savedTrip) throws Exception {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

        XMLStreamReader reader = factory.createXMLStreamReader(inputStream);

        List<TrackPoint> optimizedPoints = new ArrayList<>();
        TrackPoint currentPoint = null;
        String currentTag = "";

        int segmentId = 0;
        TrackPoint lastSavedPoint = null;

        while (reader.hasNext()) {
            int event = reader.next();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    currentTag = reader.getLocalName().toLowerCase();
                    if ("trkseg".equals(currentTag)) {
                        segmentId++;
                    } else if ("trkpt".equals(currentTag)) {
                        currentPoint = new TrackPoint();
                        currentPoint.setTrip(savedTrip);
                        currentPoint.setSegmentId(segmentId);

                        double lat = Double.parseDouble(reader.getAttributeValue(null, "lat"));
                        double lon = Double.parseDouble(reader.getAttributeValue(null, "lon"));
                        currentPoint.setLocation(geometryFactory.createPoint(new Coordinate(lon, lat)));
                    }
                    break;

                case XMLStreamConstants.CHARACTERS:
                    String text = reader.getText().trim();
                    if (text.isEmpty() || currentPoint == null) break;

                    if ("time".equals(currentTag)) {
                        currentPoint.setTime(Instant.parse(text));
                    } else if ("ele".equals(currentTag)) {
                        currentPoint.setElevation(Double.parseDouble(text));
                    } else if ("speed".equals(currentTag) || "navionics_speed".equals(currentTag)) {
                        currentPoint.setSpeed(Double.parseDouble(text) * 3.6);
                    }
                    break;

                case XMLStreamConstants.END_ELEMENT:
                    String endTag = reader.getLocalName().toLowerCase();
                    if ("trkpt".equals(endTag) && currentPoint != null && currentPoint.getTime() != null) {

                        if (lastSavedPoint == null || !currentPoint.getSegmentId().equals(lastSavedPoint.getSegmentId())) {
                            optimizedPoints.add(currentPoint);
                            lastSavedPoint = currentPoint;
                        } else {
                            double distance = GeoUtils.calculateDistance(lastSavedPoint.getLocation(), currentPoint.getLocation());
                            long timeGap = Duration.between(lastSavedPoint.getTime(), currentPoint.getTime()).abs().getSeconds();

                            if (distance >= 10.0 || timeGap >= 30) {
                                optimizedPoints.add(currentPoint);
                                lastSavedPoint = currentPoint;
                            }
                        }
                        currentPoint = null;
                    }
                    currentTag = "";
                    break;
            }
        }
        reader.close();
        return optimizedPoints;
    }
}
