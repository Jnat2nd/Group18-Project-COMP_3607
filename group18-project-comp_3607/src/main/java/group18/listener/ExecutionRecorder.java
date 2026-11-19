package group18.listener;

// Sources:
//  https://www.bluej.org/doc/extensionsAPI2/bluej/extensions2/event/InvocationFinishedEvent.html
//  https://www.bluej.org/doc/extensionsAPI2/bluej/extensions2/event/InvocationFinishedEvent.EventType.html

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import group18.model.ExecutionEvent;
import group18.model.ExecutionEvent.ConsoleOutput;
import group18.model.ExecutionEvent.ExceptionDetails;
import group18.model.ExecutionEvent.ExitType;


public final class ExecutionRecorder {
    public static final int SUMMARY_CHARS = 200;
    private static final Pattern EXCEPTION = Pattern.compile(
            "\\b((?:[a-z_$][\\w$]*\\.)*[A-Z][\\w$]*(?:Exception|Error))\\b(?::[ \\t]*([^\\r\\n]*))?");

    private static final Pattern FRAME = Pattern.compile("\\bat\\s+[\\w$.<>/]+\\(([\\w$]+\\.java:\\d+)\\)");
    private final String sessionId;
    private final EventSink sink;
    private List<String> lastPanes = List.of();

    
    public ExecutionRecorder(String sessionId, EventSink sink) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.sink = Objects.requireNonNull(sink, "sink");
    }

   
    public void record(InvocationResult invocation) {
        String output = newOutput(invocation.consolePanes());
        ExitType exitType = ExitType.fromName(invocation.exitType());
        ExceptionDetails exception = exitType == ExitType.EXCEPTION_EXIT
                ? exceptionIn(output, invocation.result())
                : null;
        sink.publish(new ExecutionEvent(sessionId, invocation.timestamp(), invocation.className(),
                invocation.methodName(), exitType, exception, ConsoleOutput.of(output, SUMMARY_CHARS)));
    }

   
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

   
    static ExceptionDetails exceptionIn(String output, String fallbackMessage) {
        Matcher exception = EXCEPTION.matcher(output);
        if (!exception.find()) {
            return new ExceptionDetails("unknown", fallbackMessage, null);
        }
        String message = exception.group(2) == null || exception.group(2).isBlank()? null : exception.group(2).strip();
        Matcher frame = FRAME.matcher(output);
        String location = frame.find(exception.end()) ? frame.group(1) : null;
        return new ExceptionDetails(exception.group(1), message, location);
    }
}
