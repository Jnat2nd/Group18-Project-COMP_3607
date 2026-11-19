package group18.analytics;

import group18.analytics.calculators.CompilationMetricsCalculator;
import group18.analytics.calculators.EditingMetricsCalculator;
import group18.analytics.calculators.ExecutionMetricsCalculator;
import group18.analytics.calculators.LifecycleMetricsCalculator;
import group18.analytics.calculators.TimeMetricsCalculator;
import group18.analytics.calculators.TimelineCalculator;
import group18.model.CompilationEvent;
import group18.model.EditorEvent;
import group18.model.ExportReason;
import group18.model.LifecycleEvent.Action;
import group18.model.ReportSummary;
import group18.model.ReportSummary.CompilationSummary;
import group18.model.ReportSummary.ErrorFrequency;
import group18.model.ReportSummary.ExceptionFrequency;
import group18.model.ReportSummary.TimeSummary;
import group18.model.SessionMetadata;
import group18.model.SessionReport;
import group18.model.TelemetryEvent;
import group18.model.TimelineEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static group18.testsupport.TestData.START;
import static group18.testsupport.TestData.at;
import static group18.testsupport.TestData.compile;
import static group18.testsupport.TestData.crash;
import static group18.testsupport.TestData.editor;
import static group18.testsupport.TestData.error;
import static group18.testsupport.TestData.lifecycle;
import static group18.testsupport.TestData.run;
import static group18.testsupport.TestData.session;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TelemetryAggregator and its metric calculators calculate the report figures")
class TelemetryAggregatorTest {

    private final TelemetryAggregator aggregator = new TelemetryAggregator();

    @Nested
    @DisplayName("active and idle time")
    class Time {

        @Test
        void sumsActiveAndIdleTimeAndWorksOutPercentages() {
            TimeSummary t = new TimeMetricsCalculator().calculate(List.of(editor(10, 10_000, 0, 5, 5),editor(20, 6_000, 4_000, 0, 0),editor(60, 0, 40_000, 0, 0),editor(70, 10_000, 0, 3, 3)));assertAll(() -> assertEquals(26, t.activeSeconds()),() -> assertEquals(44, t.idleSeconds()),() -> assertEquals(37.1, t.activePercent(), 0.001),() -> assertEquals(62.9, t.idlePercent(), 0.001),() -> assertEquals(1, t.idlePeriods()),() -> assertEquals(44, t.longestIdleSeconds()));
        }

        @Test
        void countsSeparateIdleStretchesAndFindsTheLongest() {
            TimeSummary t = new TimeMetricsCalculator().calculate(List.of( editor(10, 0, 10_000, 0, 0),editor(20, 10_000, 0, 1, 1), editor(50, 0, 30_000, 0, 0), editor(55, 0, 5_000, 0, 0),editor(60, 5_000, 0, 1, 1)));
            assertEquals(2, t.idlePeriods());
            assertEquals(35, t.longestIdleSeconds());
        }

        @Test
        void noEditorEventsMeansNoTime() {
            TimeSummary t = new TimeMetricsCalculator().calculate(List.of());
            assertEquals(new TimeSummary(0, 0, 0, 0, 0, 0), t);
        }
    }

    @Nested
    @DisplayName("typing cadence and edit frequency")
    class Editing {

        @Test
        void cadenceAndFrequencyArePerActiveMinute() {
            List<TelemetryEvent> events = List.of(
                    new EditorEvent("s", at(60), 60_000, 60_000, 0, 90, 80, 40, 10, List.of("A")),
                    new EditorEvent("s", at(120), 60_000, 30_000, 30_000, 30, 25, 0, 5, List.of("A")));
            ReportSummary.EditingSummary e = new EditingMetricsCalculator().calculate(events);
            assertAll(
                    () -> assertEquals(120, e.editCount()),
                    () -> assertEquals(105, e.charactersTyped()),
                    () -> assertEquals(40, e.charactersPasted()),
                    () -> assertEquals(15, e.charactersDeleted()),
                    () -> assertEquals(70.0, e.typingCadenceCharsPerMinute(), 0.001),
                    () -> assertEquals(80.0, e.editsPerActiveMinute(), 0.001));
        }

