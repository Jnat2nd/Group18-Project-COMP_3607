package group18.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record EditorEvent(String sessionId, Instant timestamp, long intervalMs, long activeMs, long idleMs,int editCount, int charactersTyped, int charactersPasted, int charactersDeleted,List<String> classesEdited) implements TelemetryEvent {

    public EditorEvent {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(timestamp, "timestamp");
        if (intervalMs < 0 || activeMs < 0 || idleMs < 0 || editCount < 0
                || charactersTyped < 0 || charactersPasted < 0 || charactersDeleted < 0) {
            throw new IllegalArgumentException("durations and counts must not be negative");
        }
        if (activeMs + idleMs > intervalMs) {
            throw new IllegalArgumentException("active and idle time cannot exceed the interval");
        }
        classesEdited = classesEdited == null ? List.of() : List.copyOf(classesEdited);
    }


    public static EditorEvent idle(String sessionId, Instant end, long idleMs) {
        return new EditorEvent(sessionId, end, idleMs, 0, idleMs, 0, 0, 0, 0, List.of());
    }

    @Override
    public EventType eventType() {
        return EventType.EDITOR_INTERACTION;
    }

    public double typingCadenceCharsPerMinute() {
        if (activeMs == 0) {
            return 0;
        }
        return Math.round(charactersTyped * 600_000.0 / activeMs) / 10.0;
    }


    public boolean idleOnly() {
        return activeMs == 0 && editCount == 0;
    }
}
