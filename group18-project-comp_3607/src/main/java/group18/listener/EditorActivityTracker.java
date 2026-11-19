package group18.listener;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import group18.model.EditorEvent;


public final class EditorActivityTracker {

    public static final Duration DEFAULT_IDLE_THRESHOLD = Duration.ofSeconds(60);
    public static final int PASTE_THRESHOLD = 30;
    private final String sessionId;
    private final EventSink sink;
    private final long idleThresholdMs;
    private Instant intervalStart,lastActivity, idleSince;
    private final List<Instant> activity = new ArrayList<>();
    private final Set<String> classesEdited = new TreeSet<>();
    private final Set<String> classesWithKeystrokes = new HashSet<>();
    private int edits, typed, pasted, deleted;   
    private long pendingIdleMs;


    public EditorActivityTracker(String sessionId, EventSink sink, Instant start) {
        this(sessionId, sink, start, DEFAULT_IDLE_THRESHOLD);
    }


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


    public void noteActivity(Instant at) {
        if (at.isBefore(intervalStart)) {
            if (lastActivity == null || at.isAfter(lastActivity)) {
                lastActivity = at;
            }
        } else {
            activity.add(at);
        }
    }


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
            sink.publish(new EditorEvent(sessionId, stop, intervalMs, activeMs, intervalMs - activeMs, edits, typed, pasted, deleted, new ArrayList<>(classesEdited)));
        }
        startNextInterval(stop);
    }


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