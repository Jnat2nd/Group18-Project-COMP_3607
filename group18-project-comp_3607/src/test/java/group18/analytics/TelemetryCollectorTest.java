package group18.analytics;

import group18.model.TelemetryEvent;
import group18.session.ProjectSession;
import group18.model.ProjectRef;
import group18.model.SessionInfo;
import group18.model.LifecycleEvent.Action;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static group18.testsupport.TestData.START;
import static group18.testsupport.TestData.compile;
import static group18.testsupport.TestData.run;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TelemetryCollector and TelemetryListener observe the events of a session")
class TelemetryCollectorTest {

    @Test
    void collectorKeepsEventsInOrderAndIgnoresNull() {
        TelemetryCollector collector = new TelemetryCollector();
        collector.onEvent(compile(1, true));
        collector.onEvent(null);
        collector.onEvent(run(2, "main"));
        assertEquals(2, collector.size());
        assertEquals(compile(1, true), collector.snapshot().get(0));
        assertThrows(UnsupportedOperationException.class, () -> collector.snapshot().clear());
    }

    @Test
    void snapshotIsNotAffectedByLaterEvents() {
        TelemetryCollector collector = new TelemetryCollector();
        collector.onEvent(compile(1, true));
        List<TelemetryEvent> before = collector.snapshot();
        collector.onEvent(compile(2, true));
        assertEquals(1, before.size());
    }

    @Test
    void sessionNotifiesRegisteredListenersOfEveryEvent() {
        ProjectSession session = new ProjectSession(new SessionInfo("s", null,
                ProjectRef.of("Zoo", Path.of("zoo")), START));
        List<TelemetryEvent> seen = new ArrayList<>();
        session.addListener(seen::add);
        session.recordProjectAction(Action.PROJECT_OPENED, START);
        session.record(compile(5, true));
        assertEquals(2, seen.size());
        assertEquals(seen, session.events());
        assertTrue(session.events().contains(compile(5, true)));
        assertThrows(NullPointerException.class, () -> session.addListener(null));
    }
}
