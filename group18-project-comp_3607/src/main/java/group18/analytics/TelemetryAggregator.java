package group18.analytics;

import group18.analytics.calculators.MetricCalculator;
import group18.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class TelemetryAggregator {
    //holds incoming events during a session
    private final MetricCalculator<TimeMetrics> timeCalc;


    public void addEvent (TelemetryEvent event){
        if (event != null) {
            events.add(event);
        }
    }

    public List<TelemetryEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }


}
