package group18.model;

import java.time.Instant;
import java.util.Objects;


public record ExecutionEvent(String sessionId, Instant timestamp, String className, String methodName,
                             ExitType exitType, ExceptionDetails exception, ConsoleOutput console)
        implements TelemetryEvent {


    public enum ExitType {
        NORMAL_EXIT,
        EXCEPTION_EXIT,
        TERMINATED_EXIT,
        UNKNOWN_EXIT;

        public static ExitType fromName(String name) {
            if (name != null) {
                for (ExitType type : values()) {
                    if (type.name().equals(name)) {
                        return type;
                    }
                }
            }
            return UNKNOWN_EXIT;
        }
    }

    
    public record ExceptionDetails(String type, String message, String location) {

        public ExceptionDetails {
            Objects.requireNonNull(type, "type");
        }
    }

   
    public record ConsoleOutput(int lineCount, String summary) {

        
        public static final ConsoleOutput NONE = new ConsoleOutput(0, "");

        
        public static ConsoleOutput of(String text, int maxChars) {
            String output = text == null ? "" : text.strip();
            if (output.isEmpty()) {
                return NONE;
            }
            int lines = output.split("\\R").length;
            String summary = output.length() > maxChars ? output.substring(0, maxChars) + "..." : output;
            return new ConsoleOutput(lines, summary);
        }
    }

    
    public ExecutionEvent {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(className, "className");
        Objects.requireNonNull(exitType, "exitType");
        console = console == null ? ConsoleOutput.NONE : console;
    }

    @Override
    public EventType eventType() {
        return EventType.EXECUTION;
    }

    
    public boolean successful() {
        return exitType == ExitType.NORMAL_EXIT;
    }

    
    public String invocation() {
        return methodName == null ? "new " + className : className + "." + methodName;
    }
}
