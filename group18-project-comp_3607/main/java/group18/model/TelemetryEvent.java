package group18.model;

import java.time.Instant;

/**
 * A single time-stamped piece of telemetry belonging to one session.
 *
 * <p>Every concrete event is an immutable {@code record}. New kinds of telemetry are added by writing another
 * record that implements this interface; the aggregator, the exporters and the session code only depend on this
 * abstraction (Open/Closed and Dependency Inversion principles).</p>
 */
public interface TelemetryEvent {

    /**
     * Identifies the session the event belongs to.
     *
     * @return the session identifier, never {@code null}
     */
    String sessionId();

    /**
     * When the event happened.
     *
     * @return the event time, never {@code null}
     */
    Instant timestamp();

    /**
     * The category of the event, written to the JSON export as {@code eventType}.
     *
     * @return the event category
     */
    EventType eventType();
}
