package group18.model;

import java.time.Instant;
import java.util.Objects;

public record LifecycleEvent(String sessionId, Instant timestamp, Action action, String target, String detail)
        implements TelemetryEvent {

   
    public enum Action {
        PROJECT_CREATED,
        PROJECT_OPENED,
        PROJECT_CLOSED,
        PROJECT_DELETED,
        CLASS_CREATED,
        CLASS_MODIFIED,
        CLASS_DELETED,
       
        METHOD_ADDED,
       
        METHOD_REMOVED;

        public boolean isMilestone() {
            return this != CLASS_MODIFIED;
        }
    }

    public LifecycleEvent {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(target, "target");
    }

    public static LifecycleEvent of(String sessionId, Instant timestamp, Action action, String target) {
        return new LifecycleEvent(sessionId, timestamp, action, target, null);
    }

    @Override
    public EventType eventType() {
        return EventType.LIFECYCLE;
    }
}
