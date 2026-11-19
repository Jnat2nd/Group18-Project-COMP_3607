package group18.analytics.calculators;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import group18.model.ExecutionEvent;
import group18.model.ReportSummary.ExceptionFrequency;
import group18.model.ReportSummary.ExecutionSummary;
import group18.model.TelemetryEvent;


public final class ExecutionMetricsCalculator implements MetricCalculator<ExecutionSummary> {

    public ExecutionMetricsCalculator() {
    }

    @Override
    public ExecutionSummary calculate(List<TelemetryEvent> events) {
        List<ExecutionEvent> runs = Events.ofType(events, ExecutionEvent.class);
        int successes = (int) runs.stream().filter(ExecutionEvent::successful).count();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ExecutionEvent run : runs) {
            if (run.exception() != null && !run.exception().type().isBlank()) {
                counts.merge(run.exception().type(), 1, Integer::sum);
            }
        }
        int exceptions = counts.values().stream().mapToInt(Integer::intValue).sum();
        List<ExceptionFrequency> frequency = counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new ExceptionFrequency(entry.getKey(), entry.getValue()))
                .toList();
        return new ExecutionSummary(runs.size(), successes, exceptions, Events.percent(successes, runs.size()),
                frequency);
    }
}