        @Test
        void noActiveTimeMeansZeroRates() {
            ReportSummary.EditingSummary e = new EditingMetricsCalculator().calculate(List.of(editor(10, 0, 10_000, 2, 2)));
            assertEquals(0, e.typingCadenceCharsPerMinute());
            assertEquals(0, e.editsPerActiveMinute());
        }
    }

    @Nested
    @DisplayName("compilation results")
    class Compilation {

        private CompilationSummary summary;

        @BeforeEach
        void setUp() {
            summary = new CompilationMetricsCalculator().calculate(List.of(
                    compile(0, false, error(3, "compiler.err.expected", "';' expected"),error(9, "compiler.err.cant.resolve", "cannot find symbol\n  symbol: x")),
                    compile(60, false, error(9, "compiler.err.cant.resolve", "cannot find symbol\n  symbol: y")),
                    compile(120, true),
                    compile(300, true)));
        }

        @Test
        void successAndErrorRatesAreBasedOnAttempts() {
            assertAll(
                    () -> assertEquals(4, summary.attempts()),
                    () -> assertEquals(2, summary.successful()),
                    () -> assertEquals(2, summary.failed()),
                    () -> assertEquals(50.0, summary.successRatePercent()),
                    () -> assertEquals(50.0, summary.errorRatePercent()),
                    () -> assertEquals(3, summary.totalErrors()),
                    () -> assertEquals(1.5, summary.averageErrorsPerFailedAttempt()),
                    () -> assertEquals(100.0, summary.averageSecondsBetweenAttempts()));
        }

        @Test
        void errorsAreGroupedByCodeMostFrequentFirst() {
            assertEquals(List.of(
                    new ErrorFrequency("compiler.err.cant.resolve", "Cannot find symbol", 2, 66.7, "cannot find symbol"),
                    new ErrorFrequency("compiler.err.expected", "Missing symbol (e.g. ';' or ')')", 1, 33.3,  "';' expected")), summary.errorFrequency());
        }

        @Test
        void noAttemptsGivesZeroRatesWithoutDividingByZero() {
            CompilationSummary empty = new CompilationMetricsCalculator().calculate(List.of());
            assertEquals(new CompilationSummary(0, 0, 0, 0, 0, 0, 0, 0, List.of()), empty);
        }

        @Test
        void aSingleSuccessfulAttemptIsAHundredPercent() {
            CompilationSummary one = new CompilationMetricsCalculator().calculate(List.of(compile(5, true)));
            assertEquals(100.0, one.successRatePercent());
            assertEquals(0, one.averageErrorsPerFailedAttempt());
            assertEquals(0, one.averageSecondsBetweenAttempts());
        }
    }

    @Test
    void runsAndRuntimeExceptionsAreCountedByType() {
        ReportSummary.ExecutionSummary x = new ExecutionMetricsCalculator().calculate(List.of(
                run(1, "main"), run(2, "area"),
                crash(3, "divide", "java.lang.ArithmeticException"),
                crash(4, "print", "java.lang.NullPointerException"),
                crash(5, "print", "java.lang.NullPointerException")));
        assertAll(
                () -> assertEquals(5, x.runs()),
                () -> assertEquals(2, x.successfulRuns()),
                () -> assertEquals(3, x.runtimeExceptions()),
                () -> assertEquals(40.0, x.runSuccessPercent()),
                () -> assertEquals(List.of(new ExceptionFrequency("java.lang.NullPointerException", 2),new ExceptionFrequency("java.lang.ArithmeticException", 1)), x.exceptionFrequency()));
    }

