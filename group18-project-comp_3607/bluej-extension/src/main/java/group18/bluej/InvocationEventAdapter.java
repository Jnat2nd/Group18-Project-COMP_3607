package group18.bluej;

// Sources:
//  https://www.bluej.org/doc/extensionsAPI2/bluej/extensions2/event/InvocationFinishedEvent.html
//  https://www.bluej.org/doc/extensionsAPI2/bluej/extensions2/event/InvocationFinishedEvent.EventType.html

import bluej.extensions2.event.InvocationFinishedEvent;
import bluej.extensions2.event.InvocationFinishedListener;
import group18.facade.ExecutionTelemetry;
import group18.model.ProjectRef;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * <b>Adapter</b>: turns BlueJ's "invocation finished" callback (a method or constructor run from BlueJ) into an
 * execution call on the telemetry facade, with the text of the terminal so the console output and any exception
 * can be summarised.
 */
final class InvocationEventAdapter implements InvocationFinishedListener {

    /** How long to wait for the program's last output to reach the terminal before reading it. */
    static final long CONSOLE_SETTLE_MS = 400;

    /** Longest returned-value text kept. */
    private static final int MAX_RESULT_CHARS = 100;

    /** Receives the execution calls. */
    private final ExecutionTelemetry telemetry;

    /** Reads the terminal window. */
    private final ConsoleTextSource console;

    /**
     * Creates the adapter.
     *
     * @param telemetry receives the execution calls
     * @param console   reads the terminal window
     */
    InvocationEventAdapter(ExecutionTelemetry telemetry, ConsoleTextSource console) {
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry");
        this.console = Objects.requireNonNull(console, "console");
    }

    @Override
    public void invocationFinished(InvocationFinishedEvent event) {
        Optional<ProjectRef> project = BlueJProjects.refOf(event.getPackage());
        if (project.isEmpty()) {
            return;
        }
        String className = event.getClassName();
        String methodName = event.getMethodName();
        String exitType = event.getEventType() == null ? null : event.getEventType().name();
        FxThread.runAfter(CONSOLE_SETTLE_MS, () -> {
            List<String> panes = console.readPanes(project.get().name());
            telemetry.invocationFinished(project.get().key(), className == null ? "?" : className, methodName,
                    exitType, resultText(event, methodName), panes);
        });
    }

    /** BlueJ's returned value for a method call, shortened; {@code null} for constructors or if unavailable. */
    private static String resultText(InvocationFinishedEvent event, String methodName) {
        if (methodName == null) {
            return null;
        }
        try {
            Object result = event.getResult();
            if (result == null) {
                return null;
            }
            String text = String.valueOf(result);
            return text.length() > MAX_RESULT_CHARS ? text.substring(0, MAX_RESULT_CHARS) + "..." : text;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
