package group18.listener;

import group18.model.EditorEvent;
import group18.model.TelemetryEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static group18.testsupport.TestData.SESSION;
import static group18.testsupport.TestData.START;
import static group18.testsupport.TestData.at;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("EditorActivityTracker measures typing, edits, active time and idle time")
class EditorActivityTrackerTest {

    private final List<TelemetryEvent> events = new ArrayList<>();
    private EditorActivityTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new EditorActivityTracker(SESSION, events::add, START);
    }

    private EditorEvent event(int index) {
        return (EditorEvent) events.get(index);
    }

    @Test
    void keystrokesAreCountedAsTypingInTheInterval() {
        for (int i = 1; i <= 5; i++) {
            tracker.recordEdit("Dog", 0, 1, at(i));
        }
        tracker.recordEdit("Dog", 2, 0, at(6));
        tracker.closeInterval(at(10), SampleDiff.NONE);

        EditorEvent e = event(0);
        assertEquals(10_000, e.intervalMs());
        assertEquals(10_000, e.activeMs());
        assertEquals(0, e.idleMs());
        assertEquals(6, e.editCount());
        assertEquals(5, e.charactersTyped());
        assertEquals(2, e.charactersDeleted());
        assertEquals(List.of("Dog"), e.classesEdited());
        assertEquals(30.0, e.typingCadenceCharsPerMinute(), 0.001);
    }

    @Test
    void longInsertionsCountAsPasted() {
        tracker.recordEdit("Dog", 0, EditorActivityTracker.PASTE_THRESHOLD + 1, at(1));
        tracker.recordEdit("Dog", 0, EditorActivityTracker.PASTE_THRESHOLD, at(2));
        tracker.closeInterval(at(10), SampleDiff.NONE);
        assertEquals(EditorActivityTracker.PASTE_THRESHOLD + 1, event(0).charactersPasted());
        assertEquals(EditorActivityTracker.PASTE_THRESHOLD, event(0).charactersTyped());
    }

    @Test
    void timeAfterTheThresholdWithoutActivityIsIdle() {
        tracker.closeInterval(at(50), SampleDiff.NONE);
        tracker.closeInterval(at(70), SampleDiff.NONE);
        assertEquals(50_000, event(0).activeMs());
        assertEquals(10_000, event(1).activeMs());
        assertEquals(10_000, event(1).idleMs());
    }

    @Test
    void consecutiveIdleIntervalsAreMergedIntoOneEvent() {
        tracker.closeInterval(at(60), SampleDiff.NONE);
        tracker.closeInterval(at(70), SampleDiff.NONE);
        tracker.closeInterval(at(80), SampleDiff.NONE);
        tracker.closeInterval(at(90), SampleDiff.NONE);
        assertEquals(1, events.size());
        tracker.recordEdit("Dog", 0, 1, at(95));
        tracker.closeInterval(at(100), SampleDiff.NONE);

        assertEquals(3, events.size());
        EditorEvent idle = event(1);
        assertTrue(idle.idleOnly());
        assertEquals(30_000, idle.idleMs());
        assertEquals(at(90), idle.timestamp());
        EditorEvent resumed = event(2);
        assertEquals(5_000, resumed.activeMs());
        assertEquals(5_000, resumed.idleMs());
    }

    @Test
    void compilingAndRunningKeepTheStudentActive() {
        tracker.closeInterval(at(60), SampleDiff.NONE);
        tracker.noteActivity(at(65));
        tracker.closeInterval(at(70), SampleDiff.NONE);
        assertEquals(5_000, event(1).activeMs());
        assertEquals(0, event(1).editCount());
    }

    @Test
    void activityReportedLateStillCountsFromWhenItHappened() {
        tracker.closeInterval(at(100), SampleDiff.NONE);
        tracker.noteActivity(at(95));
        tracker.closeInterval(at(110), SampleDiff.NONE);
        tracker.flush(at(110));
        EditorEvent last = (EditorEvent) events.get(events.size() - 1);
        assertEquals(10_000, last.activeMs());
    }

    @Test
    void snapshotChangesAreUsedWhenNoKeystrokesWereSeen() {
        tracker.recordEdit("Cat", 0, 1, at(1));
        SampleDiff diff = new SampleDiff(Map.of("Dog", new TextDiff.Change(3, 12), "Cat", new TextDiff.Change(0, 99)));
        tracker.closeInterval(at(10), diff);
        EditorEvent e = event(0);
        assertEquals(2, e.editCount());
        assertEquals(13, e.charactersTyped());
        assertEquals(3, e.charactersDeleted());
        assertEquals(List.of("Cat", "Dog"), e.classesEdited());
    }

    @Test
    void flushPublishesPendingIdleTime() {
        tracker.closeInterval(at(60), SampleDiff.NONE);
        tracker.closeInterval(at(300), SampleDiff.NONE);
        assertEquals(1, events.size());
        tracker.flush(at(310));
        assertEquals(2, events.size());
        assertEquals(250_000, event(1).idleMs());
        assertEquals(at(310), event(1).timestamp());
    }

    @Test
    void anIntervalEndingBeforeItStartedIsEmpty() {
        tracker.recordEdit("Dog", 0, 1, at(0));
        tracker.closeInterval(START.minusSeconds(5), SampleDiff.NONE);
        assertEquals(0, event(0).intervalMs());
        assertEquals(1, event(0).editCount());
    }

    @Test
    void customThresholdIsUsed() {
        EditorActivityTracker strict = new EditorActivityTracker(SESSION, events::add, START, Duration.ofSeconds(5));
        strict.closeInterval(at(10), SampleDiff.NONE);
        assertEquals(5_000, event(0).activeMs());
        assertThrows(IllegalArgumentException.class,
                () -> new EditorActivityTracker(SESSION, events::add, START, Duration.ZERO));
    }
}
