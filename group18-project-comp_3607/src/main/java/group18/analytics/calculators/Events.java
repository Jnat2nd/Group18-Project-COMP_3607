package group18.analytics.calculators;

import java.util.List;

import group18.model.TelemetryEvent;

public class Events {
    private Events() {}
    static <T extends TelemetryEvent> List<T> ofType(List<TelemetryEvent> all, Class<T> type) {
        return all.stream().filter(type::isInstance).map(type::cast).toList();
    }
}
