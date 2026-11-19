package group18.listener;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;


public record SampleDiff(Map<String, TextDiff.Change> changes) {
    public static final SampleDiff NONE = new SampleDiff(Map.of());

    public SampleDiff {
        changes = Collections.unmodifiableMap(new TreeMap<>(changes));
    }

    public boolean isEmpty() {
        return changes.isEmpty();
    }
}
