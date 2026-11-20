package group18.analytics.calculators;

import group18.model.LifecycleEvent;
import group18.model.LifecycleEvent.Action;
import group18.model.ReportSummary.LifecycleSummary;
import group18.model.TelemetryEvent;

import java.util.List;

/** Counts of classes created, modified and deleted, and methods added and removed. */
public final class LifecycleMetricsCalculator implements MetricCalculator<LifecycleSummary> {

    /** Creates the calculator. It has no state, so one instance can be reused. */
    public LifecycleMetricsCalculator() {
    }

    @Override
    public LifecycleSummary calculate(List<TelemetryEvent> events) {
        List<LifecycleEvent> lifecycle = Events.ofType(events, LifecycleEvent.class);
        return new LifecycleSummary(count(lifecycle, Action.CLASS_CREATED), count(lifecycle, Action.CLASS_MODIFIED),
                count(lifecycle, Action.CLASS_DELETED), count(lifecycle, Action.METHOD_ADDED),
                count(lifecycle, Action.METHOD_REMOVED));
    }

    private static int count(List<LifecycleEvent> events, Action action) {
        return (int) events.stream().filter(event -> event.action() == action).count();
    }
}
