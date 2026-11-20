package group18.listener;

import group18.model.EditorEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Measures typing, edit frequency, active coding time and idle time, and publishes them as {@link EditorEvent}s.
 *
 * <p>Edits arrive one at a time from BlueJ's editor (every keystroke, deletion or paste). Other activity, such as
 * compiling or running code, also counts as working. Each activity keeps the student "active" for the idle
 * threshold (60 seconds by default); time not covered by any activity is idle. When the sampler fires, the current
 * interval is closed and an event is published. Intervals with no activity at all are merged, so a long break
 * becomes a single idle event.</p>
 *
 * <p>If an edit is detected only by comparing source snapshots (for example, a file changed outside BlueJ), the
 * size of the change is used instead of keystrokes.</p>
 */
public final class EditorActivityTracker {

    /** Time without any activity after which the student counts as idle. */
    public static final Duration DEFAULT_IDLE_THRESHOLD = Duration.ofSeconds(60);

    /** A single edit that inserts more characters than this is counted as pasted rather than typed. */
    public static final int PASTE_THRESHOLD = 30;

    /** Session that the produced events belong to. */
    private final String sessionId;

    /** Receives the editor events. */
    private final EventSink sink;

    /** How long one activity keeps the student active, in milliseconds. */
    private final long idleThresholdMs;

    /** Start of the interval being measured. */
    private Instant intervalStart;

    /** The latest activity before the current interval, or {@code null} if there was none. */
    private Instant lastActivity;

    /** Times of activity inside the current interval. */
    private final List<Instant> activity = new ArrayList<>();

    /** Classes edited in the current interval. */
    private final Set<String> classesEdited = new TreeSet<>();

    /** Classes whose edits were reported keystroke by keystroke in the current interval. */
    private final Set<String> classesWithKeystrokes = new HashSet<>();

    /** Number of edits in the current interval. */
    private int edits;

    /** Characters typed in the current interval. */
    private int typed;

    /** Characters pasted in the current interval. */
    private int pasted;

    /** Characters deleted in the current interval. */
    private int deleted;

    /** Start of the run of idle-only intervals not yet published, or {@code null}. */
    private Instant idleSince;

    /** Length of the run of idle-only intervals not yet published. */
    private long pendingIdleMs;

    /**
     * Creates a tracker with the default idle threshold.
     *
     * @param sessionId the session that produced events belong to
     * @param sink      receives the editor events
     * @param start     when the session started; opening the project counts as activity
     */
    public EditorActivityTracker(String sessionId, EventSink sink, Instant start) {
        this(sessionId, sink, start, DEFAULT_IDLE_THRESHOLD);
    }

    /**
     * Creates a tracker.
     *
     * @param sessionId     the session that produced events belong to
     * @param sink          receives the editor events
     * @param start         when the session started; opening the project counts as activity
     * @param idleThreshold time without activity after which the student counts as idle
     * @throws IllegalArgumentException if the threshold is not positive
     */
    public EditorActivityTracker(String sessionId, EventSink sink, Instant start, Duration idleThreshold) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.sink = Objects.requireNonNull(sink, "sink");
        if (idleThreshold.isNegative() || idleThreshold.isZero()) {
            throw new IllegalArgumentException("idle threshold must be positive");
        }
        this.idleThresholdMs = idleThreshold.toMillis();
        this.intervalStart = Objects.requireNonNull(start, "start");
        this.lastActivity = start;
    }

    /**
     * Records one edit made in BlueJ's editor.
     *
     * @param className    the class being edited
     * @param removedChars characters removed by the edit
     * @param insertedChars characters inserted by the edit
     * @param at           when the edit happened
     */
    public void recordEdit(String className, int removedChars, int insertedChars, Instant at) {
        edits++;
        if (insertedChars > PASTE_THRESHOLD) {
            pasted += insertedChars;
        } else {
            typed += Math.max(0, insertedChars);
        }
        deleted += Math.max(0, removedChars);
        classesEdited.add(className);
        classesWithKeystrokes.add(className);
        noteActivity(at);
    }

    /**
     * Records activity that is not an edit, such as compiling or running code.
     *
     * @param at when it happened
     */
    public void noteActivity(Instant at) {
        if (at.isBefore(intervalStart)) {
            if (lastActivity == null || at.isAfter(lastActivity)) {
                lastActivity = at;
            }
        } else {
            activity.add(at);
        }
    }

    /**
     * Ends the current interval, publishes what happened in it and starts the next one.
     *
     * @param end  when the interval ends
     * @param diff source changes found by comparing snapshots; used for classes that had no keystroke events
     */
    public void closeInterval(Instant end, SampleDiff diff) {
        for (Map.Entry<String, TextDiff.Change> change : diff.changes().entrySet()) {
            if (!classesWithKeystrokes.contains(change.getKey())) {
                edits++;
                typed += change.getValue().inserted();
                deleted += change.getValue().removed();
                classesEdited.add(change.getKey());
                activity.add(end);
            }
        }
        Instant stop = end.isBefore(intervalStart) ? intervalStart : end;
        long intervalMs = Duration.between(intervalStart, stop).toMillis();
        long activeMs = activeMillis(stop);
        if (activeMs == 0 && edits == 0) {
            if (idleSince == null) {
                idleSince = intervalStart;
            }
            pendingIdleMs += intervalMs;
        } else {
            publishPendingIdle();
            sink.publish(new EditorEvent(sessionId, stop, intervalMs, activeMs, intervalMs - activeMs, edits, typed,
                    pasted, deleted, new ArrayList<>(classesEdited)));
        }
        startNextInterval(stop);
    }

    /**
     * Closes the current interval and publishes any idle time still being merged. Called before a report is
     * exported so that the report includes everything up to that moment.
     *
     * @param end the export time
     */
    public void flush(Instant end) {
        closeInterval(end, SampleDiff.NONE);
        publishPendingIdle();
    }

    private void startNextInterval(Instant start) {
        for (Instant time : activity) {
            if (lastActivity == null || time.isAfter(lastActivity)) {
                lastActivity = time;
            }
        }
        activity.clear();
        classesEdited.clear();
        classesWithKeystrokes.clear();
        edits = 0;
        typed = 0;
        pasted = 0;
        deleted = 0;
        intervalStart = start;
    }

    private void publishPendingIdle() {
        if (idleSince != null && pendingIdleMs > 0) {
            sink.publish(EditorEvent.idle(sessionId, idleSince.plusMillis(pendingIdleMs), pendingIdleMs));
        }
        idleSince = null;
        pendingIdleMs = 0;
    }

    /**
     * Active time inside the current interval: the part of it covered by at least one activity window, where each
     * activity keeps the student active for the idle threshold.
     */
    private long activeMillis(Instant end) {
        long from = intervalStart.toEpochMilli();
        long to = end.toEpochMilli();
        List<Long> times = new ArrayList<>();
        if (lastActivity != null) {
            times.add(lastActivity.toEpochMilli());
        }
        activity.stream().map(Instant::toEpochMilli).sorted().forEach(times::add);
        long covered = from;
        long total = 0;
        for (long time : times) {
            long windowStart = Math.max(Math.min(time, to), covered);
            long windowEnd = Math.min(time + idleThresholdMs, to);
            if (windowEnd > windowStart) {
                total += windowEnd - windowStart;
                covered = windowEnd;
            }
        }
        return total;
    }
}
