package group18.command;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import group18.model.ExportReason;
import group18.session.SessionExporter;
import group18.session.SessionRegistry;


public final class CloseProjectsCommand implements TelemetryCommand {

    private final SessionRegistry registry;
    private final SessionExporter exporter;
    private final Predicate<String> selected;
    private final ExportReason reason;
    private final Instant at;


    private final List<Path> writtenFiles = new ArrayList<>();

    public CloseProjectsCommand(SessionRegistry registry, SessionExporter exporter, Predicate<String> selected,
                                ExportReason reason, Instant at) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.exporter = Objects.requireNonNull(exporter, "exporter");
        this.selected = Objects.requireNonNull(selected, "selected");
        this.reason = Objects.requireNonNull(reason, "reason");
        this.at = Objects.requireNonNull(at, "at");
    }

    @Override
    public void execute() throws IOException {
        IOException failure = null;
        for (String key : registry.keys()) {
            if (!selected.test(key)) {
                continue;
            }
            CloseProjectCommand close = new CloseProjectCommand(registry, exporter, key, reason, at);
            try {
                close.execute();
                writtenFiles.addAll(close.writtenFiles());
            } catch (IOException e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    public List<Path> writtenFiles() {
        return List.copyOf(writtenFiles);
    }
}
