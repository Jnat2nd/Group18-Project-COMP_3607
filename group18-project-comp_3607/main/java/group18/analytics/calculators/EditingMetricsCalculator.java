package group18.analytics.calculators;

import group18.model.EditorEvent;
import group18.model.ReportSummary.EditingSummary;
import group18.model.TelemetryEvent;

import java.util.List;

/** Editor interaction totals, typing cadence (characters typed per active minute) and edit frequency. */
public final class EditingMetricsCalculator implements MetricCalculator<EditingSummary> {

    /** Creates the calculator. It has no state, so one instance can be reused. */
    public EditingMetricsCalculator() {
    }

    @Override
    public EditingSummary calculate(List<TelemetryEvent> events) {
        List<EditorEvent> editor = Events.ofType(events, EditorEvent.class);
        long activeMs = editor.stream().mapToLong(EditorEvent::activeMs).sum();
        int edits = editor.stream().mapToInt(EditorEvent::editCount).sum();
        long typed = editor.stream().mapToLong(EditorEvent::charactersTyped).sum();
        long pasted = editor.stream().mapToLong(EditorEvent::charactersPasted).sum();
        long deleted = editor.stream().mapToLong(EditorEvent::charactersDeleted).sum();
        double activeMinutes = activeMs / 60_000.0;
        double cadence = activeMinutes == 0 ? 0 : Events.round1(typed / activeMinutes);
        double editsPerMinute = activeMinutes == 0 ? 0 : Events.round1(edits / activeMinutes);
        return new EditingSummary(edits, typed, pasted, deleted, cadence, editsPerMinute);
    }
}
