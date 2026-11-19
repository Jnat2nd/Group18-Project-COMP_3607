package group18.model;

import java.util.List;
import java.util.Objects;


public record SessionReport(SessionMetadata metadata, ReportSummary summary, List<TimelineEntry> timeline,List<TelemetryEvent> events) {

    public SessionReport {
        Objects.requireNonNull(metadata, "metadata");
        Objects.requireNonNull(summary, "summary");
        timeline = timeline == null ? List.of() : List.copyOf(timeline);
        events = events == null ? List.of() : List.copyOf(events);
    }
}
