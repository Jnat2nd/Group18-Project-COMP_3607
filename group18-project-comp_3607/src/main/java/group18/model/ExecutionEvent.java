package group18.model;

public class ExecutionEvent extends TelemetryEvent{
    private final boolean successful;
    private final long executionDurationMs;
    private final String targetClassName;
    private final String exceptionType;
    private final String exceptionMessage;
    private final String consoleOutputSummary;

    public ExecutionEvent(
        String sessionId,
        boolean successful,
        long executionDurationMs,
        String targetClassName,
        String exceptionType,
        String exceptionMessage,
        String consoleOutputSummary
    ) {
        super(sessionId);
        this.successful = successful;
        this.executionDurationMs = executionDurationMs;
        this.targetClassName = targetClassName;
        this.exceptionType = exceptionType;
        this.exceptionMessage = exceptionMessage;
        this.consoleOutputSummary = consoleOutputSummary != null ? consoleOutputSummary : "";
    }

    @Override 
    public String getEventType() {
        return "EXECUTION";
    }

    public boolean isSuccessful() { 
        return successful;
    }

    public long getExecutionDurationMs() {
        return executionDurationMs;
    }

    public String getTargetClass() {
        return targetClassName;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public String getConsoleOutputSummary() {
        return consoleOutputSummary;
    }
}
