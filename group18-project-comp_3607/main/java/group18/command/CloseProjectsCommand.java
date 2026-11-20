package group18.command;

import group18.model.ExportReason;
import group18.session.SessionExporter;
import group18.session.SessionRegistry;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Closes every session whose project key passes a test, by running a {@link CloseProjectCommand} for each one
 * (a "macro" command). Used when BlueJ exits (close all) and to tidy up sessions of projects BlueJ no longer has
 * open. Every selected session is closed even if writing an earlier one's reports fails.
 */
public final class CloseProjectsCommand implements TelemetryCommand {

    private final SessionRegistry registry;
    private final SessionExporter exporter;
    private final Predicate<String> selected;
    private final ExportReason reason;
    private final Instant at;

    /** Files written by {@link #execute()}. */
    private final List<Path> writtenFiles = new ArrayList<>();

    /**
     * Creates the command.
     *
     * @param registry the open sessions
     * @param exporter writes the reports
     * @param selected chooses, by project key, which sessions to close
     * @param reason   why the sessions end
     * @param at       when they end
     */
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

    /**
     * The report files written when the command ran.
     *
     * @return the files
     */
    public List<Path> writtenFiles() {
        return List.copyOf(writtenFiles);
    }
}
