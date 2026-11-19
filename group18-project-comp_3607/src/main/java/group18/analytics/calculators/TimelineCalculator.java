package group18.analytics.calculators;

import java.util.Comparator;
import java.util.List;

import group18.analytics.summary.TimelineEntry;
import group18.model.LifecycleEvent;
import group18.model.TelemetryEvent;

public class TimelineCalculator implements MetricCalculator<List<TimelineEntry>>{

    @Override
    public List<TimelineEntry> calculate(List<TelemetryEvent> events) {
        return Events.ofType(events, LifecycleEvent.class).stream()
            .sorted(Comparator.comparing(TelemetryEvent::getTimestamp))
            .map(e -> new TimelineEntry(e.getTimestamp(), e.getActionType(), e.getTargetName()))
            .toList();
        }

}
