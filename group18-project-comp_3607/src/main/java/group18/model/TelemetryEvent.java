package group18.model;

import java.time.Instant;

public interface TelemetryEvent {

    String sessionId();

    Instant timestamp();

    EventType eventType();
}
