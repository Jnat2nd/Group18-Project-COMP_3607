package group18.analytics.summary;

import java.util.Map;

public record ExecutionMetrics(
    int totalRuns,
    int successfulRuns,
    int failedRuns,
    long totalRuntimeMs,
    Map<String, Long> exceptionFrequency
) {
    public double successRate() {
        return totalRuns == 0 ? 0.0 : (double) successfulRuns / totalRuns;
    }

    public double averageRuntimeMs() {
        return totalRuns == 0 ? 0.0 : (double) totalRuntimeMs / totalRuns;
    }
}
