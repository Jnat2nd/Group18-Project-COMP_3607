package group18.listener;

import group18.model.LifecycleEvent;
import group18.model.LifecycleEvent.Action;
import group18.model.TelemetryEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static group18.testsupport.TestData.SESSION;
import static group18.testsupport.TestData.at;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SourceTracker turns source snapshots into class and method lifecycle events")
class SourceTrackerTest {

    private final List<TelemetryEvent> events = new ArrayList<>();
    private SourceTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new SourceTracker(SESSION, events::add);
    }

    private List<String> actions() {
        return events.stream().map(e -> {
            LifecycleEvent l = (LifecycleEvent) e;
            return l.action() + " " + l.target() + (l.detail() == null ? "" : " " + l.detail());
        }).toList();
    }

    @Test
    void firstSampleOnlyRecordsTheStartingPoint() {
        SampleDiff diff = tracker.sample(Map.of("Dog", "class Dog { void bark() {} }"), at(0));
        assertTrue(events.isEmpty());
        assertTrue(diff.isEmpty());
    }

    @Test
    void newClassIsCreatedWithItsMethods() {
        tracker.sample(Map.of(), at(0));
        tracker.sample(Map.of("Dog", "class Dog { Dog() {} void bark() {} }"), at(10));
        assertEquals(List.of("CLASS_CREATED Dog", "METHOD_ADDED Dog Dog()", "METHOD_ADDED Dog bark()"), actions());
        assertTrue(events.stream().allMatch(e -> e.timestamp().equals(at(10)) && e.sessionId().equals(SESSION)));
    }

    @Test
    void changedClassIsModifiedAndMethodChangesAreReported() {
        tracker.sample(Map.of("Dog", "class Dog { void bark() {} }"), at(0));
        SampleDiff diff = tracker.sample(Map.of("Dog", "class Dog { void bark(int times) {} void sit() {} }"), at(10));
        assertEquals(List.of("CLASS_MODIFIED Dog", "METHOD_ADDED Dog bark(int)", "METHOD_ADDED Dog sit()",
                "METHOD_REMOVED Dog bark()"), actions());
        assertEquals(1, diff.changes().size());
        assertEquals(new TextDiff.Change(0, 23), diff.changes().get("Dog"));
    }

    @Test
    void missingClassIsDeletedWithoutMethodEvents() {
        tracker.sample(Map.of("Dog", "class Dog { void bark() {} }", "Cat", "class Cat {}"), at(0));
        tracker.sample(Map.of("Cat", "class Cat {}"), at(10));
        assertEquals(List.of("CLASS_DELETED Dog"), actions());
    }

    @Test
    void unchangedSourceProducesNoEvents() {
        tracker.sample(Map.of("Dog", "class Dog {}"), at(0));
        SampleDiff diff = tracker.sample(Map.of("Dog", "class Dog {}"), at(10));
        assertTrue(events.isEmpty());
        assertTrue(diff.isEmpty());
    }

    @Test
    void nullSourceTextIsTreatedAsEmpty() {
        Map<String, String> sources = new HashMap<>();
        sources.put("Dog", null);
        tracker.sample(sources, at(0));
        tracker.sample(Map.of("Dog", "class Dog {}"), at(10));
        assertEquals(List.of("CLASS_MODIFIED Dog"), actions());
    }

    @Test
    void renamingAClassIsADeleteAndACreate() {
        tracker.sample(Map.of("Dog", "class Dog {}"), at(0));
        tracker.sample(Map.of("Puppy", "class Puppy {}"), at(10));
        assertEquals(List.of("CLASS_CREATED Puppy", "CLASS_DELETED Dog"), actions());
        assertEquals(Action.CLASS_CREATED, ((LifecycleEvent) events.get(0)).action());
    }
}
