package group18.analytics;

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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Builds a session report by running one {@link MetricCalculator} per part of the summary: active and idle time,
 * editing, compilation, execution, lifecycle counts and the progression timeline.
 *
 * <p>The aggregator only sorts the events, hands them to the calculators and assembles the results with the
 * session metadata. It has no other state and no side effects, so the same events always give the same report.
 * Both exporters use its output, so the JSON summary and the PDF tables always agree.</p>
 */
public final class TelemetryAggregator {

    /** Version of the JSON layout written by this aggregator. */
    public static final String SCHEMA_VERSION = "1.0";

    /** Name and version of the plugin, written to every report. */
    public static final String GENERATOR = "COMP3607 Group 18 BlueJ Telemetry 1.0.0";

    private final MetricCalculator<TimeSummary> time;
    private final MetricCalculator<EditingSummary> editing;
    private final MetricCalculator<CompilationSummary> compilation;
    private final MetricCalculator<ExecutionSummary> execution;
    private final MetricCalculator<LifecycleSummary> lifecycle;
    private final MetricCalculator<List<TimelineEntry>> timeline;

    /** Creates an aggregator with the standard calculators. */
    public TelemetryAggregator() {
        this(new TimeMetricsCalculator(), new EditingMetricsCalculator(), new CompilationMetricsCalculator(),
                new ExecutionMetricsCalculator(), new LifecycleMetricsCalculator(), new TimelineCalculator());
    }

    /**
     * Creates an aggregator with chosen calculators, for example to change how one figure is defined.
     *
     * @param time        calculates active and idle time
     * @param editing     calculates typing cadence and edit frequency
     * @param compilation calculates compile rates and error frequencies
     * @param execution   calculates run outcomes and exception frequencies
     * @param lifecycle   counts class and method changes
     * @param timeline    builds the progression timeline
     */
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

    /**
     * Builds the report for a session.
     *
     * @param info   facts fixed at the start of the session
     * @param events the events recorded so far, in any order
     * @param end    when the report is generated
     * @param reason why the report is generated
     * @return the report
     */
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
