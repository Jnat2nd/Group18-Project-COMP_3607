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

final class InvocationEventAdapter implements InvocationFinishedListener {

    static final long CONSOLE_SETTLE_MS = 400;

    private static final int MAX_RESULT_CHARS = 100;

    private final ExecutionTelemetry telemetry;

    private final ConsoleTextSource console;

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
