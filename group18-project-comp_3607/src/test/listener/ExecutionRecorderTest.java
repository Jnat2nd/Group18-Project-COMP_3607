package group18.listener;

import group18.model.ExecutionEvent;
import group18.model.ExecutionEvent.ExceptionDetails;
import group18.model.ExecutionEvent.ExitType;
import group18.model.TelemetryEvent;
import group18.testsupport.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static group18.testsupport.TestData.SESSION;
import static group18.testsupport.TestData.at;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ExecutionRecorder records runs, runtime exceptions and console output summaries")
class ExecutionRecorderTest {

    private final List<TelemetryEvent> events = new ArrayList<>();
    private ExecutionRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new ExecutionRecorder(SESSION, events::add);
    }

    private ExecutionEvent last() {
        return (ExecutionEvent) events.get(events.size() - 1);
    }

    private void record(String method, String exit, String result, String... panes) {
        recorder.record(new InvocationResult("Student", method, exit, result, List.of(panes), at(events.size())));
    }

    @Test
    void normalRunIsSuccessfulWithItsConsoleSummary() {
        record("main", "NORMAL_EXIT", null, "  Hello\nWorld\n");
        ExecutionEvent e = last();
        assertTrue(e.successful());
        assertEquals(ExitType.NORMAL_EXIT, e.exitType());
        assertEquals("Student.main", e.invocation());
        assertEquals("Hello\nWorld", e.console().summary());
        assertEquals(2, e.console().lineCount());
        assertNull(e.exception());
    }

    @Test
    void onlyOutputAddedSinceThePreviousRunIsSummarised() {
        record("first", "NORMAL_EXIT", null, "one\n");
        record("second", "NORMAL_EXIT", null, "one\ntwo\n");
        assertEquals("two", last().console().summary());
    }

    @Test
    void aClearedTerminalCountsAsAllNewOutput() {
        record("first", "NORMAL_EXIT", null, "one\n");
        record("second", "NORMAL_EXIT", null, "fresh\n");
        assertEquals("fresh", last().console().summary());
    }

    @Test
    void standardErrorPaneIsTrackedSeparately() {
        record("first", "NORMAL_EXIT", null, "out1\n", "");
        record("second", "NORMAL_EXIT", null, "out1\nout2\n", "warning\n");
        assertEquals("out2\nwarning", last().console().summary());
    }

    @Test
    void unreadableTerminalGivesNoOutputAndKeepsTheLastSnapshot() {
        record("first", "NORMAL_EXIT", null, "one\n");
        recorder.record(new InvocationResult("Student", "second", "NORMAL_EXIT", null, null, at(5)));
        assertEquals(0, last().console().lineCount());
        record("third", "NORMAL_EXIT", null, "one\nthree\n");
        assertEquals("three", last().console().summary());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "terminal/arithmetic-exception.txt, java.lang.ArithmeticException, / by zero, Student.java:23",
            "terminal/custom-exception.txt, bank.InsufficientFundsException, Balance is only 120.0, BankAccount.java:41"
    })
    void exceptionTypeMessageAndLocationAreReadFromTheConsole(String file, String type, String message,
                                                              String location) {
        record("getAverage", "EXCEPTION_EXIT", null, TestData.resource(file));
        ExecutionEvent e = last();
        assertFalse(e.successful());
        assertEquals(new ExceptionDetails(type, message, location), e.exception());
    }

    @Test
    void errorsWithoutAMessageAreRecognised() {
        record("compute", "EXCEPTION_EXIT", null, TestData.resource("terminal/stack-overflow.txt"));
        assertEquals(new ExceptionDetails("java.lang.StackOverflowError", null, "Factorial.java:7"),
                last().exception());
    }

    @Test
    void exceptionWithoutConsoleTextIsUnknownAndKeepsBlueJsResult() {
        record("divide", "EXCEPTION_EXIT", "boom");
        assertEquals(new ExceptionDetails("unknown", "boom", null), last().exception());
    }

    @Test
    void terminatedAndUnknownRunsFailWithoutAnException() {
        record("loop", "TERMINATED_EXIT", null);
        assertEquals(ExitType.TERMINATED_EXIT, last().exitType());
        record("loop", null, null);
        assertEquals(ExitType.UNKNOWN_EXIT, last().exitType());
        record("loop", "SOMETHING_NEW", null);
        assertEquals(ExitType.UNKNOWN_EXIT, last().exitType());
        assertTrue(events.stream().map(ExecutionEvent.class::cast)
                .allMatch(e -> !e.successful() && e.exception() == null));
    }

    @Test
    void constructorCallsAreDescribedWithNew() {
        recorder.record(new InvocationResult("Student", null, "NORMAL_EXIT", null, List.of(), at(1)));
        assertEquals("new Student", last().invocation());
    }

    @Test
    void longOutputIsShortened() {
        record("main", "NORMAL_EXIT", null, "x".repeat(500));
        String summary = last().console().summary();
        assertEquals(ExecutionRecorder.SUMMARY_CHARS + 3, summary.length());
        assertTrue(summary.endsWith("..."));
    }
}
