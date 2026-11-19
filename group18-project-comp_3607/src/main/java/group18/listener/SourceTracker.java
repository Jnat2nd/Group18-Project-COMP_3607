package group18.listener;

import group18.model.LifecycleEvent;
import group18.model.LifecycleEvent.Action;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;


public final class SourceTracker {

    
    private final String sessionId;

    private final EventSink sink;

    private Map<String, String> previous;

    private final Map<String, Set<String>> methods = new HashMap<>();

    public SourceTracker(String sessionId, EventSink sink) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.sink = Objects.requireNonNull(sink, "sink");
    }

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
