package group18.command;

import group18.analytics.TelemetryAggregator;
import group18.export.ExportStrategy;
import group18.listener.CompileSignal;
import group18.listener.InvocationResult;
import group18.model.CompilationEvent;
import group18.model.EditorEvent;
import group18.model.ExecutionEvent;
import group18.model.ExportReason;
import group18.model.LifecycleEvent;
import group18.model.LifecycleEvent.Action;
import group18.model.ProjectRef;
import group18.model.SessionInfo;
import group18.model.SessionReport;
import group18.model.TelemetryEvent;
import group18.session.OutputDirectories;
import group18.session.ProjectSession;
import group18.session.SessionExporter;
import group18.session.SessionRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static group18.testsupport.TestData.START;
import static group18.testsupport.TestData.at;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Each command changes the sessions as described")
class CommandsTest {

    @TempDir
    Path workspace;

    private final SessionRegistry registry = new SessionRegistry();
    private final List<SessionReport> exported = new ArrayList<>();
    private final List<Path> exportFolders = new ArrayList<>();
    private SessionExporter exporter;
    private ProjectRef zoo;
    private ProjectRef farm;

    /** Records what would be exported instead of writing files. */
    private final ExportStrategy recordingStrategy = (report, folder) -> {
        exported.add(report);
        exportFolders.add(folder);
        return List.of(folder.resolve(report.metadata().projectName() + ".json"));
    };

    @BeforeEach
    void setUp() throws IOException {
        exporter = new SessionExporter(new TelemetryAggregator(), recordingStrategy,
                new OutputDirectories(workspace.resolve("fallback")));
        zoo = ProjectRef.of("Zoo", Files.createDirectories(workspace.resolve("zoo")));
        farm = ProjectRef.of("Farm", Files.createDirectories(workspace.resolve("farm")));
    }

    private void open(ProjectRef project, boolean isNew) {
        new OpenProjectCommand(registry, new SessionInfo("id-" + project.name(), "816000123", project, START),
                (folder, time) -> isNew).execute();
    }

    private List<TelemetryEvent> events(ProjectRef project) {
        return registry.find(project.key()).orElseThrow().events();
    }

    private static Action lastAction(List<TelemetryEvent> events) {
        return events.stream().filter(LifecycleEvent.class::isInstance).map(LifecycleEvent.class::cast)
                .reduce((first, second) -> second).orElseThrow().action();
    }

    @Test
    void openingStartsOneSessionAndRecordsWhetherTheProjectIsNew() {
        open(zoo, true);
        open(farm, false);
        open(zoo, false);
        assertEquals(List.of(zoo.key(), farm.key()), registry.keys());
        assertEquals(Action.PROJECT_CREATED, lastAction(events(zoo)));
        assertEquals(Action.PROJECT_OPENED, lastAction(events(farm)));
        assertEquals(1, events(zoo).size());
    }

    @Test
    void closingEndsTheSessionAndExportsIt() throws IOException {
        open(zoo, false);
        CloseProjectCommand close = new CloseProjectCommand(registry, exporter, zoo.key(),
                ExportReason.PROJECT_CLOSED, at(60));
        close.execute();
        assertFalse(registry.contains(zoo.key()));
        assertEquals(List.of(zoo.directory().resolve("telemetry").resolve("Zoo.json")), close.writtenFiles());
        SessionReport report = exported.get(0);
        assertEquals(ExportReason.PROJECT_CLOSED, report.metadata().exportReason());
        assertEquals(Action.PROJECT_CLOSED, lastAction(report.events()));
        assertEquals(60, report.metadata().sessionDurationSeconds());
    }

    @Test
    void closingAProjectWhoseFolderWasDeletedRecordsTheDeletionAndUsesTheFallbackFolder() throws IOException {
        open(zoo, false);
        Files.delete(zoo.directory());
        new CloseProjectCommand(registry, exporter, zoo.key(), ExportReason.PROJECT_CLOSED, at(5)).execute();
        assertEquals(Action.PROJECT_DELETED, lastAction(exported.get(0).events()));
        assertEquals(workspace.resolve("fallback").resolve("Zoo"), exportFolders.get(0));
    }

    @Test
    void closingAnUnknownProjectDoesNothing() throws IOException {
        CloseProjectCommand close = new CloseProjectCommand(registry, exporter, "nope", ExportReason.BLUEJ_EXIT, at(1));
        close.execute();
        assertTrue(close.writtenFiles().isEmpty());
        assertTrue(exported.isEmpty());
    }

    @Test
    void closeProjectsOnlyClosesTheSelectedSessions() throws IOException {
        open(zoo, false);
        open(farm, false);
        CloseProjectsCommand close = new CloseProjectsCommand(registry, exporter, key -> key.equals(farm.key()),
                ExportReason.PROJECT_CLOSED, at(30));
        close.execute();
        assertEquals(List.of(zoo.key()), registry.keys());
        assertEquals(1, close.writtenFiles().size());
        assertEquals("Farm", exported.get(0).metadata().projectName());
    }

    @Test
    void closeProjectsClosesEverySessionEvenIfExportsFail() {
        open(zoo, false);
        open(farm, false);
        SessionExporter failing = new SessionExporter(new TelemetryAggregator(), (report, folder) -> {
            throw new IOException("cannot write " + report.metadata().projectName());
        }, new OutputDirectories(workspace));
        IOException thrown = assertThrows(IOException.class, () -> new CloseProjectsCommand(registry, failing,
                key -> true, ExportReason.BLUEJ_EXIT, at(30)).execute());
        assertTrue(registry.keys().isEmpty());
        assertEquals("cannot write Zoo", thrown.getMessage());
        assertEquals(1, thrown.getSuppressed().length);
    }

