package group18.analytics;

import group18.model.TelemetryEvent;

/**
 * Observes the events recorded in a session: the observer side of the <b>Observer</b> pattern.
 * {@link group18.session.ProjectSession} is the subject and notifies every registered listener of each new event.
 */
@FunctionalInterface
public interface TelemetryListener {

    /**
     * Called once for every event recorded, in the order recorded.
     *
     * @param event the new event; never {@code null}
     */
    void onEvent(TelemetryEvent event);
}
