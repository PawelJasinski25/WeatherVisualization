package jasinski.pawel.weather_visualization.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DayData {
    public LocalDate date;
    public List<TimelineEvent> events = new ArrayList<>();
    public long movingSeconds = 0;
    public long stoppedSeconds = 0;
    public long gapSeconds = 0;

    public DayData(LocalDate date) {
        this.date = date;
    }

    public void addEvent(TimelineEvent e) {
        if (e.start().equals(e.end())) return;
        events.add(e);

        if ("RUCH".equals(e.type())) movingSeconds += e.durationSeconds();
        else if ("POSTÓJ".equals(e.type())) stoppedSeconds += e.durationSeconds();
        else if ("BRAK DANYCH".equals(e.type())) gapSeconds += e.durationSeconds();
    }
}