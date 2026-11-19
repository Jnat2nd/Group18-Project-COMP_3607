package group18.listener;

import group18.model.TelemetryEvent;


@FunctionalInterface
public interface EventSink {

    void publish(TelemetryEvent event);
}
