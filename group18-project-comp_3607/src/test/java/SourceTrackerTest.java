import group18.model.EditorEvent;
import group18.model.LifecycleEvent;
import group18.model.TelemetryEvent;

import group18.model.LifecycleEvent.ActionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SourceTrackerTest {

    final List<TelemetryEvent> events = new ArrayList<>();
    SourceTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new SourceTracker("s1", events::add);
    }

    LifecycleEvent lifecycle(int i) {
        return (LifecycleEvent) events.get(i);
    }

    EditorEvent editor(int i) {
        return (EditorEvent) events.get(i);
    }

    @Test
    void firstSampleIsOnlyABaseline() {
        tracker.sample(Map.of("p/A", "class A {}"), 0);
        assertTrue(events.isEmpty());
    }

    @Test
    void changedClassGivesModifiedAndActiveTime() {
        tracker.sample(Map.of("p/A", "class A {}"), 0);
        tracker.sample(Map.of("p/A", "class A { int x; }"), 5_000);

        assertEquals(2, events.size());
        assertEquals(ActionType.CLASS_MODIFIED, lifecycle(0).getActionType());
        assertEquals("p/A", lifecycle(0).getTargetName());
        assertEquals("EDITOR_INTERACTION", editor(1).getEventType());
        assertEquals(5_000, editor(1).getActiveTimeMs());
        assertEquals(0, editor(1).getIdleTimeMs());
        assertEquals(8, editor(1).getCharacterEditCount()); // " int x;" plus the space
    }

    @Test
    void newAndMissingClassesGiveCreatedAndDeleted() {
        tracker.sample(Map.of("p/A", "a"), 0);
        tracker.sample(Map.of("p/B", "b"), 5_000);

        assertEquals(ActionType.CLASS_CREATED, lifecycle(0).getActionType());
        assertEquals("p/B", lifecycle(0).getTargetName());
        assertEquals(ActionType.CLASS_DELETED, lifecycle(1).getActionType());
        assertEquals("p/A", lifecycle(1).getTargetName());
        assertEquals(0, editor(2).getCharacterEditCount());
    }

    @Test
    void unchangedSourceIsActiveFor60SecondsThenIdle() {
        Map<String, String> same = Map.of("p/A", "a");
        tracker.sample(same, 0);
        tracker.sample(same, 55_000);
        tracker.sample(same, 65_000);

        assertEquals(55_000, editor(0).getActiveTimeMs());
        assertEquals(0, editor(0).getIdleTimeMs());
        assertEquals(0, editor(1).getActiveTimeMs());
        assertEquals(10_000, editor(1).getIdleTimeMs());
    }

    @Test
    void anEditEndsIdleTime() {
        tracker.sample(Map.of("p/A", "a"), 0);
        tracker.sample(Map.of("p/A", "a"), 120_000);
        tracker.sample(Map.of("p/A", "ab"), 125_000);

        assertEquals(120_000, editor(0).getIdleTimeMs());
        assertEquals(5_000, editor(2).getActiveTimeMs());
    }

    @Test
    void rebaselineStopsAnOpenedProjectCountingAsCreated() {
        tracker.sample(Map.of("p/A", "a"), 0);
        tracker.rebaseline();
        tracker.sample(Map.of("p/A", "a", "q/B", "b"), 5_000); // project q was just opened
        assertTrue(events.isEmpty());
        tracker.sample(Map.of("p/A", "a", "q/B", "b"), 10_000);
        assertEquals(1, events.size());
        assertEquals("EDITOR_INTERACTION", events.get(0).getEventType());
    }

    @Test
    void rebaselineKeepsIdleTimeRunning() {
        tracker.sample(Map.of("p/A", "a"), 0);
        tracker.rebaseline();
        tracker.sample(Map.of("p/A", "a"), 70_000);
        tracker.sample(Map.of("p/A", "a"), 75_000);
        assertEquals(5_000, editor(0).getIdleTimeMs());
    }

    @Test
    void textDiffCountsInsertsRemovalsAndReplacements() {
        assertEquals(1, TextDiff.changedCharacters("abc", "abXc"));
        assertEquals(1, TextDiff.changedCharacters("abc", "aXc"));
        assertEquals(1, TextDiff.changedCharacters("abc", "ac"));
        assertEquals(3, TextDiff.changedCharacters("", "abc"));
        assertEquals(3, TextDiff.changedCharacters("abc", ""));
        assertEquals(0, TextDiff.changedCharacters("abc", "abc"));
        assertEquals(2, TextDiff.changedCharacters(null, "ab"));
    }
}
