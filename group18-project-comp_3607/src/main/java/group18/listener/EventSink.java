package group18.listener;

import group18.model.TelemetryEvent;

public interface EventSink {

    void publish(TelemetryEvent event);
}
