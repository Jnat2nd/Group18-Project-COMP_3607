package group18.model;

import java.util.List;
import java.util.Objects;

/**
 * Everything exported for one session: the root object of the JSON file and the input of the PDF report.
 *
 * @param metadata identifying information and headline totals
 * @param summary  figures calculated from the events
 * @param timeline project, class and method milestones in time order
 * @param events   every recorded event in time order
 */
public record SessionReport(SessionMetadata metadata, ReportSummary summary, List<TimelineEntry> timeline,
                            List<TelemetryEvent> events) {

    /**
     * Validates the components and makes the lists immutable.
     *
     * @throws NullPointerException if {@code metadata} or {@code summary} is null
     */
    public SessionReport {
        Objects.requireNonNull(metadata, "metadata");
        Objects.requireNonNull(summary, "summary");
        timeline = timeline == null ? List.of() : List.copyOf(timeline);
        events = events == null ? List.of() : List.copyOf(events);
    }
}
