package group18.model;

import java.time.Instant;
import java.util.Objects;

/**
 * One method or constructor call made from BlueJ, for example running {@code main} or calling a method on an
 * object on the object bench.
 *
 * @param sessionId  the session the event belongs to
 * @param timestamp  when the call finished
 * @param className  the class the call was made on
 * @param methodName the method called, or {@code null} when a constructor was called
 * @param exitType   how the call ended
 * @param exception  details of the runtime exception, or {@code null} if the call did not throw one
 * @param console    summary of the console output the call produced
 */
public record ExecutionEvent(String sessionId, Instant timestamp, String className, String methodName,
                             ExitType exitType, ExceptionDetails exception, ConsoleOutput console)
        implements TelemetryEvent {

    /** How an invocation ended. The names match BlueJ's {@code InvocationFinishedEvent.EventType}. */
    public enum ExitType {
        /** The call returned normally. */
        NORMAL_EXIT,
        /** The call ended with an uncaught exception. */
        EXCEPTION_EXIT,
        /** The call was stopped, for example by resetting the Java virtual machine or calling System.exit. */
        TERMINATED_EXIT,
        /** BlueJ could not tell how the call ended. */
        UNKNOWN_EXIT;

        /**
         * Converts BlueJ's name for an exit type.
         *
         * @param name a name such as {@code "NORMAL_EXIT"}; may be {@code null}
         * @return the matching exit type, or {@link #UNKNOWN_EXIT} if the name is null or not recognised
         */
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

    /**
     * A runtime exception thrown by the call.
     *
     * @param type     fully qualified exception class, for example {@code java.lang.ArithmeticException}, or
     *                 {@code unknown} if it could not be read from the console
     * @param message  the exception message, or {@code null}
     * @param location the first {@code File.java:line} of the stack trace, or {@code null}
     */
    public record ExceptionDetails(String type, String message, String location) {

        /**
         * Validates the components.
         *
         * @throws NullPointerException if {@code type} is null
         */
        public ExceptionDetails {
            Objects.requireNonNull(type, "type");
        }
    }

    /**
     * The console output the call produced.
     *
     * @param lineCount number of lines of output
     * @param summary   the start of the output, shortened with {@code ...} when it is long
     */
    public record ConsoleOutput(int lineCount, String summary) {

        /** Output of a call that printed nothing. */
        public static final ConsoleOutput NONE = new ConsoleOutput(0, "");

        /**
         * Summarises console text.
         *
         * @param text     the full output, may be {@code null}
         * @param maxChars the longest summary to keep before shortening
         * @return the summary
         */
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

    /**
     * Validates the components.
     *
     * @throws NullPointerException if {@code sessionId}, {@code timestamp}, {@code className} or {@code exitType}
     *                              is null
     */
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

    /**
     * Whether the call returned normally.
     *
     * @return {@code true} for {@link ExitType#NORMAL_EXIT}
     */
    public boolean successful() {
        return exitType == ExitType.NORMAL_EXIT;
    }

    /**
     * A readable description of what was called, such as {@code Calculator.add} or {@code new Calculator}.
     *
     * @return the description
     */
    public String invocation() {
        return methodName == null ? "new " + className : className + "." + methodName;
    }
}
