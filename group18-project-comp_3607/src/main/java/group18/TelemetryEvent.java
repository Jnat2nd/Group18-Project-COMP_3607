package group18;

import java.time.Instant;

public abstract class TelemetryEvent {
    private final String sessionId;
    private final Instant timestamp;

    protected TelemetryEvent (String sessionId) {
        this.sessionId = sessionId;
        this.timestamp = Instant.now();
    }

    public String getSessionId() { 
        return sessionId; 
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public abstract String getEventType();
}


