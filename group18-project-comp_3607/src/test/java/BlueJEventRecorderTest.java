import group18.model.CompilationError;
import group18.model.CompilationEvent;
import group18.model.ExecutionEvent;
import group18.model.TelemetryEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BlueJEventRecorderTest {

    final List<TelemetryEvent> events = new ArrayList<>();
    BlueJEventRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new BlueJEventRecorder("s1", events::add);
    }

    CompilationEvent compilation(int index) {
        return (CompilationEvent) events.get(index);
    }

    ExecutionEvent execution() {
        return (ExecutionEvent) events.get(0);
    }

    @Test
    void failedCompileCarriesItsErrorsWithCodesAndLines() {
        recorder.compileStarted();
        recorder.compileError(3, "';' expected");
        recorder.compileError(7, "cannot find symbol");
        recorder.compileFinished(false);

        assertEquals(1, events.size());
        CompilationEvent e = compilation(0);
        assertFalse(e.isSuccessful());
        assertEquals("s1", e.getSessionId());
        assertEquals("COMPILATION", e.getEventType());
        assertEquals(2, e.getErrors().size());
        assertEquals(new CompilationError(3, "compiler.err.expected", "';' expected"), e.getErrors().get(0));
        assertEquals(7, e.getErrors().get(1).lineNumber());
        assertEquals("compiler.err.cant.resolve", e.getErrors().get(1).errorCode());
    }

    @Test
    void successfulCompileHasNoErrors() {
        recorder.compileStarted();
        recorder.compileFinished(true);
        assertTrue(compilation(0).isSuccessful());
        assertTrue(compilation(0).getErrors().isEmpty());
    }

    @Test
    void errorsDoNotLeakIntoTheNextAttempt() {
        recorder.compileError(1, "missing return statement");
        recorder.compileFinished(false);
        recorder.compileFinished(true);
        assertEquals(1, compilation(0).getErrors().size());
        assertTrue(compilation(1).getErrors().isEmpty());
    }

    @Test
    void startingAnAttemptDropsErrorsFromOneThatNeverFinished() {
        recorder.compileError(1, "missing return statement");
        recorder.compileStarted();
        recorder.compileFinished(true);
        assertTrue(compilation(0).getErrors().isEmpty());
    }

    @Test
    void nullErrorMessageBecomesEmptyAndUnclassified() {
        recorder.compileError(5, null);
        recorder.compileFinished(false);
        assertEquals(new CompilationError(5, ErrorCodes.UNCLASSIFIED, ""), compilation(0).getErrors().get(0));
    }

    @Test
    void normalRunIsSuccessfulWithItsConsoleSummary() {
        recorder.executionFinished("Main", "NORMAL_EXIT", null, "  Hello\n");
        ExecutionEvent e = execution();
        assertTrue(e.isSuccessful());
        assertEquals("EXECUTION", e.getEventType());
        assertEquals("Main", e.getTargetClass());
        assertEquals("Hello", e.getConsoleOutputSummary());
        assertNull(e.getExceptionType());
        assertNull(e.getExceptionMessage());
    }

    @Test
    void exceptionTypeAndMessageAreReadFromTheConsole() {
        recorder.executionFinished("Main", "EXCEPTION_EXIT", "null",
                "Exception in thread \"main\" java.lang.ArithmeticException: / by zero\n\tat Main.main(Main.java:5)");
        ExecutionEvent e = execution();
        assertFalse(e.isSuccessful());
        assertEquals("java.lang.ArithmeticException", e.getExceptionType());
        assertEquals("/ by zero", e.getExceptionMessage());
    }

    @Test
    void exceptionWithoutReadableConsoleIsUnknownAndKeepsBlueJsResult() {
        recorder.executionFinished("Main", "EXCEPTION_EXIT", "boom", null);
        ExecutionEvent e = execution();
        assertEquals("unknown", e.getExceptionType());
        assertEquals("boom", e.getExceptionMessage());
        assertEquals("", e.getConsoleOutputSummary());
    }

    @Test
    void terminatedAndUnknownRunsFailWithoutAnException() {
        recorder.executionFinished("Main", "UNKNOWN_EXIT", null, "");
        recorder.executionFinished("Main", "TERMINATED_EXIT", null, "");
        recorder.executionFinished("Main", null, null, "");
        for (TelemetryEvent e : events) {
            assertFalse(((ExecutionEvent) e).isSuccessful());
            assertNull(((ExecutionEvent) e).getExceptionType());
        }
    }

    @Test
    void longConsoleOutputIsTruncated() {
        recorder.executionFinished("Main", "NORMAL_EXIT", null, "x".repeat(500));
        String summary = execution().getConsoleOutputSummary();
        assertEquals(BlueJEventRecorder.CONSOLE_SUMMARY_CHARS + 3, summary.length());
        assertTrue(summary.endsWith("..."));
    }
}
