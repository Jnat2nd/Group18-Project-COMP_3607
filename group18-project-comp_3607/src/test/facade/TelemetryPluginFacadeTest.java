package group18.facade;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import group18.command.CommandProcessor;
import group18.demo.SimulatedClock;
import group18.model.ProjectRef;
import group18.testsupport.TestData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


@DisplayName("TelemetryPluginFacade: end-to-end from BlueJ calls to JSON and PDF files")
class TelemetryPluginFacadeTest {

    private static final ZoneId ZONE = ZoneId.of("America/Port_of_Spain");

    @TempDir
    Path workspace;

    private SimulatedClock clock;
    private TelemetryPluginFacade facade;
    private ProjectRef project;

    @BeforeEach
    void setUp() throws IOException {
        clock = new SimulatedClock(TestData.START, ZONE);
        facade = TelemetryPluginFacade.builder()
                .clock(clock)
                .zone(ZONE)
                .studentId(() -> "816000123")
                .sessionIds(() -> "session-1")
                .newProjectTest((folder, time) -> true)
                .fallbackDirectory(workspace.resolve("fallback"))
                .processor(CommandProcessor.singleThreaded())
                .build();
        project = ProjectRef.of("Shapes", Files.createDirectories(workspace.resolve("Shapes")));
    }

    @AfterEach
    void tearDown() {
        facade.shutdown(Duration.ofSeconds(5));
    }

    private void after(long seconds) {
        clock.advance(Duration.ofSeconds(seconds));
    }

    private void simulateAShortSession() {
        Path circle = project.directory().resolve("Circle.java");
        facade.projectOpened(project);
        after(10);
        facade.sourcesSampled(project.key(), Map.of());
        after(2);
        for (int i = 0; i < 6; i++) {
            facade.textEdited(project.key(), "Circle", 0, 1);
        }
        after(8);
        facade.sourcesSampled(project.key(), Map.of("Circle", "class Circle { double area() { return 0 } }"));
        facade.compileStarted(List.of(circle));
        facade.compileError(circle, 1, "';' expected");
        facade.compileFinished(List.of(circle), false);
        after(10);
        facade.compileStarted(List.of(circle));
        facade.compileFinished(List.of(circle), true);
        facade.invocationFinished(project.key(), "Circle", "area", "NORMAL_EXIT", "0.0", List.of("Area: 0.0\n"));
    }

    @Test
    void manualExportWritesJsonAndPdfIntoTheProjectsTelemetryFolder() throws IOException {
        simulateAShortSession();
        List<Path> files = facade.exportNow(project.key()).join();

        Path folder = project.directory().resolve("telemetry");
        assertEquals(List.of(folder.resolve("Shapes_2026-11-09_14-00-00.json"),
                folder.resolve("Shapes_2026-11-09_14-00-00.pdf")), files);
        assertTrue(Files.size(files.get(1)) > 1000);

        JsonNode json = new ObjectMapper().readTree(files.get(0).toFile());
        assertAll(
                () -> assertEquals("816000123", json.at("/metadata/studentId").asText()),
                () -> assertEquals("MANUAL_EXPORT", json.at("/metadata/exportReason").asText()),
                () -> assertEquals(30, json.at("/metadata/sessionDurationSeconds").asLong()),
                () -> assertEquals(2, json.at("/metadata/totalCompilationAttempts").asInt()),
                () -> assertEquals(50.0, json.at("/summary/compilation/successRatePercent").asDouble()),
                () -> assertEquals("compiler.err.expected",
                        json.at("/summary/compilation/errorFrequency/0/errorCode").asText()),
                () -> assertEquals(6, json.at("/summary/editing/charactersTyped").asInt()),
                () -> assertEquals(1, json.at("/summary/execution/runs").asInt()),
                () -> assertEquals(1, json.at("/summary/lifecycle/classesCreated").asInt()),
                () -> assertEquals("area()", json.at("/timeline/2/detail").asText()),
                () -> assertEquals("PROJECT_CREATED", json.at("/timeline/0/action").asText()));
    }

    @Test
    void closingTheProjectEndsTheSessionWithAFinalReport() throws IOException {
        simulateAShortSession();
        after(5);
        List<Path> files = facade.projectClosed(project.key()).join();
        assertEquals(2, files.size());
        JsonNode json = new ObjectMapper().readTree(files.get(0).toFile());
        assertEquals("PROJECT_CLOSED", json.at("/metadata/exportReason").asText());
        assertEquals("PROJECT_CLOSED", json.at("/timeline").get(json.at("/timeline").size() - 1)
                .get("action").asText());
        CompletionException none = assertThrows(CompletionException.class,
                () -> facade.exportNow(project.key()).join());
        assertInstanceOf(IllegalStateException.class, none.getCause());
    }

    @Test
    void projectsThatAreNoLongerOpenAreClosed() {
        facade.projectOpened(project);
        after(5);
        assertTrue(facade.openProjectsChanged(Set.of(project.key())).join().isEmpty());
        assertEquals(2, facade.openProjectsChanged(Set.of()).join().size());
    }

    @Test
    void shutdownWritesTheReportsOfOpenProjects() {
        facade.projectOpened(project);
        after(5);
        assertTrue(facade.shutdown(Duration.ofSeconds(10)));
        assertTrue(Files.isDirectory(project.directory().resolve("telemetry")));
        try (var files = Files.list(project.directory().resolve("telemetry"))) {
            assertEquals(2, files.count());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void eachSessionGetsItsOwnFiles() {
        facade.projectOpened(project);
        after(60);
        List<Path> first = facade.projectClosed(project.key()).join();
        after(3600);
        facade.projectOpened(project);
        after(60);
        List<Path> second = facade.projectClosed(project.key()).join();
        assertEquals(2, first.size());
        assertEquals(2, second.size());
        assertTrue(first.stream().noneMatch(second::contains));
        assertTrue(second.get(0).getFileName().toString().startsWith("Shapes_2026-11-09_15-01-00"));
    }

    @Test
    void defaultBuilderGivesAWorkingFacade() {
        TelemetryPluginFacade defaults = TelemetryPluginFacade.builder().build();
        try {
            assertThrows(CompletionException.class, () -> defaults.exportNow(null).join());
        } finally {
            assertTrue(defaults.shutdown(Duration.ofSeconds(5)));
        }
    }

    @Test
    void customExportStrategyIsUsed() {
        TelemetryPluginFacade custom = TelemetryPluginFacade.builder()
                .clock(clock)
                .exportStrategy((report, folder) -> List.of(folder.resolve("custom.txt")))
                .newProjectTest((folder, time) -> false)
                .build();
        try {
            custom.projectOpened(project);
            assertEquals(List.of(project.directory().resolve("telemetry").resolve("custom.txt")),
                    custom.exportNow(project.key()).join());
        } finally {
            custom.shutdown(Duration.ofSeconds(5));
        }
    }

    @Test
    void builderRejectsMissingSettings() {
        TelemetryPluginFacade.Builder builder = TelemetryPluginFacade.builder();
        assertThrows(NullPointerException.class, () -> builder.clock(null));
        assertThrows(NullPointerException.class, () -> builder.zone(null));
        assertThrows(NullPointerException.class, () -> builder.studentId(null));
        assertThrows(NullPointerException.class, () -> builder.sessionIds(null));
        assertThrows(NullPointerException.class, () -> builder.fallbackDirectory(null));
        assertThrows(NullPointerException.class, () -> builder.newProjectTest(null));
        assertThrows(NullPointerException.class, () -> builder.exportStrategy(null));
        assertThrows(NullPointerException.class, () -> builder.processor(null));
    }
}
