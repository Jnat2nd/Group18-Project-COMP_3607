package group18.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;


public record CompilationEvent(String sessionId, Instant timestamp, boolean successful, List<String> files, List<CompilationError> errors) implements TelemetryEvent {

    
    public CompilationEvent {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(timestamp, "timestamp");
        files = files == null ? List.of() : List.copyOf(files);
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    @Override
    public EventType eventType() {
        return EventType.COMPILATION;
    }
    public int errorCount() {
        return errors.size();
    }
}
