package group18.model;

import java.time.Instant;

public record SessionMetadata(String schemaVersion, String generator, String sessionId, String studentId,String projectName, Instant startTime, Instant endTime, long sessionDurationSeconds,int totalCompilationAttempts, int totalEvents, ExportReason exportReason) {


}
