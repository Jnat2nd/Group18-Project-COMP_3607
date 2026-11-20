package group18.model;

import java.time.Instant;

/**
 * Identifying information and headline totals for one exported session. This is the {@code metadata} object at
 * the top of the JSON file.
 *
 * @param schemaVersion            version of the JSON layout, so readers can detect format changes
 * @param generator                name and version of the plugin that wrote the file
 * @param sessionId                unique identifier of the session
 * @param studentId                student ID from BlueJ's preferences, or {@link SessionInfo#STUDENT_ID_PLACEHOLDER}
 * @param projectName              the BlueJ project name
 * @param startTime                when the session started
 * @param endTime                  when the report was generated
 * @param sessionDurationSeconds   seconds from start to end
 * @param totalCompilationAttempts number of compilations the user requested
 * @param totalEvents              number of events in the {@code events} array
 * @param exportReason             why the report was written
 */
public record SessionMetadata(String schemaVersion, String generator, String sessionId, String studentId,
                              String projectName, Instant startTime, Instant endTime, long sessionDurationSeconds,
                              int totalCompilationAttempts, int totalEvents, ExportReason exportReason) {
}
