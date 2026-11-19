package group18.analytics;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import group18.analytics.calculators.CompilationMetricsCalculator;
import group18.analytics.calculators.EditingMetricsCalculator;
import group18.analytics.calculators.ExecutionMetricsCalculator;
import group18.analytics.calculators.LifecycleMetricsCalculator;
import group18.analytics.calculators.MetricCalculator;
import group18.analytics.calculators.TimeMetricsCalculator;
import group18.analytics.calculators.TimelineCalculator;
import group18.model.ExportReason;
import group18.model.ReportSummary;
import group18.model.ReportSummary.CompilationSummary;
import group18.model.ReportSummary.EditingSummary;
import group18.model.ReportSummary.ExecutionSummary;
import group18.model.ReportSummary.LifecycleSummary;
import group18.model.ReportSummary.TimeSummary;
import group18.model.SessionInfo;
import group18.model.SessionMetadata;
import group18.model.SessionReport;
import group18.model.TelemetryEvent;
import group18.model.TimelineEntry;


public final class TelemetryAggregator {

    public static final String SCHEMA_VERSION = "1.0";

    public static final String GENERATOR = "COMP3607 Group 18 BlueJ Telemetry 1.0.0";

    private final MetricCalculator<TimeSummary> time;
    private final MetricCalculator<EditingSummary> editing;
    private final MetricCalculator<CompilationSummary> compilation;
    private final MetricCalculator<ExecutionSummary> execution;
    private final MetricCalculator<LifecycleSummary> lifecycle;
    private final MetricCalculator<List<TimelineEntry>> timeline;


    public TelemetryAggregator() {
        this(new TimeMetricsCalculator(), new EditingMetricsCalculator(), new CompilationMetricsCalculator(),
                new ExecutionMetricsCalculator(), new LifecycleMetricsCalculator(), new TimelineCalculator());
    }

    public TelemetryAggregator(MetricCalculator<TimeSummary> time, MetricCalculator<EditingSummary> editing,
                               MetricCalculator<CompilationSummary> compilation,
                               MetricCalculator<ExecutionSummary> execution,
                               MetricCalculator<LifecycleSummary> lifecycle,
                               MetricCalculator<List<TimelineEntry>> timeline) {
        this.time = Objects.requireNonNull(time, "time");
        this.editing = Objects.requireNonNull(editing, "editing");
        this.compilation = Objects.requireNonNull(compilation, "compilation");
        this.execution = Objects.requireNonNull(execution, "execution");
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
        this.timeline = Objects.requireNonNull(timeline, "timeline");
    }

    public SessionReport aggregate(SessionInfo info, List<TelemetryEvent> events, Instant end, ExportReason reason) {
        List<TelemetryEvent> ordered = new ArrayList<>(events);
        ordered.sort(Comparator.comparing(TelemetryEvent::timestamp));

        CompilationSummary compilations = compilation.calculate(ordered);
        ReportSummary summary = new ReportSummary(time.calculate(ordered), editing.calculate(ordered), compilations,
                execution.calculate(ordered), lifecycle.calculate(ordered));

        long durationSeconds = Math.max(0, Duration.between(info.startTime(), end).getSeconds());
        SessionMetadata metadata = new SessionMetadata(SCHEMA_VERSION, GENERATOR, info.sessionId(), info.studentId(),
                info.project().name(), info.startTime(), end, durationSeconds, compilations.attempts(),
                ordered.size(), reason);
        return new SessionReport(metadata, summary, timeline.calculate(ordered), ordered);
    }
}
