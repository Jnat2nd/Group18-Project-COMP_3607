package group18.analytics.summary;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record SessionSummary(
    String sessionId,
    String studentId,
    Instant start,
    Instant end,
    TimeMetrics time,
    CompilationMetrics compilation,
    ExecutionMetrics execution,
    List<TimelineEntry> timeline
) {
    public Duration sessionDuration() {
        return Duration.between(start, end);
    }
    
}
