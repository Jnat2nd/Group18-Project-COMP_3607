package group18.analytics.calculators;

import group18.model.ExecutionEvent;
import group18.model.TelemetryEvent;
import group18.analytics.summary.ExecutionMetrics;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExecutionMetricsCalculator implements MetricCalculator<ExecutionMetrics> {

    @Override
    public ExecutionMetrics calculate(List<TelemetryEvent> events) {
        List<ExecutionEvent> runs = Events.ofType(events, ExecutionEvent.class);

        int successes = (int) runs.stream()
                .filter(ExecutionEvent::isSuccessful)
                .count();

        long totalRuntime = runs.stream()
                .mapToLong(ExecutionEvent::getExecutionDurationMs)
                .sum();

        Map<String, Long> exceptions = runs.stream()
                .map(ExecutionEvent::getExceptionType)
                .filter(type -> type != null && !type.isBlank())
                .collect(Collectors.groupingBy(type -> type, Collectors.counting()));

        return new ExecutionMetrics(
                runs.size(),
                successes,
                runs.size() - successes,
                totalRuntime,
                Map.copyOf(exceptions));
    }
}