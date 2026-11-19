package group18.analytics.calculators;

import java.util.List;

import group18.analytics.summary.TimeMetrics;
import group18.model.EditorEvent;
import group18.model.TelemetryEvent;

public class TimeMetricsCalculator implements MetricCalculator<TimeMetrics> {

    @Override
    public TimeMetrics calculate(List<TelemetryEvent> events) {
        List<EditorEvent> editor = Events.ofType(events, EditorEvent.class);
        return new TimeMetrics(
            editor.stream().mapToLong(EditorEvent::getActiveTimeMs).sum(),
            editor.stream().mapToLong(EditorEvent::getIdleTimeMs).sum(),
            editor.stream().mapToInt(EditorEvent::getCharacterEditCount).sum(),
            editor.size()
        );
    }

}
