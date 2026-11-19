package group18.listener;

import group18.model.LifecycleEvent;
import group18.model.LifecycleEvent.Action;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Detects class and method lifecycle changes by comparing successive snapshots of a project's source code.
 *
 * <p>BlueJ is sampled every few seconds. Each sample is a map from class name to source text. Compared with the
 * previous sample, a new key is a created class, a missing key is a deleted class and a different text is a
 * modified class; the methods of created and modified classes are compared with {@link MethodScanner} to find
 * added and removed methods. The first sample only establishes the starting point, so classes that already exist
 * when a project is opened are not reported as created.</p>
 */
public final class SourceTracker {

    /** Session that the produced events belong to. */
    private final String sessionId;

    /** Receives the lifecycle events. */
    private final EventSink sink;

    /** Source text of each class in the previous sample, or {@code null} before the first sample. */
    private Map<String, String> previous;

    /** Method signatures of each class in the previous sample. */
    private final Map<String, Set<String>> methods = new HashMap<>();

    /**
     * Creates a tracker for one session.
     *
     * @param sessionId the session that produced events belong to
     * @param sink      receives the lifecycle events
     */
    public SourceTracker(String sessionId, EventSink sink) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    /**
     * Compares a new snapshot with the previous one and publishes a lifecycle event for every change.
     *
     * @param sources source text by class name; a {@code null} text is treated as empty
     * @param at      when the snapshot was taken
     * @return the classes whose text changed and by how much (created and deleted classes are not included)
     */
    public SampleDiff sample(Map<String, String> sources, Instant at) {
        Map<String, String> current = new TreeMap<>();
        sources.forEach((name, text) -> current.put(name, text == null ? "" : text));
        if (previous == null) {
            current.forEach((name, text) -> methods.put(name, MethodScanner.scan(text)));
            previous = current;
            return SampleDiff.NONE;
        }
        Map<String, TextDiff.Change> changes = new TreeMap<>();
        for (Map.Entry<String, String> entry : current.entrySet()) {
            String name = entry.getKey();
            String text = entry.getValue();
            String before = previous.get(name);
            if (before == null) {
                classCreated(name, text, at);
            } else if (!before.equals(text)) {
                classModified(name, text, at);
                changes.put(name, TextDiff.between(before, text));
            }
        }
        for (String name : previous.keySet()) {
            if (!current.containsKey(name)) {
                publish(Action.CLASS_DELETED, name, null, at);
                methods.remove(name);
            }
        }
        previous = current;
        return new SampleDiff(changes);
    }

    private void classCreated(String name, String text, Instant at) {
        publish(Action.CLASS_CREATED, name, null, at);
        Set<String> found = MethodScanner.scan(text);
        for (String method : found) {
            publish(Action.METHOD_ADDED, name, method, at);
        }
        methods.put(name, found);
    }

    private void classModified(String name, String text, Instant at) {
        publish(Action.CLASS_MODIFIED, name, null, at);
        Set<String> before = methods.getOrDefault(name, Set.of());
        Set<String> now = MethodScanner.scan(text);
        for (String method : now) {
            if (!before.contains(method)) {
                publish(Action.METHOD_ADDED, name, method, at);
            }
        }
        for (String method : before) {
            if (!now.contains(method)) {
                publish(Action.METHOD_REMOVED, name, method, at);
            }
        }
        methods.put(name, now);
    }

    private void publish(Action action, String target, String detail, Instant at) {
        sink.publish(new LifecycleEvent(sessionId, at, action, target, detail));
    }
}
