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

public final class SessionExporter {

    private final TelemetryAggregator aggregator;

    private final ExportStrategy strategy;

    private final OutputDirectories directories;

    public SessionExporter(TelemetryAggregator aggregator, ExportStrategy strategy, OutputDirectories directories) {
        this.aggregator = Objects.requireNonNull(aggregator, "aggregator");
        this.strategy = Objects.requireNonNull(strategy, "strategy");
        this.directories = Objects.requireNonNull(directories, "directories");
    }

    public List<Path> export(ProjectSession session, ExportReason reason, Instant at) throws IOException {
        session.flush(at);
        SessionReport report = aggregator.aggregate(session.info(), session.events(), at, reason);
        return strategy.export(report, directories.forSession(session.info()));
    }
}