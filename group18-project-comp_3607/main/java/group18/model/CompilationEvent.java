package group18.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * One compilation requested by the user (BlueJ's automatic background checks are not recorded).
 *
 * @param sessionId  the session the event belongs to
 * @param timestamp  when the compilation finished
 * @param successful whether the compilation produced class files
 * @param files      names of the source files involved in the attempt
 * @param errors     the errors reported during the attempt, in the order BlueJ reported them
 */
public record CompilationEvent(String sessionId, Instant timestamp, boolean successful, List<String> files,
                               List<CompilationError> errors) implements TelemetryEvent {

    /**
     * Validates the components and makes the lists immutable.
     *
     * @throws NullPointerException if {@code sessionId} or {@code timestamp} is null
     */
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

    /**
     * The number of errors reported in this attempt.
     *
     * @return the error count
     */
    public int errorCount() {
        return errors.size();
    }
}
