package group18.listener;

import group18.model.CompilationError;
import group18.model.CompilationEvent;
import group18.model.TelemetryEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static group18.testsupport.TestData.SESSION;
import static group18.testsupport.TestData.at;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CompilationRecorder combines BlueJ's compile callbacks into one event per attempt")
class CompilationRecorderTest {

    private static final Path DOG = Path.of("/projects/zoo/Dog.java");
    private static final Path CAT = Path.of("/projects/zoo/Cat.java");

    private final List<TelemetryEvent> events = new ArrayList<>();
    private CompilationRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new CompilationRecorder(SESSION, events::add);
    }

    private CompilationEvent compilation(int index) {
        return (CompilationEvent) events.get(index);
    }

    @Test
    void failedAttemptCarriesEveryErrorWithCodeFileAndLine() {
        recorder.handle(CompileSignal.started(List.of(DOG, CAT), at(1)));
        assertTrue(recorder.inProgress());
        recorder.handle(CompileSignal.error(DOG, 3, "';' expected", at(2)));
        recorder.handle(CompileSignal.error(CAT, 7, "cannot find symbol\n  symbol: variable x", at(2)));
        recorder.handle(CompileSignal.finished(List.of(DOG, CAT), false, at(3)));

        assertFalse(recorder.inProgress());
        assertEquals(1, events.size());
        CompilationEvent e = compilation(0);
        assertFalse(e.successful());
        assertEquals(at(3), e.timestamp());
        assertEquals(List.of("Dog.java", "Cat.java"), e.files());
        assertEquals(2, e.errorCount());
        assertEquals(new CompilationError("Dog.java", 3, "compiler.err.expected", "';' expected"), e.errors().get(0));
        assertEquals("compiler.err.cant.resolve", e.errors().get(1).errorCode());
        assertEquals(7, e.errors().get(1).lineNumber());
    }

    @Test
    void successfulAttemptHasNoErrors() {
        recorder.handle(CompileSignal.started(List.of(DOG), at(1)));
        recorder.handle(CompileSignal.finished(List.of(DOG), true, at(2)));
        assertTrue(compilation(0).successful());
        assertTrue(compilation(0).errors().isEmpty());
    }

    @Test
    void errorsDoNotLeakIntoTheNextAttempt() {
        recorder.handle(CompileSignal.error(DOG, 1, "missing return statement", at(1)));
        recorder.handle(CompileSignal.finished(List.of(DOG), false, at(2)));
        recorder.handle(CompileSignal.finished(List.of(DOG), true, at(3)));
        assertEquals(1, compilation(0).errorCount());
        assertEquals(0, compilation(1).errorCount());
    }

    @Test
    void startingAnAttemptDropsErrorsFromOneThatNeverFinished() {
        recorder.handle(CompileSignal.error(DOG, 1, "missing return statement", at(1)));
        recorder.handle(CompileSignal.started(List.of(DOG), at(2)));
        recorder.handle(CompileSignal.finished(List.of(DOG), true, at(3)));
        assertEquals(0, compilation(0).errorCount());
    }

    @Test
    void missingErrorDetailsAreRecordedSafely() {
        recorder.handle(CompileSignal.error(null, -4, null, at(1)));
        recorder.handle(CompileSignal.finished(List.of(), false, at(2)));
        assertEquals(new CompilationError("", 0, ErrorCodes.UNCLASSIFIED, ""), compilation(0).errors().get(0));
        assertTrue(compilation(0).files().isEmpty());
    }

    @Test
    void signalsDescribeTheirPhase() {
        assertTrue(CompileSignal.finished(List.of(), true, at(0)).isFinish());
        assertTrue(CompileSignal.finished(List.of(), false, at(0)).isFinish());
        assertFalse(CompileSignal.started(null, at(0)).isFinish());
        assertEquals(CompileSignal.Phase.ERROR, CompileSignal.error(DOG, 1, "x", at(0)).phase());
    }
}
