package group18.listener;

// Sources:
//  https://www.bluej.org/doc/extensionsAPI2/bluej/extensions2/event/InvocationFinishedEvent.html
//  https://www.bluej.org/doc/extensionsAPI2/bluej/extensions2/event/InvocationFinishedEvent.EventType.html

import group18.model.ExecutionEvent;
import group18.model.ExecutionEvent.ConsoleOutput;
import group18.model.ExecutionEvent.ExceptionDetails;
import group18.model.ExecutionEvent.ExitType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns finished invocations into {@link ExecutionEvent}s: the outcome, any runtime exception and a summary of
 * the console output the call produced.
 *
 * <p>BlueJ's terminal keeps the output of earlier calls, so the recorder remembers what the terminal held last
 * time and only summarises what was added. If the terminal was cleared in between, all of its text is new.</p>
 */
public final class ExecutionRecorder {

    /** Longest console summary kept before it is shortened with "...". */
    public static final int SUMMARY_CHARS = 200;

    /** An exception class name with an optional message, e.g. {@code java.lang.ArithmeticException: / by zero}. */
    private static final Pattern EXCEPTION = Pattern.compile(
            "\\b((?:[a-z_$][\\w$]*\\.)*[A-Z][\\w$]*(?:Exception|Error))\\b(?::[ \\t]*([^\\r\\n]*))?");

    /** A stack frame location such as {@code at Calculator.divide(Calculator.java:12)}. */
    private static final Pattern FRAME = Pattern.compile("\\bat\\s+[\\w$.<>/]+\\(([\\w$]+\\.java:\\d+)\\)");

    /** Session that the produced events belong to. */
    private final String sessionId;

    /** Receives the execution events. */
    private final EventSink sink;

    /** Terminal text seen after the previous call, one entry per pane. */
    private List<String> lastPanes = List.of();

    /**
     * Creates a recorder for one session.
     *
     * @param sessionId the session that produced events belong to
     * @param sink      receives the execution events
     */
    public ExecutionRecorder(String sessionId, EventSink sink) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    /**
     * Records a finished call.
     *
     * @param invocation the call reported by BlueJ
     */
    public void record(InvocationResult invocation) {
        String output = newOutput(invocation.consolePanes());
        ExitType exitType = ExitType.fromName(invocation.exitType());
        ExceptionDetails exception = exitType == ExitType.EXCEPTION_EXIT
                ? exceptionIn(output, invocation.result())
                : null;
        sink.publish(new ExecutionEvent(sessionId, invocation.timestamp(), invocation.className(),
                invocation.methodName(), exitType, exception, ConsoleOutput.of(output, SUMMARY_CHARS)));
    }

    /**
     * The text added to the terminal since the previous call.
     *
     * @param panes the current text of each terminal pane; an empty list means the terminal could not be read
     * @return the new text of all panes joined by line breaks
     */
    String newOutput(List<String> panes) {
        if (panes.isEmpty()) {
            return "";
        }
        StringJoiner added = new StringJoiner("\n");
        for (int i = 0; i < panes.size(); i++) {
            String now = panes.get(i);
            String before = i < lastPanes.size() ? lastPanes.get(i) : "";
            String text = now.startsWith(before) ? now.substring(before.length()) : now;
            if (!text.isBlank()) {
                added.add(text.strip());
            }
        }
        lastPanes = new ArrayList<>(panes);
        return added.toString();
    }

    /**
     * Reads the exception type, message and location from console output.
     *
     * @param output          the output of the call
     * @param fallbackMessage BlueJ's result text, used as the message if the console shows no exception
     * @return the exception details; the type is {@code unknown} if no exception appears in the output
     */
    static ExceptionDetails exceptionIn(String output, String fallbackMessage) {
        Matcher exception = EXCEPTION.matcher(output);
        if (!exception.find()) {
            return new ExceptionDetails("unknown", fallbackMessage, null);
        }
        String message = exception.group(2) == null || exception.group(2).isBlank()
                ? null : exception.group(2).strip();
        Matcher frame = FRAME.matcher(output);
        String location = frame.find(exception.end()) ? frame.group(1) : null;
        return new ExceptionDetails(exception.group(1), message, location);
    }
}
