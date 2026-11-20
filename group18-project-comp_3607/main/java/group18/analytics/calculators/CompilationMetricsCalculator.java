package group18.analytics.calculators;

import group18.listener.ErrorCodes;
import group18.model.CompilationError;
import group18.model.CompilationEvent;
import group18.model.ReportSummary.CompilationSummary;
import group18.model.ReportSummary.ErrorFrequency;
import group18.model.TelemetryEvent;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Compile success and error rates, and the compiler errors grouped by javac code, most frequent first. */
public final class CompilationMetricsCalculator implements MetricCalculator<CompilationSummary> {

    /** Creates the calculator. It has no state, so one instance can be reused. */
    public CompilationMetricsCalculator() {
    }

    @Override
    public CompilationSummary calculate(List<TelemetryEvent> events) {
        List<CompilationEvent> compiles = Events.ofType(events, CompilationEvent.class);
        int attempts = compiles.size();
        int successes = (int) compiles.stream().filter(CompilationEvent::successful).count();
        int failures = attempts - successes;
        int totalErrors = compiles.stream().mapToInt(CompilationEvent::errorCount).sum();
        double averageErrors = failures == 0 ? 0 : Events.round1((double) totalErrors / failures);
        double averageGap = 0;
        if (attempts > 1) {
            long spanMs = Duration.between(compiles.get(0).timestamp(), compiles.get(attempts - 1).timestamp())
                    .toMillis();
            averageGap = Events.round1(spanMs / 1000.0 / (attempts - 1));
        }

        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, String> examples = new LinkedHashMap<>();
        for (CompilationEvent compile : compiles) {
            for (CompilationError error : compile.errors()) {
                String code = error.errorCode() != null ? error.errorCode() : ErrorCodes.UNCLASSIFIED;
                counts.merge(code, 1, Integer::sum);
                examples.putIfAbsent(code, firstLine(error.message()));
            }
        }
        List<ErrorFrequency> frequency = counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new ErrorFrequency(entry.getKey(), ErrorCodes.label(entry.getKey()), entry.getValue(),
                        Events.percent(entry.getValue(), totalErrors), examples.get(entry.getKey())))
                .toList();
        return new CompilationSummary(attempts, successes, failures, Events.percent(successes, attempts),
                Events.percent(failures, attempts), totalErrors, averageErrors, averageGap, frequency);
    }

    private static String firstLine(String text) {
        if (text == null) {
            return "";
        }
        int newline = text.indexOf('\n');
        return (newline < 0 ? text : text.substring(0, newline)).strip();
    }
}
