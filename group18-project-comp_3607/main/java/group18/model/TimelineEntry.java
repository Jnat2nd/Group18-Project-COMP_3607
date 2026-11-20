package group18.model;

import java.time.Instant;

/**
 * One step on the progression timeline: a project, class or method milestone.
 *
 * @param timestamp when it happened
 * @param action    the lifecycle action, for example {@code CLASS_CREATED} or {@code METHOD_ADDED}
 * @param target    the project or class name
 * @param detail    the method signature for method milestones, otherwise {@code null}
 */
public record TimelineEntry(Instant timestamp, LifecycleEvent.Action action, String target, String detail) {

    /**
     * Builds a timeline entry from a lifecycle event.
     *
     * @param event the lifecycle event
     * @return the matching timeline entry
     */
    public static TimelineEntry from(LifecycleEvent event) {
        return new TimelineEntry(event.timestamp(), event.action(), event.target(), event.detail());
    }
}
