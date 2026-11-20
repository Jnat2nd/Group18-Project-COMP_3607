package group18.listener;

import group18.model.TelemetryEvent;

/**
 * Receives the events produced by the trackers and recorders in this package.
 *
 * <p>The trackers only know this interface, not where events end up, which keeps them independent of sessions,
 * storage and BlueJ and lets the tests collect events in a plain list ({@code events::add}).</p>
 */
@FunctionalInterface
public interface EventSink {

    /**
     * Accepts one event.
     *
     * @param event the event to store or forward; never {@code null}
     */
    void publish(TelemetryEvent event);
}
