package group18.export;

import group18.analytics.TelemetryAggregator;
import group18.model.CompilationEvent;
import group18.model.EditorEvent;
import group18.model.ExecutionEvent;
import group18.model.ExecutionEvent.ConsoleOutput;
import group18.model.ExecutionEvent.ExceptionDetails;
import group18.model.ExecutionEvent.ExitType;
import group18.model.ExportReason;
import group18.model.LifecycleEvent.Action;
import group18.model.ProjectRef;
import group18.model.SessionInfo;
import group18.model.SessionReport;
import group18.model.TelemetryEvent;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static group18.testsupport.TestData.START;
import static group18.testsupport.TestData.at;
import static group18.testsupport.TestData.compile;
import static group18.testsupport.TestData.editor;
import static group18.testsupport.TestData.error;
import static group18.testsupport.TestData.lifecycle;


final class SampleReports {

    static final String TRICKY_MESSAGE = "cannot find symbol\n  symbol: \"x\\y\" é\t";

    private SampleReports() {
    }

    static SessionReport typical(String studentId) {
        List<TelemetryEvent> events = new ArrayList<>(List.of(
                lifecycle(0, Action.PROJECT_OPENED, "Shapes", null),
                lifecycle(10, Action.CLASS_CREATED, "Circle", null),
                lifecycle(10, Action.METHOD_ADDED, "Circle", "area()"),
                editor(10, 10_000, 0, 12, 12),
                new EditorEvent("session-1", at(20), 10_000, 10_000, 0, 30, 25, 40, 3, List.of("Circle")),
                compile(25, false, error(7, "compiler.err.cant.resolve", TRICKY_MESSAGE),error(9, "compiler.err.expected", "';' expected")),
                compile(40, true),
                editor(100, 20_000, 60_000, 0, 0),
                new ExecutionEvent("session-1", at(45), "Circle", "area", ExitType.NORMAL_EXIT, null,ConsoleOutput.of("Area: 3.14", 200)),
                new ExecutionEvent("session-1", at(50), "Circle", "scale", ExitType.EXCEPTION_EXIT,new ExceptionDetails("java.lang.ArithmeticException", "/ by zero", "Circle.java:12"),ConsoleOutput.of("java.lang.ArithmeticException: / by zero", 200)),
                lifecycle(60, Action.METHOD_ADDED, "Circle", "scale(double)"),
                lifecycle(120, Action.PROJECT_CLOSED, "Shapes", null)));
        return report(studentId, events);
    }

    static SessionReport empty() {
        return report(null, List.of());
    }

    static SessionReport manyCompilations(int attempts) {
        List<TelemetryEvent> events = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            CompilationEvent attempt = compile(i * 10, false, error(i, "compiler.err.expected", "';' expected"));
            events.add(attempt);
        }
        return report("816000123", events);
    }

    private static SessionReport report(String studentId, List<TelemetryEvent> events) {
        SessionInfo info = new SessionInfo("session-1", studentId, ProjectRef.of("Shapes", Path.of("shapes")), START);
        return new TelemetryAggregator().aggregate(info, events, at(125), ExportReason.MANUAL_EXPORT);
    }
}
