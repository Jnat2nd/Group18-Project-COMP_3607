package group18.analytics.calculators;

import java.util.List;

import group18.model.EditorEvent;
import group18.model.ReportSummary.TimeSummary;
import group18.model.TelemetryEvent;


public final class TimeMetricsCalculator implements MetricCalculator<TimeSummary> {

    public TimeMetricsCalculator() {
    }

    @Override
    public TimeSummary calculate(List<TelemetryEvent> events) {
        long activeMs = 0;
        long idleMs = 0;
        int idlePeriods = 0;
        long longestIdleMs = 0;
        long currentIdleMs = 0;
        boolean inIdleStretch = false;
        for (EditorEvent event : Events.ofType(events, EditorEvent.class)) {
            activeMs += event.activeMs();
            idleMs += event.idleMs();
            if (event.idleMs() > 0) {
                if (!inIdleStretch) {
                    idlePeriods++;
                    currentIdleMs = 0;
                    inIdleStretch = true;
                }
                currentIdleMs += event.idleMs();
                longestIdleMs = Math.max(longestIdleMs, currentIdleMs);
            } else {
                inIdleStretch = false;
            }
        }
        double activePercent = Events.percent(activeMs, activeMs + idleMs);
        double idlePercent = activeMs + idleMs == 0 ? 0 : Events.round1(100 - activePercent);
        return new TimeSummary(Events.seconds(activeMs), Events.seconds(idleMs), activePercent, idlePercent,
                idlePeriods, Events.seconds(longestIdleMs));
    }
}
