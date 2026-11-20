package group18.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Editing activity measured over one interval that ends at {@link #timestamp()}.
 *
 * <p>Intervals normally last one sampling period (10 seconds). Consecutive intervals with no activity at all are
 * merged into a single idle interval, so a long break appears as one event rather than hundreds.</p>
 *
 * @param sessionId          the session the event belongs to
 * @param timestamp          the end of the interval
 * @param intervalMs         length of the interval in milliseconds
 * @param activeMs           part of the interval that counts as active coding time
 * @param idleMs             part of the interval that counts as idle time ({@code intervalMs - activeMs})
 * @param editCount          number of separate edits (keystrokes, deletions, pastes) in the interval
 * @param charactersTyped    characters inserted by short edits, which is what typing produces
 * @param charactersPasted   characters inserted by long single edits, which is what pasting produces
 * @param charactersDeleted  characters removed
 * @param classesEdited      names of the classes edited in the interval, sorted
 */
public record EditorEvent(String sessionId, Instant timestamp, long intervalMs, long activeMs, long idleMs,
                          int editCount, int charactersTyped, int charactersPasted, int charactersDeleted,
                          List<String> classesEdited) implements TelemetryEvent {

    /**
     * Validates the components and makes the class list immutable.
     *
     * @throws NullPointerException     if {@code sessionId} or {@code timestamp} is null
     * @throws IllegalArgumentException if any count or duration is negative, or active plus idle time exceeds the
     *                                  interval
     */
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

    /**
     * Creates an interval in which nothing happened.
     *
     * @param sessionId the session the event belongs to
     * @param end       the end of the idle interval
     * @param idleMs    the length of the idle interval in milliseconds
     * @return the new event
     */
    public static EditorEvent idle(String sessionId, Instant end, long idleMs) {
        return new EditorEvent(sessionId, end, idleMs, 0, idleMs, 0, 0, 0, 0, List.of());
    }

    @Override
    public EventType eventType() {
        return EventType.EDITOR_INTERACTION;
    }

    /**
     * Typing cadence for this interval: characters typed per minute of active time.
     *
     * @return characters per active minute rounded to one decimal place, or 0 if there was no active time
     */
    public double typingCadenceCharsPerMinute() {
        if (activeMs == 0) {
            return 0;
        }
        return Math.round(charactersTyped * 600_000.0 / activeMs) / 10.0;
    }

    /**
     * Whether nothing at all happened in this interval.
     *
     * @return {@code true} if there was no active time and no edit
     */
    public boolean idleOnly() {
        return activeMs == 0 && editCount == 0;
    }
}
