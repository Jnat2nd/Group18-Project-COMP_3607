package group18.analytics.calculators;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import group18.analytics.summary.CompilationMetrics;
import group18.model.CompilationEvent;
import group18.model.TelemetryEvent;

public class CompilationMetricsCalculator implements MetricCalculator<CompilationMetrics>{
    @Override
    public CompilationMetrics calculate(List<TelemetryEvent> events) {
        List<CompilationEvent> compiles = Events.ofType(events, CompilationEvent.class);
        int successes = (int) compiles.stream().filter(CompilationEvent::isSuccessful).count();

        Map<String, Long> freq = compiles.stream()
            .flatMap(c -> c.getErrors().stream())
            .collect(Collectors.groupingBy(
                e -> e.errorCode() != null ? e.errorCode() : "UNKNOWN",
                Collectors.counting()));

        return new CompilationMetrics(compiles.size(), successes,
                                      compiles.size() - successes, Map.copyOf(freq));
    }
}
