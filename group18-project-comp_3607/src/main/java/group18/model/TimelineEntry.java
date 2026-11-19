package group18.model;

import java.time.Instant;

public record TimelineEntry(Instant timestamp, LifecycleEvent.Action action, String target, String detail) {

    public static TimelineEntry from(LifecycleEvent event) {
        return new TimelineEntry(event.timestamp(), event.action(), event.target(), event.detail());
    }
}
