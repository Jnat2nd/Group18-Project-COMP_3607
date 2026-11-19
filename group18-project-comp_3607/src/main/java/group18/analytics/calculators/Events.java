package group18.analytics.calculators;

import java.util.List;

import group18.model.TelemetryEvent;


final class Events {

    private Events() {
    }

    static <T extends TelemetryEvent> List<T> ofType(List<TelemetryEvent> all, Class<T> type) {
        return all.stream().filter(type::isInstance).map(type::cast).toList();
    }

    static double percent(long part, long whole) {
        return whole == 0 ? 0 : Math.round(part * 1000.0 / whole) / 10.0;
    }

    static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    static long seconds(long millis) {
        return Math.round(millis / 1000.0);
    }
}
