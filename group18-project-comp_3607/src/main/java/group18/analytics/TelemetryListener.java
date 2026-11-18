package group18.analytics;

import group18.model.TelemetryEvent;

public interface TelemetryListener {
    void onEvent (TelemetryEvent event);
}
