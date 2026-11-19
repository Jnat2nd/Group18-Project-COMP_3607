package group18.analytics.calculators;

import java.util.Comparator;
import java.util.List;

import group18.model.LifecycleEvent;
import group18.model.TelemetryEvent;
import group18.model.TimelineEntry;


public final class TimelineCalculator implements MetricCalculator<List<TimelineEntry>> {

   
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
