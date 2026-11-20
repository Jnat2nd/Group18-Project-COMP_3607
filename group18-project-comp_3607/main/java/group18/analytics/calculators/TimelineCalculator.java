package group18.analytics.calculators;

import group18.model.LifecycleEvent;
import group18.model.TelemetryEvent;
import group18.model.TimelineEntry;

import java.util.Comparator;
import java.util.List;

/**
 * The progression timeline: project, class and method milestones in time order. Class modifications are left out
 * because they happen every few seconds while typing.
 */
public final class TimelineCalculator implements MetricCalculator<List<TimelineEntry>> {

    /** Creates the calculator. It has no state, so one instance can be reused. */
    public TimelineCalculator() {
    }

    @Override
    public List<TimelineEntry> calculate(List<TelemetryEvent> events) {
        return Events.ofType(events, LifecycleEvent.class).stream()
                .filter(event -> event.action().isMilestone())
                .sorted(Comparator.comparing(LifecycleEvent::timestamp))
                .map(TimelineEntry::from)
                .toList();
    }
}
