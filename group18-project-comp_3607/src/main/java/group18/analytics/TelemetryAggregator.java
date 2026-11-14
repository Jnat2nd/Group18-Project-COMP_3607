package group18.analytics;

import group18.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class TelemetryAggregator {
    //holds incoming events during a session
    private final List<TelemetryEvent> events = new ArrayList<>();

    public void addEvent (TelemetryEvent event){
        if (event != null) {
            events.add(event);
        }
    }

    public List<TelemetryEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }


    //TODO: method relating to typing cadence?
}
