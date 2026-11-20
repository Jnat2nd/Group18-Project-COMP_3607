package group18.analytics.calculators;

import group18.model.TelemetryEvent;

import java.util.List;

/**
 * Calculates one part of a session report from the session's events: the <b>Strategy</b> pattern applied to the
 * report figures.
 *
 * <p>{@link group18.analytics.TelemetryAggregator} holds one calculator per part of the summary and only depends on
 * this interface. A new figure is added by writing a new calculator; a calculator can be replaced (for example with
 * a different definition of idle time) without touching the aggregator or the exporters.</p>
 *
 * @param <R> the type of result the calculator produces
 */
@FunctionalInterface
public interface MetricCalculator<R> {

    /**
     * Calculates the result.
     *
     * @param events every event of the session, in time order; each calculator picks the types it needs
     * @return the result; never {@code null}
     */
    R calculate(List<TelemetryEvent> events);
}
