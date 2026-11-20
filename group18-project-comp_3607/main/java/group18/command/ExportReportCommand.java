package group18.command;

import group18.model.ExportReason;
import group18.session.ProjectSession;
import group18.session.SessionExporter;
import group18.session.SessionRegistry;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Writes the reports of an open session on request (Tools &gt; Export Telemetry Report). The session carries on;
 * a later export of the same session replaces its files with up-to-date ones.
 */
public final class ExportReportCommand implements TelemetryCommand {

    private final SessionRegistry registry;
    private final SessionExporter exporter;
    private final String projectKey;
    private final Instant at;

    /** Files written by {@link #execute()}. */
    private final List<Path> writtenFiles = new ArrayList<>();

    /**
     * Creates the command.
     *
     * @param registry   the open sessions
     * @param exporter   writes the reports
     * @param projectKey the project to export, or {@code null} for every open project
     * @param at         when the export was requested
     */
    public ExportReportCommand(SessionRegistry registry, SessionExporter exporter, String projectKey, Instant at) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.exporter = Objects.requireNonNull(exporter, "exporter");
        this.projectKey = projectKey;
        this.at = Objects.requireNonNull(at, "at");
    }

    /**
     * Writes the reports.
     *
     * @throws IllegalStateException if there is no open project to export
     * @throws IOException           if a report file cannot be written
     */
    @Override
    public void execute() throws IOException {
        List<ProjectSession> targets = projectKey == null
                ? registry.all()
                : registry.find(projectKey).map(List::of).orElse(List.of());
        if (targets.isEmpty()) {
            throw new IllegalStateException("There is no open BlueJ project to export.");
        }
        for (ProjectSession session : targets) {
            writtenFiles.addAll(exporter.export(session, ExportReason.MANUAL_EXPORT, at));
        }
    }

    /**
     * The report files written when the command ran.
     *
     * @return the files
     */
    public List<Path> writtenFiles() {
        return List.copyOf(writtenFiles);
    }
}
