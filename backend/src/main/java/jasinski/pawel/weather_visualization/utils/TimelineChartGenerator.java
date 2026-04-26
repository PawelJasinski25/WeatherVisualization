package jasinski.pawel.weather_visualization.utils;

import jasinski.pawel.weather_visualization.dto.TimelineEvent;
import jasinski.pawel.weather_visualization.service.GeoNamesService;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TimelineChartGenerator {

    private static final int WIDTH = 1400;
    private static final int HEIGHT = 200;
    private static final int MARGIN_SIDE = 50;
    private static final int PLOT_WIDTH = WIDTH - (2 * MARGIN_SIDE);

    private static final int AXIS_Y = 120;

    // Możemy przygotować "pędzle" z opcją CAP_BUTT, żeby linie ucinały się idealnie na współrzędnych
    private static final Stroke THICK_STROKE = new BasicStroke(8, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
    private static final Stroke THIN_STROKE = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
    private static final Stroke LINE_STROKE = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);

    public static byte[] generateDailyTimelineChart(LocalDate date, List<TimelineEvent> events, ZoneId zoneId, GeoNamesService geoNamesService) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Tło i tytuł
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, WIDTH, HEIGHT);

            g2d.setColor(Color.DARK_GRAY);
            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            g2d.drawString(date.toString(), MARGIN_SIDE, 30);

            // Oś główna
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.setStroke(THIN_STROKE);
            g2d.drawLine(MARGIN_SIDE, AXIS_Y, WIDTH - MARGIN_SIDE, AXIS_Y);

            if (events == null || events.isEmpty()) {
                return toByteArray(image);
            }

            ZonedDateTime startOfDay = date.atStartOfDay(zoneId);
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

            int lastTopTextEndX = MARGIN_SIDE;
            int lastBottomTextEndX = MARGIN_SIDE - 20;

            Font typeFont = new Font("Arial", Font.BOLD, 11);
            Font placeFont = new Font("Arial", Font.ITALIC, 10);
            Font timeFont = new Font("Arial", Font.PLAIN, 12);

            FontMetrics fmType = g2d.getFontMetrics(typeFont);
            FontMetrics fmPlace = g2d.getFontMetrics(placeFont);
            FontMetrics fmTime = g2d.getFontMetrics(timeFont);

            for (int i = 0; i < events.size(); i++) {
                TimelineEvent ev = events.get(i);

                long startSeconds = Duration.between(startOfDay.toInstant(), ev.start()).getSeconds();
                long endSeconds = Duration.between(startOfDay.toInstant(), ev.end()).getSeconds();

                startSeconds = Math.max(0, Math.min(startSeconds, 86400));
                endSeconds = Math.max(0, Math.min(endSeconds, 86400));

                int x1 = MARGIN_SIDE + (int) ((startSeconds / 86400.0) * PLOT_WIDTH);
                int x2 = MARGIN_SIDE + (int) ((endSeconds / 86400.0) * PLOT_WIDTH);
                int centerX = (x1 + x2) / 2;

                // --- 1. RYSOWANIE LINII FAZY ---
                if ("RUCH".equals(ev.type())) {
                    g2d.setColor(new Color(34, 139, 34));
                } else if ("POSTÓJ".equals(ev.type())) {
                    g2d.setColor(Color.BLUE);
                } else {
                    g2d.setColor(new Color(160, 160, 160)); // Szary
                }

                // NOWE: Używamy pędzla THICK_STROKE (tnie kolor idealnie równo z krawędzią)
                g2d.setStroke(THICK_STROKE);
                g2d.drawLine(x1, AXIS_Y, x2, AXIS_Y);

                // Znacznik początku na osi
                g2d.setColor(Color.DARK_GRAY);
                g2d.setStroke(THIN_STROKE);
                g2d.drawLine(x1, AXIS_Y - 6, x1, AXIS_Y + 6); // Zrobiłem go minimalnie dłuższym (z 5 na 6), by wyglądał jeszcze ostrzej

                // --- 2. NAPISY GÓRNE (WYCENTROWANE NA ŚRODKU FAZY) ---
                String typeStr = ev.type();
                String placeStr = "";

                if ("POSTÓJ".equals(ev.type()) && ev.lat() != 0.0 && ev.lon() != 0.0 && geoNamesService != null) {
                    String p = geoNamesService.getPlaceName(ev.lat(), ev.lon());
                    if (p != null && !p.equals("--")) {
                        if (p.matches(".*\\d+\\.\\d{4,}.*")) {
                            placeStr = String.format("%.2f, %.2f", ev.lat(), ev.lon());
                        } else {
                            placeStr = p;
                        }
                    }
                }

                int w1 = fmType.stringWidth(typeStr);
                int w2 = placeStr.isEmpty() ? 0 : fmPlace.stringWidth(placeStr);
                int textBlockWidth = Math.max(w1, w2);

                int desiredTopX = centerX - (textBlockWidth / 2);
                int actualTopX = Math.max(desiredTopX, lastTopTextEndX + 15);

                if (actualTopX > desiredTopX + 5) {
                    g2d.setColor(Color.LIGHT_GRAY);
                    g2d.setStroke(LINE_STROKE);
                    int textCenterActual = actualTopX + (textBlockWidth / 2);
                    g2d.drawLine(centerX, AXIS_Y - 8, textCenterActual, AXIS_Y - 14);
                }

                if ("RUCH".equals(ev.type())) { g2d.setColor(new Color(34, 139, 34)); }
                else if ("POSTÓJ".equals(ev.type())) { g2d.setColor(Color.BLUE); }
                else { g2d.setColor(Color.GRAY); }

                g2d.setFont(typeFont);
                g2d.drawString(typeStr, actualTopX, AXIS_Y - 15);

                if (!placeStr.isEmpty()) {
                    g2d.setFont(placeFont);
                    g2d.setColor(Color.DARK_GRAY);
                    g2d.drawString(placeStr, actualTopX, AXIS_Y - 28);
                }

                lastTopTextEndX = actualTopX + textBlockWidth;

                // --- 3. NAPISY DOLNE (Godziny) ZAKOTWICZONE DO STARTU FAZY ---
                String timeStr = timeFormatter.format(ev.start().atZone(zoneId));
                int timeWidth = fmTime.stringWidth(timeStr);

                int desiredBotX = x1 - (timeWidth / 2);
                int actualBotX = Math.max(desiredBotX, lastBottomTextEndX + 15);

                if (actualBotX > desiredBotX + 5) {
                    g2d.setColor(Color.LIGHT_GRAY);
                    g2d.setStroke(LINE_STROKE);
                    g2d.drawLine(x1, AXIS_Y + 8, actualBotX + (timeWidth / 2), AXIS_Y + 18);
                }

                g2d.setColor(Color.BLACK);
                g2d.setFont(timeFont);
                g2d.drawString(timeStr, actualBotX, AXIS_Y + 32);

                lastBottomTextEndX = actualBotX + timeWidth;

                // --- 4. OSTATNIA GODZINA DNIA ---
                if (i == events.size() - 1) {
                    g2d.setColor(Color.DARK_GRAY);
                    g2d.setStroke(THIN_STROKE);
                    g2d.drawLine(x2, AXIS_Y - 6, x2, AXIS_Y + 6);

                    String endTimeStr = timeFormatter.format(ev.end().atZone(zoneId));
                    int endWidth = fmTime.stringWidth(endTimeStr);

                    int desiredEndX = x2 - (endWidth / 2);
                    int actualEndX = Math.max(desiredEndX, lastBottomTextEndX + 15);

                    if (actualEndX > desiredEndX + 5) {
                        g2d.setColor(Color.LIGHT_GRAY);
                        g2d.setStroke(LINE_STROKE);
                        g2d.drawLine(x2, AXIS_Y + 8, actualEndX + (endWidth / 2), AXIS_Y + 18);
                    }

                    g2d.setColor(Color.BLACK);
                    g2d.drawString(endTimeStr, actualEndX, AXIS_Y + 32);
                }
            }

        } finally {
            g2d.dispose();
        }

        return toByteArray(image);
    }

    private static byte[] toByteArray(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Błąd przy generowaniu wykresu PNG", e);
        }
    }
}