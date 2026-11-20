package group18.model;

import java.time.Instant;
import java.util.Objects;

/**
 * A project, class or method was created, opened, modified, closed or deleted.
 *
 * @param sessionId the session the event belongs to
 * @param timestamp when the change was detected
 * @param action    what happened
 * @param target    the project name for project actions, otherwise the class name (for example {@code Dog} or
 *                  {@code animals.Dog})
 * @param detail    the method signature for method actions, for example {@code bark(int)}; {@code null} otherwise
 */
public record LifecycleEvent(String sessionId, Instant timestamp, Action action, String target, String detail)
        implements TelemetryEvent {

    /** The kinds of lifecycle change the plugin detects. */
    public enum Action {
        /** A project that did not exist shortly before was opened, so it was created in this session. */
        PROJECT_CREATED,
        /** An existing project was opened in BlueJ. */
        PROJECT_OPENED,
        /** The project was closed, or BlueJ was closed while the project was open. */
        PROJECT_CLOSED,
        /** The project folder disappeared while the project was open. */
        PROJECT_DELETED,
        /** A class appeared in the project. */
        CLASS_CREATED,
        /** The source code of a class changed. */
        CLASS_MODIFIED,
        /** A class was removed from the project. */
        CLASS_DELETED,
        /** A method or constructor appeared in a class. */
        METHOD_ADDED,
        /** A method or constructor disappeared from a class that still exists. */
        METHOD_REMOVED;

        /**
         * Whether this action is shown on the progression timeline of the report. Every action except
         * {@link #CLASS_MODIFIED} is a milestone; modifications happen every few seconds while typing and are
         * summarised as counts instead.
         *
         * @return {@code true} if the action belongs on the timeline
         */
        public boolean isMilestone() {
            return this != CLASS_MODIFIED;
        }
    }

    /**
     * Validates the components.
     *
     * @throws NullPointerException if {@code sessionId}, {@code timestamp}, {@code action} or {@code target} is null
     */
    public LifecycleEvent {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(target, "target");
    }

    /**
     * Creates an event that has no method detail.
     *
     * @param sessionId the session the event belongs to
     * @param timestamp when the change was detected
     * @param action    what happened
     * @param target    the project or class name
     * @return the new event
     */
    public static LifecycleEvent of(String sessionId, Instant timestamp, Action action, String target) {
        return new LifecycleEvent(sessionId, timestamp, action, target, null);
    }

    @Override
    public EventType eventType() {
        return EventType.LIFECYCLE;
    }
}
