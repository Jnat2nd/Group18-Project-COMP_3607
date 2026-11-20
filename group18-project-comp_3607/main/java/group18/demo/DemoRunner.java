package group18.demo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;

import group18.command.CommandProcessor;
import group18.facade.TelemetryPluginFacade;
import group18.model.ProjectRef;

public final class DemoRunner {

    /** Time zone of the demo sessions (UWI St. Augustine). */
    public static final ZoneId ZONE = ZoneId.of("America/Port_of_Spain");

    /** Date of the demo sessions. */
    public static final LocalDate DATE = LocalDate.of(2026, 11, 9);

    /** Creates a runner. Each scenario gets its own facade, so the runner keeps no state. */
    public DemoRunner() {
    }

    /**
     * Runs one scenario.
     *
     * @param scenario   the scenario
     * @param outputRoot folder in which the scenario's project folder is created
     * @return the report files written
     * @throws IOException if the project folder cannot be created or the reports cannot be written
     */
    public List<Path> run(DemoScenario scenario, Path outputRoot) throws IOException {
        Path projectDirectory = Files.createDirectories(outputRoot.resolve(scenario.projectName()));
        SimulatedClock clock = new SimulatedClock(DATE.atTime(scenario.startTime()).atZone(ZONE).toInstant(), ZONE);
        TelemetryPluginFacade facade = TelemetryPluginFacade.builder()
                .clock(clock)
                .zone(ZONE)
                .studentId(scenario::studentId)
                .sessionIds(() -> UUID.nameUUIDFromBytes(scenario.name().getBytes(StandardCharsets.UTF_8)).toString())
                .newProjectTest((folder, openedAt) -> true)
                .fallbackDirectory(outputRoot.resolve("fallback"))
                .processor(CommandProcessor.singleThreaded())
                .build();
        DemoSession session = new DemoSession(facade, clock, ProjectRef.of(scenario.projectName(), projectDirectory));
        try {
            scenario.play(session);
            return session.close().join();
        } catch (CompletionException e) {
            throw e.getCause() instanceof IOException io ? io : new IOException(e.getCause());
        } finally {
            facade.shutdown(Duration.ofSeconds(30));
        }
    }

    /**
     * Runs every scenario.
     *
     * @param outputRoot folder in which a project folder per scenario is created
     * @return the report files written
     * @throws IOException if a report cannot be written
     */
    public List<Path> runAll(Path outputRoot) throws IOException {
        List<Path> written = new ArrayList<>();
        for (DemoScenario scenario : DemoScenario.values()) {
            written.addAll(run(scenario, outputRoot));
        }
        return written;
    }
}
