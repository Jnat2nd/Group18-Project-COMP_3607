package group18.listener;
// Sources:
//  https://www.bluej.org/doc/extensionsAPI2/bluej/extensions2/event/InvocationFinishedEvent.EventType.html
//  https://www.bluej.org/doc/extensionsAPI2/bluej/extensions2/event/CompileListener.html

import group18.model.CompilationError;
import group18.model.CompilationEvent;
import group18.model.ExecutionEvent;
import group18.model.TelemetryEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns plain values from BlueJ's compile and invocation callbacks into {@link TelemetryEvent}s.
 * It has no BlueJ imports, so it is tested without BlueJ.
 */
public class BlueJEventRecorder {

    static final int CONSOLE_SUMMARY_CHARS = 200;

    /** "java.lang.ArithmeticException: / by zero" gives type and message. */
    private static final Pattern EXCEPTION =
            Pattern.compile("\\b((?:[a-z_]\\w*\\.)*[A-Z]\\w*(?:Exception|Error))\\b(?::[ \\t]*([^\\n]*))?");

    private final String sessionId;
    private final Consumer<TelemetryEvent> sink;
    private final List<CompilationError> errors = new ArrayList<>();

    public BlueJEventRecorder(String sessionId, Consumer<TelemetryEvent> sink) {
        this.sessionId = sessionId;
        this.sink = sink;
    }

    /** A new compile attempt starts: forget errors left over from an attempt that never finished. */
    public synchronized void compileStarted() {
        errors.clear();
    }

    /** BlueJ reports each compiler error separately, before the attempt finishes. */
    public synchronized void compileError(int line, String message) {
        String text = message == null ? "" : message;
        errors.add(new CompilationError(line, ErrorCodes.code(text), text));
    }

    /** One {@link CompilationEvent} per attempt, carrying the errors collected since it started. */
    public synchronized void compileFinished(boolean success) {
        sink.accept(new CompilationEvent(sessionId, success, List.copyOf(errors)));
        errors.clear();
    }

    /**
     * @param exitType BlueJ's invocation result name: NORMAL_EXIT, EXCEPTION_EXIT, TERMINATED_EXIT, UNKNOWN_EXIT
     * @param console  text of BlueJ's Terminal, or null if it could not be read
     */
    public void executionFinished(String className, String exitType, String result, String console) {
        String text = console == null ? "" : console.strip();
        String exceptionType = null;
        String exceptionMessage = null;
        if ("EXCEPTION_EXIT".equals(exitType)) {
            Matcher m = EXCEPTION.matcher(text);
            if (m.find()) {
                exceptionType = m.group(1);
                exceptionMessage = m.group(2);
            } else {
                exceptionType = "unknown";
                exceptionMessage = result;
            }
        }
        String summary = text.length() > CONSOLE_SUMMARY_CHARS ? text.substring(0, CONSOLE_SUMMARY_CHARS) + "..." : text;
        // shortcut: duration is 0 because BlueJ has no "invocation started" event; upgrade if one appears
        sink.accept(new ExecutionEvent(sessionId, "NORMAL_EXIT".equals(exitType), 0, className,
                exceptionType, exceptionMessage, summary));
    }
}
