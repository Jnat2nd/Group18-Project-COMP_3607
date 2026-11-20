package group18.listener;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * The classes whose source changed between two samples and how many characters changed in each.
 *
 * @param changes characters removed and inserted, by class name, sorted by class name
 */
public record SampleDiff(Map<String, TextDiff.Change> changes) {

    /** A sample in which no existing class changed. */
    public static final SampleDiff NONE = new SampleDiff(Map.of());

    /** Copies the map so the diff cannot change after it is created. */
    public SampleDiff {
        changes = Collections.unmodifiableMap(new TreeMap<>(changes));
    }

    /**
     * Whether no existing class changed.
     *
     * @return {@code true} if there are no changes
     */
    public boolean isEmpty() {
        return changes.isEmpty();
    }
}
