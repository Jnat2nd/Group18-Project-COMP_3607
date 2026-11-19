package group18.model;

import java.time.Instant;
import java.util.Objects;


public record SessionInfo(String sessionId, String studentId, ProjectRef project, Instant startTime) {
    public static final String STUDENT_ID_PLACEHOLDER = "STUDENT_ID_PLACEHOLDER";
    public SessionInfo {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(startTime, "startTime");
        studentId = studentId == null || studentId.isBlank() ? STUDENT_ID_PLACEHOLDER : studentId.strip();
    }
}
