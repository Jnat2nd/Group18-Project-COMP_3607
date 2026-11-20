package group18.analytics.calculators;

import group18.model.TelemetryEvent;

import java.util.List;

/** Helpers shared by the calculators: filtering events by type and rounding figures. */
final class Events {

    private Events() {
    }

    /**
     * The events of one type, keeping their order.
     *
     * @param all  every event
     * @param type the event class wanted
     * @param <T>  the event type
     * @return the matching events
     */
    static <T extends TelemetryEvent> List<T> ofType(List<TelemetryEvent> all, Class<T> type) {
        return all.stream().filter(type::isInstance).map(type::cast).toList();
    }

    /**
     * A percentage rounded to one decimal place.
     *
     * @param part  the part
     * @param whole the whole
     * @return {@code part / whole * 100}, or 0 when {@code whole} is 0
     */
    static double percent(long part, long whole) {
        return whole == 0 ? 0 : Math.round(part * 1000.0 / whole) / 10.0;
    }

    /**
     * Rounds to one decimal place.
     *
     * @param value the value
     * @return the rounded value
     */
    static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    /**
     * Converts milliseconds to whole seconds, rounding to the nearest second.
     *
     * @param millis milliseconds
     * @return seconds
     */
    static long seconds(long millis) {
        return Math.round(millis / 1000.0);
    }
}
