package group18.model;

import group18.model.ExecutionEvent.ConsoleOutput;
import group18.model.ExecutionEvent.ExceptionDetails;
import group18.model.ExecutionEvent.ExitType;
import group18.model.LifecycleEvent.Action;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Model records validate their data and derive values correctly")
class ModelTest {

    private static final Instant T = Instant.parse("2026-11-09T18:00:00Z");

    @Test
    void eachEventReportsItsCategory() {
        assertEquals(EventType.LIFECYCLE, LifecycleEvent.of("s", T, Action.CLASS_CREATED, "Dog").eventType());
        assertEquals(EventType.EDITOR_INTERACTION, EditorEvent.idle("s", T, 1000).eventType());
        assertEquals(EventType.COMPILATION, new CompilationEvent("s", T, true, null, null).eventType());
        assertEquals(EventType.EXECUTION,
                new ExecutionEvent("s", T, "Dog", "bark", ExitType.NORMAL_EXIT, null, null).eventType());
        assertEquals(4, EventType.values().length);
    }

    @Test
    void requiredFieldsMustNotBeNull() {
        assertThrows(NullPointerException.class, () -> LifecycleEvent.of(null, T, Action.CLASS_CREATED, "Dog"));
        assertThrows(NullPointerException.class, () -> LifecycleEvent.of("s", null, Action.CLASS_CREATED, "Dog"));
        assertThrows(NullPointerException.class, () -> LifecycleEvent.of("s", T, null, "Dog"));
        assertThrows(NullPointerException.class, () -> LifecycleEvent.of("s", T, Action.CLASS_CREATED, null));
        assertThrows(NullPointerException.class, () -> new CompilationEvent("s", null, true, null, null));
        assertThrows(NullPointerException.class,
                () -> new ExecutionEvent("s", T, null, "m", ExitType.NORMAL_EXIT, null, null));
        assertThrows(NullPointerException.class, () -> new ExecutionEvent("s", T, "C", "m", null, null, null));
        assertThrows(NullPointerException.class, () -> new ExceptionDetails(null, "m", null));
        assertThrows(NullPointerException.class, () -> new SessionReport(null, null, null, null));
        assertThrows(NullPointerException.class, () -> new ProjectRef(null, "n", Path.of("p")));
    }

    @Test
    void editorEventRejectsImpossibleValues() {
        assertThrows(IllegalArgumentException.class,
                () -> new EditorEvent("s", T, 1000, -1, 0, 0, 0, 0, 0, null));
        assertThrows(IllegalArgumentException.class,
                () -> new EditorEvent("s", T, 1000, 800, 800, 0, 0, 0, 0, null));
        assertThrows(IllegalArgumentException.class,
                () -> new EditorEvent("s", T, 1000, 0, 0, 0, -3, 0, 0, null));
        assertThrows(NullPointerException.class, () -> EditorEvent.idle(null, T, 5));
    }

    @Test
    void editorEventCalculatesCadence() {
        EditorEvent e = new EditorEvent("s", T, 30_000, 30_000, 0, 20, 45, 0, 2, null);
        assertEquals(90.0, e.typingCadenceCharsPerMinute());
        assertFalse(e.idleOnly());
        assertTrue(e.classesEdited().isEmpty());
        assertEquals(0, EditorEvent.idle("s", T, 5000).typingCadenceCharsPerMinute());
        assertTrue(EditorEvent.idle("s", T, 5000).idleOnly());
    }

    @Test
    void listsAreCopiedSoEventsStayUnchanged() {
        List<String> files = new ArrayList<>(List.of("A.java"));
        CompilationEvent e = new CompilationEvent("s", T, false, files, List.of());
        files.add("B.java");
        assertEquals(List.of("A.java"), e.files());
        assertThrows(UnsupportedOperationException.class, () -> e.files().add("C.java"));
        assertEquals(0, e.errorCount());
    }

    @ParameterizedTest
    @EnumSource(ExitType.class)
    void exitTypesAreReadFromBlueJsNames(ExitType type) {
        assertEquals(type, ExitType.fromName(type.name()));
    }

