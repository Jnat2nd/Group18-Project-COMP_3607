package group18.analytics;

import group18.model.TelemetryEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A {@link TelemetryListener} that keeps every event it is told about, so the aggregator can be given a snapshot of
 * the session at any moment. Safe to read from another thread while events are being added.
 */
public final class TelemetryCollector implements TelemetryListener {

    /** The events received so far, in order. */
    private final List<TelemetryEvent> events = new CopyOnWriteArrayList<>();

    /** Creates an empty collector. */
    public TelemetryCollector() {
    }

    @Override
    public void onEvent(TelemetryEvent event) {
        if (event != null) {
            events.add(event);
        }
    }

    /**
     * The events received so far.
     *
     * @return an unmodifiable copy, in the order received
     */
    public List<TelemetryEvent> snapshot() {
        return List.copyOf(events);
    }

    /**
     * How many events have been received.
     *
     * @return the event count
     */
    public int size() {
        return events.size();
    }
}
