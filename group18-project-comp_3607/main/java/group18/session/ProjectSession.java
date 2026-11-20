package group18.session;

import group18.analytics.TelemetryCollector;
import group18.analytics.TelemetryListener;
import group18.listener.CompilationRecorder;
import group18.listener.CompileSignal;
import group18.listener.EditorActivityTracker;
import group18.listener.EventSink;
import group18.listener.ExecutionRecorder;
import group18.listener.InvocationResult;
import group18.listener.SampleDiff;
import group18.listener.SourceTracker;
import group18.model.CompilationEvent;
import group18.model.ExecutionEvent;
import group18.model.LifecycleEvent;
import group18.model.ProjectRef;
import group18.model.SessionInfo;
import group18.model.TelemetryEvent;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The telemetry of one project from the moment it is opened until it is closed.
 *
 * <p>A session owns the trackers and recorders for its project. It is the subject of the <b>Observer</b> pattern:
 * every event they produce is passed to each registered {@link TelemetryListener}. Two listeners are always
 * registered: a {@link TelemetryCollector} that stores the events for reports, and one that counts compilations and
 * runs as activity for the active/idle time calculation. All calls are made from the single command thread (see
 * {@code group18.command.CommandProcessor}).</p>
 */
public final class ProjectSession {

    /** Facts fixed when the session started. */
    private final SessionInfo info;

    /** Stores every event recorded so far, in the order recorded. */
    private final TelemetryCollector collector = new TelemetryCollector();

    /** Observers notified of each new event, in registration order. */
    private final List<TelemetryListener> listeners = new CopyOnWriteArrayList<>();

    /** Detects created, modified and deleted classes and methods. */
    private final SourceTracker sources;

    /** Measures typing, edits, active time and idle time. */
    private final EditorActivityTracker activity;

    /** Records compilations. */
    private final CompilationRecorder compilations;

    /** Records method and constructor calls. */
    private final ExecutionRecorder executions;

    /**
     * Starts a session.
     *
     * @param info facts about the session; its start time is the time the project was opened
     */
    public ProjectSession(SessionInfo info) {
        this.info = Objects.requireNonNull(info, "info");
        EventSink sink = this::record;
        this.sources = new SourceTracker(info.sessionId(), sink);
        this.activity = new EditorActivityTracker(info.sessionId(), sink, info.startTime());
        this.compilations = new CompilationRecorder(info.sessionId(), sink);
        this.executions = new ExecutionRecorder(info.sessionId(), sink);
        listeners.add(collector);
        listeners.add(event -> {
            if (event instanceof CompilationEvent || event instanceof ExecutionEvent) {
                activity.noteActivity(event.timestamp());
            }
        });
    }

    /**
     * Registers an observer that is told about every event recorded from now on.
     *
     * @param listener the observer
     */
    public void addListener(TelemetryListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    /**
     * Facts fixed when the session started.
     *
     * @return the session information
     */
    public SessionInfo info() {
        return info;
    }

    /**
     * The project the session belongs to.
     *
     * @return the project
     */
    public ProjectRef project() {
        return info.project();
    }

    /**
     * Records an event by notifying every listener (which stores it and updates the activity tracker).
     *
     * @param event the event
     */
    public void record(TelemetryEvent event) {
        Objects.requireNonNull(event, "event");
        for (TelemetryListener listener : listeners) {
            listener.onEvent(event);
        }
    }

    /**
     * Records a project-level lifecycle change such as the project being opened or closed.
     *
     * @param action the project action
     * @param at     when it happened
     */
    public void recordProjectAction(LifecycleEvent.Action action, Instant at) {
        record(LifecycleEvent.of(info.sessionId(), at, action, info.project().name()));
    }

    /**
     * Processes a snapshot of the project's source code and closes the current activity interval.
     *
     * @param classSources source text by class name
     * @param at           when the snapshot was taken
     */
    public void sampleSources(Map<String, String> classSources, Instant at) {
        SampleDiff diff = sources.sample(classSources, at);
        activity.closeInterval(at, diff);
    }

    /**
     * Records one edit made in BlueJ's editor.
     *
     * @param className     the class being edited
     * @param removedChars  characters removed
     * @param insertedChars characters inserted
     * @param at            when the edit happened
     */
    public void recordEdit(String className, int removedChars, int insertedChars, Instant at) {
        activity.recordEdit(className, removedChars, insertedChars, at);
    }

    /**
     * Passes a compilation step to the compilation recorder.
     *
     * @param signal the step
     */
    public void handleCompileSignal(CompileSignal signal) {
        compilations.handle(signal);
    }

    /**
     * Whether a compilation of this project has started but not finished.
     *
     * @return {@code true} while compiling
     */
    public boolean compileInProgress() {
        return compilations.inProgress();
    }

    /**
     * Records a finished method or constructor call.
     *
     * @param invocation the call
     */
    public void recordInvocation(InvocationResult invocation) {
        executions.record(invocation);
    }

    /**
     * Closes the current activity interval so a report includes everything up to now.
     *
     * @param at the current time
     */
    public void flush(Instant at) {
        activity.flush(at);
    }

    /**
     * Every event recorded so far.
     *
     * @return an unmodifiable copy of the events
     */
    public List<TelemetryEvent> events() {
        return collector.snapshot();
    }
}
