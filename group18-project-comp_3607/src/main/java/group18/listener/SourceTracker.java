package group18.listener;

import group18.model.EditorEvent;
import group18.model.LifecycleEvent;
import group18.model.TelemetryEvent;

import group18.model.LifecycleEvent.ActionType;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;


public class SourceTracker {

    /** Idle starts after this long without any change. */
    static final long IDLE_MS = 60_000;

    private final String sessionId;
    private final Consumer<TelemetryEvent> sink;
    private Map<String, String> last; // null = next sample is only a baseline
    private long lastSampleMs;
    private long lastEditMs;
    private boolean started;

    public SourceTracker(String sessionId, Consumer<TelemetryEvent> sink) {
        this.sessionId = sessionId;
        this.sink = sink;
    }

    //Call when a project opens or closes, so its classes appearing or vanishing are not reported as edits. //
    public synchronized void rebaseline() {
        last = null;
    }

    public synchronized void sample(Map<String, String> sources, long nowMs) {
        Map<String, String> now = new TreeMap<>(sources);
        if (last == null) {
            last = now;
            lastSampleMs = nowMs;
            if (!started) {
                started = true;
                lastEditMs = nowMs;
            }
            return;
        }

        boolean changed = false;
        int chars = 0;
        for (Map.Entry<String, String> e : now.entrySet()) {
            String before = last.get(e.getKey());
            if (before == null) {
                sink.accept(new LifecycleEvent(sessionId, ActionType.CLASS_CREATED, e.getKey()));
                changed = true;
            } else if (!before.equals(e.getValue())) {
                sink.accept(new LifecycleEvent(sessionId, ActionType.CLASS_MODIFIED, e.getKey()));
                chars += TextDiff.changedCharacters(before, e.getValue());
                changed = true;
            }
        }
        for (String key : last.keySet()) {
            if (!now.containsKey(key)) {
                sink.accept(new LifecycleEvent(sessionId, ActionType.CLASS_DELETED, key));
                changed = true;
            }
        }

        long interval = nowMs - lastSampleMs;
        if (changed) {
            lastEditMs = nowMs;
        }
        boolean idle = nowMs - lastEditMs > IDLE_MS;
        sink.accept(new EditorEvent(sessionId, idle ? 0 : interval, idle ? interval : 0, chars));
        last = now;
        lastSampleMs = nowMs;
    }
}
