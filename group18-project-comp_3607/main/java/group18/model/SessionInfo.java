package group18.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Facts about a session that are fixed when it starts.
 *
 * <p>A session runs from the moment a project is opened in BlueJ until it is closed (or BlueJ exits). Each session
 * produces its own JSON and PDF files.</p>
 *
 * @param sessionId unique identifier of the session
 * @param studentId the student ID configured in BlueJ's preferences, or a placeholder
 * @param project   the project the session belongs to
 * @param startTime when the session started
 */
public record SessionInfo(String sessionId, String studentId, ProjectRef project, Instant startTime) {

    /** Value used for {@link #studentId()} until a student sets their ID in BlueJ's preferences. */
    public static final String STUDENT_ID_PLACEHOLDER = "STUDENT_ID_PLACEHOLDER";

    /**
     * Validates the components and substitutes the placeholder for a blank student ID.
     *
     * @throws NullPointerException if {@code sessionId}, {@code project} or {@code startTime} is null
     */
    public SessionInfo {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(startTime, "startTime");
        studentId = studentId == null || studentId.isBlank() ? STUDENT_ID_PLACEHOLDER : studentId.strip();
    }
}
