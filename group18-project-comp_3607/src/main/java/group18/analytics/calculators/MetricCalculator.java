package group18.analytics.calculators;

import java.util.List;

import group18.model.TelemetryEvent;

public interface MetricCalculator<R> {
    public R calculate(List<TelemetryEvent> events);

}
