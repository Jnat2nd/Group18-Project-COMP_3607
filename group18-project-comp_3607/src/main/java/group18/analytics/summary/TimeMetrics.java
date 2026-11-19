package group18.analytics.summary;

public record TimeMetrics(
    long activeMs,
    long idleMs,
    int characterEdits,
    int editEvents) {
    public long totalMs() {
        return activeMs + idleMs;
    }

    public double activeRatio() {
        return totalMs() == 0 ? 0.0 : (double) activeMs/totalMs();
    }

    //typing cadence calculation (characters per minute of active time):
    public double charsPerMinute() {
        return activeMs == 0 ? 0.0 : characterEdits/(activeMs/60000.0);
    }
}
