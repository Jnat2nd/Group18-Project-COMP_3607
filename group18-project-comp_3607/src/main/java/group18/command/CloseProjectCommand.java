package group18.command;

import group18.model.ExportReason;
import group18.model.LifecycleEvent.Action;
import group18.session.ProjectSession;
import group18.session.SessionExporter;
import group18.session.SessionRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


public final class CloseProjectCommand implements TelemetryCommand {

    private final SessionRegistry registry;
    private final SessionExporter exporter;
    private final String projectKey;
    private final ExportReason reason;
    private final Instant at;


    public CloseProjectCommand(SessionRegistry registry, SessionExporter exporter, String projectKey,
                               ExportReason reason, Instant at) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.exporter = Objects.requireNonNull(exporter, "exporter");
        this.projectKey = Objects.requireNonNull(projectKey, "projectKey");
        this.reason = Objects.requireNonNull(reason, "reason");
        this.at = Objects.requireNonNull(at, "at");
    }

    @Override
    public void execute() throws IOException {
        Optional<ProjectSession> found = registry.remove(projectKey);
        if (found.isEmpty()) {
            return;
        }
        ProjectSession session = found.get();
        boolean deleted = !Files.isDirectory(session.project().directory());
        session.recordProjectAction(deleted ? Action.PROJECT_DELETED : Action.PROJECT_CLOSED, at);
        writtenFiles = exporter.export(session, reason, at);
    }


    public List<Path> writtenFiles() {
        return writtenFiles;
    }
}