    @Test
    void unknownExitNamesBecomeUnknownExit() {
        assertEquals(ExitType.UNKNOWN_EXIT, ExitType.fromName(null));
        assertEquals(ExitType.UNKNOWN_EXIT, ExitType.fromName("normal_exit"));
    }

    @Test
    void executionEventDescribesTheCall() {
        ExecutionEvent method = new ExecutionEvent("s", T, "Dog", "bark", ExitType.NORMAL_EXIT, null, null);
        assertEquals("Dog.bark", method.invocation());
        assertTrue(method.successful());
        assertEquals(ConsoleOutput.NONE, method.console());
        ExecutionEvent constructor = new ExecutionEvent("s", T, "Dog", null, ExitType.EXCEPTION_EXIT,
                new ExceptionDetails("java.lang.IllegalStateException", null, null), null);
        assertEquals("new Dog", constructor.invocation());
        assertFalse(constructor.successful());
    }

    @Test
    void consoleOutputIsSummarised() {
        assertEquals(ConsoleOutput.NONE, ConsoleOutput.of(null, 10));
        assertEquals(ConsoleOutput.NONE, ConsoleOutput.of("  \n ", 10));
        assertEquals(new ConsoleOutput(2, "a\nb"), ConsoleOutput.of("a\nb\n", 10));
        assertEquals(new ConsoleOutput(1, "abcde..."), ConsoleOutput.of("abcdefgh", 5));
    }

    @Test
    void onlyClassModificationsAreLeftOffTheTimeline() {
        for (Action action : Action.values()) {
            assertEquals(action != Action.CLASS_MODIFIED, action.isMilestone(), action.name());
        }
    }

    @Test
    void projectReferenceUsesTheNormalisedFolderAsKey() {
        Path folder = Path.of("projects", "zoo", "..", "zoo");
        ProjectRef project = ProjectRef.of("Zoo", folder);
        assertEquals(folder.toAbsolutePath().normalize(), project.directory());
        assertEquals(ProjectRef.keyOf(folder), project.key());
        assertTrue(project.contains(folder.resolve("Dog.java")));
        assertFalse(project.contains(Path.of("elsewhere", "Dog.java")));
        assertFalse(project.contains(null));
    }

    @Test
    void blankStudentIdBecomesThePlaceholder() {
        ProjectRef project = ProjectRef.of("Zoo", Path.of("zoo"));
        assertEquals(SessionInfo.STUDENT_ID_PLACEHOLDER, new SessionInfo("s", "  ", project, T).studentId());
        assertEquals(SessionInfo.STUDENT_ID_PLACEHOLDER, new SessionInfo("s", null, project, T).studentId());
        assertEquals("816000123", new SessionInfo("s", " 816000123 ", project, T).studentId());
        assertThrows(NullPointerException.class, () -> new SessionInfo(null, "x", project, T));
    }

    @Test
    void exportReasonsHaveDescriptions() {
        for (ExportReason reason : ExportReason.values()) {
            assertFalse(reason.description().isBlank());
        }
    }

    @Test
    void timelineEntryCopiesTheLifecycleEvent() {
        LifecycleEvent event = new LifecycleEvent("s", T, Action.METHOD_ADDED, "Dog", "bark()");
        assertEquals(new TimelineEntry(T, Action.METHOD_ADDED, "Dog", "bark()"), TimelineEntry.from(event));
    }

    @Test
    void reportListsAreNeverNull() {
        ReportSummary summary = new ReportSummary(null, null,
                new ReportSummary.CompilationSummary(0, 0, 0, 0, 0, 0, 0, 0, null), null, null);
        SessionMetadata metadata = new SessionMetadata("1", "g", "s", "id", "p", T, T, 0, 0, 0,
                ExportReason.MANUAL_EXPORT);
        SessionReport report = new SessionReport(metadata, summary, null, null);
        assertTrue(report.timeline().isEmpty());
        assertTrue(report.events().isEmpty());
        assertTrue(summary.compilation().errorFrequency().isEmpty());
        assertTrue(new ReportSummary.ExecutionSummary(0, 0, 0, 0, null).exceptionFrequency().isEmpty());
        assertNull(summary.time());
    }
}