    @Test
    void sourceSamplesAndEditsReachTheProjectSession() {
        open(zoo, false);
        new SampleSourcesCommand(registry, zoo.key(), Map.of(), at(10)).execute();
        new TextEditCommand(registry, zoo.key(), "Lion", 0, 1, at(12)).execute();
        new SampleSourcesCommand(registry, zoo.key(), Map.of("Lion", "class Lion { void roar() {} }"), at(20))
                .execute();
        new TextEditCommand(registry, "unknown", "Lion", 0, 1, at(21)).execute();
        new SampleSourcesCommand(registry, "unknown", Map.of(), at(22)).execute();

        List<TelemetryEvent> events = events(zoo);
        assertTrue(events.stream().anyMatch(e -> e instanceof LifecycleEvent l && l.action() == Action.CLASS_CREATED
                && l.target().equals("Lion")));
        EditorEvent second = events.stream().filter(EditorEvent.class::isInstance).map(EditorEvent.class::cast)
                .reduce((a, b) -> b).orElseThrow();
        assertEquals(1, second.charactersTyped());
        assertEquals(List.of("Lion"), second.classesEdited());
    }

    @Test
    void compileSignalsGoToTheSessionThatOwnsTheFiles() {
        open(zoo, false);
        open(farm, false);
        Path lion = zoo.directory().resolve("Lion.java");
        new CompileSignalCommand(registry, CompileSignal.started(List.of(lion), at(1))).execute();
        new CompileSignalCommand(registry, CompileSignal.error(lion, 4, "';' expected", at(2))).execute();
        new CompileSignalCommand(registry, CompileSignal.finished(List.of(), false, at(3))).execute();

        CompilationEvent compilation = (CompilationEvent) events(zoo).stream()
                .filter(CompilationEvent.class::isInstance).findFirst().orElseThrow();
        assertEquals(List.of("Lion.java"), compilation.files());
        assertEquals(1, compilation.errorCount());
        assertTrue(events(farm).stream().noneMatch(CompilationEvent.class::isInstance));
    }

    @Test
    void compileSignalsForUnknownFilesAreIgnored() {
        open(zoo, false);
        new CompileSignalCommand(registry, CompileSignal.started(List.of(Path.of("/other/A.java")), at(1)))
                .execute();
        new CompileSignalCommand(registry, CompileSignal.finished(List.of(Path.of("/other/A.java")), true, at(2)))
                .execute();
        assertTrue(events(zoo).stream().noneMatch(CompilationEvent.class::isInstance));
    }

    @Test
    void invocationsAreRecordedInTheProjectSession() {
        open(zoo, false);
        new ExecutionCommand(registry, zoo.key(), new InvocationResult("Lion", "roar", "NORMAL_EXIT", null,
                List.of("ROAR\n"), at(5))).execute();
        new ExecutionCommand(registry, "unknown", new InvocationResult("Lion", "roar", "NORMAL_EXIT", null,
                List.of(), at(6))).execute();
        ExecutionEvent run = (ExecutionEvent) events(zoo).stream().filter(ExecutionEvent.class::isInstance)
                .findFirst().orElseThrow();
        assertEquals("ROAR", run.console().summary());
    }

    @Test
    void manualExportWritesReportsAndTheSessionCarriesOn() throws IOException {
        open(zoo, false);
        open(farm, false);
        ExportReportCommand one = new ExportReportCommand(registry, exporter, zoo.key(), at(10));
        one.execute();
        assertEquals(1, one.writtenFiles().size());
        assertEquals(ExportReason.MANUAL_EXPORT, exported.get(0).metadata().exportReason());
        assertTrue(registry.contains(zoo.key()));

        ExportReportCommand all = new ExportReportCommand(registry, exporter, null, at(20));
        all.execute();
        assertEquals(2, all.writtenFiles().size());
    }

    @Test
    void manualExportWithoutAnOpenProjectFails() {
        assertThrows(IllegalStateException.class,
                () -> new ExportReportCommand(registry, exporter, null, at(1)).execute());
        assertThrows(IllegalStateException.class,
                () -> new ExportReportCommand(registry, exporter, "nope", at(1)).execute());
    }

    @Test
    void commandsRejectMissingCollaborators() {
        assertThrows(NullPointerException.class, () -> new OpenProjectCommand(null, null, null));
        assertThrows(NullPointerException.class,
                () -> new CloseProjectCommand(registry, exporter, null, ExportReason.BLUEJ_EXIT, at(1)));
        assertThrows(NullPointerException.class, () -> new CompileSignalCommand(registry, null));
        assertThrows(NullPointerException.class, () -> new ExecutionCommand(registry, "k", null));
        assertThrows(NullPointerException.class, () -> new TextEditCommand(registry, "k", null, 0, 0, at(1)));
        assertThrows(NullPointerException.class, () -> new ExportReportCommand(registry, exporter, null, null));
        assertThrows(NullPointerException.class,
                () -> new CloseProjectsCommand(registry, exporter, null, ExportReason.BLUEJ_EXIT, at(1)));
        assertThrows(NullPointerException.class, () -> new SampleSourcesCommand(registry, null, Map.of(), at(1)));
    }

    @Test
    void sessionsAreFoundByProjectFolder() {
        open(zoo, false);
        ProjectSession session = registry.owning(List.of(zoo.directory().resolve("sub/Lion.java"))).get(0);
        assertEquals(zoo, session.project());
    }
}
