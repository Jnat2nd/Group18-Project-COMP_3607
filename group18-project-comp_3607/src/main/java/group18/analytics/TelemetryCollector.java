package group18.analytics;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import group18.model.TelemetryEvent;

public class TelemetryCollector implements TelemetryListener{
    private final List<TelemetryEvent> events = new CopyOnWriteArrayList<>();

    @Override 
    public void onEvent(TelemetryEvent event) {
        if (event != null)
            events.add(event);
    }

    public List<TelemetryEvent> snapshot() {
        return List.copyOf(events);
    }
}
