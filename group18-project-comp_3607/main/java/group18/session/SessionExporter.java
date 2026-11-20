package group18.session;

import group18.analytics.TelemetryAggregator;
import group18.export.ExportStrategy;
import group18.model.ExportReason;
import group18.model.SessionReport;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Turns a session into report files: flushes its activity, aggregates its events and hands the report to the
 * configured {@link ExportStrategy}. It depends only on the strategy interface, never on JSON or PDF code
 * (Dependency Inversion principle).
 */
public final class SessionExporter {

    /** Calculates the report figures. */
    private final TelemetryAggregator aggregator;

    /** Writes the report files. */
    private final ExportStrategy strategy;

    /** Decides where the files go. */
    private final OutputDirectories directories;

    /**
     * Creates the exporter.
     *
     * @param aggregator  calculates the report figures
     * @param strategy    writes the report files
     * @param directories decides where the files go
     */
    public SessionExporter(TelemetryAggregator aggregator, ExportStrategy strategy, OutputDirectories directories) {
        this.aggregator = Objects.requireNonNull(aggregator, "aggregator");
        this.strategy = Objects.requireNonNull(strategy, "strategy");
        this.directories = Objects.requireNonNull(directories, "directories");
    }

    /**
     * Writes the reports of a session as it stands now.
     *
     * @param session the session
     * @param reason  why the report is written
     * @param at      the current time; the report covers the session up to this moment
     * @return the files written
     * @throws IOException if a report file cannot be written
     */
    public List<Path> export(ProjectSession session, ExportReason reason, Instant at) throws IOException {
        session.flush(at);
        SessionReport report = aggregator.aggregate(session.info(), session.events(), at, reason);
        return strategy.export(report, directories.forSession(session.info()));
    }
}
