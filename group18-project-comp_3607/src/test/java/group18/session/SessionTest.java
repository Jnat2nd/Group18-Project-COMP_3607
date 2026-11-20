package group18.session;

import group18.analytics.TelemetryAggregator;
import group18.listener.CompileSignal;
import group18.model.EditorEvent;
import group18.model.ExportReason;
import group18.model.LifecycleEvent;
import group18.model.LifecycleEvent.Action;
import group18.model.ProjectRef;
import group18.model.SessionInfo;
import group18.model.SessionReport;
import group18.model.TelemetryEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static group18.testsupport.TestData.START;
import static group18.testsupport.TestData.at;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Sessions, the registry, output folders and new-project detection")
class SessionTest {

    @TempDir
    Path workspace;

    private ProjectSession session(String name) throws IOException {
        Path folder = Files.createDirectories(workspace.resolve(name));
        return new ProjectSession(new SessionInfo("id-" + name, null, ProjectRef.of(name, folder), START));
    }

    @Test
    void sessionRecordsEverythingItsTrackersProduce() throws IOException {
        ProjectSession session = session("Zoo");
        session.recordProjectAction(Action.PROJECT_OPENED, START);
        session.sampleSources(Map.of(), at(10));
        session.recordEdit("Lion", 0, 1, at(12));
        session.sampleSources(Map.of("Lion", "class Lion {}"), at(20));
        session.handleCompileSignal(CompileSignal.started(List.of(), at(21)));
        assertTrue(session.compileInProgress());
        session.handleCompileSignal(CompileSignal.finished(List.of(), true, at(22)));
        assertFalse(session.compileInProgress());
        session.flush(at(30));

        List<TelemetryEvent> events = session.events();
        assertEquals(Action.PROJECT_OPENED, ((LifecycleEvent) events.get(0)).action());
        assertTrue(events.stream().anyMatch(e -> e instanceof LifecycleEvent l && l.action() == Action.CLASS_CREATED));
        assertEquals(3, events.stream().filter(EditorEvent.class::isInstance).count());
        assertThrows(UnsupportedOperationException.class, () -> session.events().clear());
        assertEquals("Zoo", session.project().name());
        assertEquals(SessionInfo.STUDENT_ID_PLACEHOLDER, session.info().studentId());
    }

    @Test
    void compilationsCountAsActivity() throws IOException {
        ProjectSession session = session("Zoo");
        session.sampleSources(Map.of(), at(100));
        session.handleCompileSignal(CompileSignal.finished(List.of(), true, at(105)));
        session.sampleSources(Map.of(), at(110));
        EditorEvent last = (EditorEvent) session.events().stream().filter(EditorEvent.class::isInstance)
                .reduce((a, b) -> b).orElseThrow();
        assertEquals(5_000, last.activeMs());
    }

    @Test
    void registryKeepsOneSessionPerProject() throws IOException {
        SessionRegistry registry = new SessionRegistry();
        ProjectSession zoo = session("Zoo");
        ProjectSession farm = session("Farm");
        assertTrue(registry.add(zoo));
        assertFalse(registry.add(zoo));
        assertTrue(registry.add(farm));
        assertEquals(List.of(zoo, farm), registry.all());
        assertEquals(zoo, registry.find(zoo.project().key()).orElseThrow());
        assertEquals(List.of(farm), registry.owning(List.of(farm.project().directory().resolve("Cow.java"))));
        assertTrue(registry.owning(List.of(Path.of("/elsewhere/X.java"))).isEmpty());
        assertEquals(zoo, registry.remove(zoo.project().key()).orElseThrow());
        assertTrue(registry.remove(zoo.project().key()).isEmpty());
        assertEquals(List.of(farm.project().key()), registry.keys());
    }

    @Test
    void reportsGoInATelemetryFolderInsideTheProject() throws IOException {
        ProjectSession zoo = session("Zoo");
        OutputDirectories directories = new OutputDirectories(workspace.resolve("fallback"));
        assertEquals(zoo.project().directory().resolve(OutputDirectories.FOLDER_NAME),
                directories.forSession(zoo.info()));
    }

    @Test
    void reportsGoToTheFallbackFolderWhenTheProjectFolderIsGone() {
        SessionInfo gone = new SessionInfo("id", null, ProjectRef.of("Lab 3/Zoo", workspace.resolve("missing")),
                START);
        assertEquals(workspace.resolve("fallback").resolve("Lab_3_Zoo"),
                new OutputDirectories(workspace.resolve("fallback")).forSession(gone));
    }

    @Test
    void exporterFlushesAggregatesAndWritesToTheSessionFolder() throws IOException {
        ProjectSession zoo = session("Zoo");
        zoo.recordEdit("Lion", 0, 1, at(5));
        List<SessionReport> reports = new ArrayList<>();
        List<Path> folders = new ArrayList<>();
        SessionExporter exporter = new SessionExporter(new TelemetryAggregator(), (report, folder) -> {
            reports.add(report);
            folders.add(folder);
            return List.of(folder.resolve("x.json"));
        }, new OutputDirectories(workspace));
        List<Path> written = exporter.export(zoo, ExportReason.MANUAL_EXPORT, at(30));
        assertEquals(List.of(zoo.project().directory().resolve("telemetry").resolve("x.json")), written);
        assertEquals(1, reports.get(0).summary().editing().charactersTyped());
        assertEquals(ExportReason.MANUAL_EXPORT, reports.get(0).metadata().exportReason());
        assertEquals(zoo.project().directory().resolve("telemetry"), folders.get(0));
    }

    @Test
    void anEmptyFolderCreatedJustNowIsANewProject() throws IOException {
        Path folder = Files.createDirectories(workspace.resolve("NewProject"));
        Files.writeString(folder.resolve("package.bluej"), "#BlueJ package file");
        NewProjectDetector detector = new NewProjectDetector();
        assertTrue(detector.isNewlyCreated(folder, Instant.now()));
    }

    @Test
    void aFolderWithJavaFilesOrEarlierReportsIsNotNew() throws IOException {
        Path withCode = Files.createDirectories(workspace.resolve("WithCode"));
        Files.writeString(withCode.resolve("Dog.java"), "class Dog {}");
        Path reported = Files.createDirectories(workspace.resolve("Reported").resolve(OutputDirectories.FOLDER_NAME));
        NewProjectDetector detector = new NewProjectDetector();
        assertFalse(detector.isNewlyCreated(withCode, Instant.now()));
        assertFalse(detector.isNewlyCreated(reported.getParent(), Instant.now()));
        assertFalse(detector.isNewlyCreated(workspace.resolve("missing"), Instant.now()));
    }

    @Test
    void onlyRecentlyCreatedFoldersCount() throws IOException {
        NewProjectDetector detector = new NewProjectDetector(Duration.ofMinutes(2));
        assertTrue(detector.isRecent(START, START.plusSeconds(119)));
        assertTrue(detector.isRecent(START.plusSeconds(3), START));
        assertFalse(detector.isRecent(START, START.plusSeconds(121)));
        Path folder = Files.createDirectories(workspace.resolve("Old"));
        assertFalse(detector.isNewlyCreated(folder, Instant.now().plus(Duration.ofHours(1))));
    }
}