    @Test
    void lifecycleChangesAreCountedAndTheTimelineSkipsModifications() {
        List<group18.model.LifecycleEvent> events = List.of(
                lifecycle(0, Action.PROJECT_OPENED, "Shapes", null),
                lifecycle(10, Action.CLASS_CREATED, "Circle", null),
                lifecycle(10, Action.METHOD_ADDED, "Circle", "area()"),
                lifecycle(20, Action.CLASS_MODIFIED, "Circle", null),
                lifecycle(20, Action.METHOD_ADDED, "Circle", "radius()"),
                lifecycle(30, Action.METHOD_REMOVED, "Circle", "radius()"),
                lifecycle(40, Action.CLASS_DELETED, "Square", null));
        assertEquals(new ReportSummary.LifecycleSummary(1, 1, 1, 2, 1), new LifecycleMetricsCalculator().calculate(List.copyOf(events)));
        List<TimelineEntry> timeline = new TimelineCalculator().calculate(List.copyOf(events));
        assertEquals(6, timeline.size());
        assertTrue(timeline.stream().noneMatch(entry -> entry.action() == Action.CLASS_MODIFIED));
        assertEquals(new TimelineEntry(at(10), Action.METHOD_ADDED, "Circle", "area()"), timeline.get(2));
    }

    @Test
    void aggregatorUsesTheCalculatorsItIsGiven() {
        TelemetryAggregator custom = new TelemetryAggregator(events -> new TimeSummary(1, 2, 3, 4, 5, 6), new EditingMetricsCalculator(), new CompilationMetricsCalculator(), new ExecutionMetricsCalculator(), new LifecycleMetricsCalculator(), events -> List.of());
        SessionReport report = custom.aggregate(session(Path.of("/p")), List.of(lifecycle(0, Action.PROJECT_OPENED, "Shapes", null)), at(10), ExportReason.MANUAL_EXPORT);
        assertEquals(new TimeSummary(1, 2, 3, 4, 5, 6), report.summary().time());
        assertTrue(report.timeline().isEmpty());
        assertThrows(NullPointerException.class, () -> new TelemetryAggregator(null, null, null, null, null, null));
    }

    @Test
    void reportHasMetadataTimelineAndEventsInTimeOrder() {
        List<TelemetryEvent> events = new ArrayList<>(List.of(compile(90, true),lifecycle(0, Action.PROJECT_CREATED, "Shapes", null), editor(60, 60_000, 0, 10, 10),compile(30, false, error(1, "compiler.err.expected", "';' expected"))));
        SessionReport report = aggregator.aggregate(session(Path.of("/projects/shapes")), events, at(125.6), ExportReason.PROJECT_CLOSED);

        SessionMetadata m = report.metadata();
        assertAll(
                () -> assertEquals(TelemetryAggregator.SCHEMA_VERSION, m.schemaVersion()),
                () -> assertEquals(TelemetryAggregator.GENERATOR, m.generator()),
                () -> assertEquals("session-1", m.sessionId()),
                () -> assertEquals("816000123", m.studentId()),
                () -> assertEquals("Shapes", m.projectName()),
                () -> assertEquals(START, m.startTime()),
                () -> assertEquals(at(125.6), m.endTime()),
                () -> assertEquals(125, m.sessionDurationSeconds()),
                () -> assertEquals(2, m.totalCompilationAttempts()),
                () -> assertEquals(4, m.totalEvents()),
                () -> assertEquals(ExportReason.PROJECT_CLOSED, m.exportReason()));
        assertEquals(List.of(at(0), at(30), at(60), at(90)),report.events().stream().map(TelemetryEvent::timestamp).toList());
        assertEquals(1, report.timeline().size());
        assertEquals(60, report.summary().time().activeSeconds());
        assertEquals(50.0, report.summary().compilation().successRatePercent());
        assertTrue(report.events().get(1) instanceof CompilationEvent);
    }

    @Test
    void reportEndingBeforeTheStartHasZeroDuration() {
        SessionReport report = aggregator.aggregate(session(Path.of("/p")), List.of(), START.minusSeconds(10),ExportReason.MANUAL_EXPORT);
        assertEquals(0, report.metadata().sessionDurationSeconds());
        assertTrue(report.events().isEmpty());
    }
}
