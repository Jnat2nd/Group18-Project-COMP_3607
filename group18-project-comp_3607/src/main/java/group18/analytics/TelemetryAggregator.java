package group18.analytics;

import group18.analytics.calculators.CompilationMetricsCalculator;
import group18.analytics.calculators.ExecutionMetricsCalculator;
import group18.analytics.calculators.MetricCalculator;
import group18.analytics.calculators.TimeMetricsCalculator;
import group18.analytics.calculators.TimelineCalculator;
import group18.analytics.summary.CompilationMetrics;
import group18.analytics.summary.ExecutionMetrics;
import group18.analytics.summary.SessionSummary;
import group18.analytics.summary.TimeMetrics;
import group18.analytics.summary.TimelineEntry;
import group18.model.*;

import java.time.Instant;
import java.util.List;
import java.util.Comparator;

public class TelemetryAggregator {
    //holds incoming events during a session
    private final MetricCalculator<TimeMetrics> timeCalc;
    private final MetricCalculator<CompilationMetrics> compileCalc;
    private final MetricCalculator<ExecutionMetrics> execCalc;
    private final MetricCalculator<List<TimelineEntry>> timelineCalc;

    public TelemetryAggregator(MetricCalculator<TimeMetrics> timeCalc,
                               MetricCalculator<CompilationMetrics> compileCalc,
                               MetricCalculator<ExecutionMetrics> execCalc,
                               MetricCalculator<List<TimelineEntry>> timelineCalc) {
        this.timeCalc = timeCalc;
        this.compileCalc = compileCalc;
        this.execCalc = execCalc;
        this.timelineCalc = timelineCalc;
    }

    public static TelemetryAggregator withDefaults() {
        return new TelemetryAggregator(new TimeMetricsCalculator(),
            new CompilationMetricsCalculator(), new ExecutionMetricsCalculator(),
            new TimelineCalculator());
    }

    public SessionSummary aggregate(String sessionId, String studentId,
                                    List<TelemetryEvent> events) {
        Instant start = events.stream().map(TelemetryEvent::getTimestamp)
                              .min(Comparator.naturalOrder()).orElse(Instant.now());
        Instant end = events.stream().map(TelemetryEvent::getTimestamp)
                            .max(Comparator.naturalOrder()).orElse(start);

        return new SessionSummary(sessionId, studentId, start, end,
            timeCalc.calculate(events), compileCalc.calculate(events),
            execCalc.calculate(events), timelineCalc.calculate(events));
    }


}
