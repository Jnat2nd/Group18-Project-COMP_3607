package group18.session;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

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


public final class ProjectSession {
    private final SessionInfo info;
    private final TelemetryCollector collector = new TelemetryCollector();
    private final List<TelemetryListener> listeners = new CopyOnWriteArrayList<>();
    private final SourceTracker sources;
    private final EditorActivityTracker activity;
    private final CompilationRecorder compilations;
    private final ExecutionRecorder executions;

    
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

   
    public void addListener(TelemetryListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }
    
    public SessionInfo info() {
        return info;
    }
    
    public ProjectRef project() {
        return info.project();
    }
    
    public void record(TelemetryEvent event) {
        Objects.requireNonNull(event, "event");
        for (TelemetryListener listener : listeners) {
            listener.onEvent(event);
        }
    }
   
    public void recordProjectAction(LifecycleEvent.Action action, Instant at) {
        record(LifecycleEvent.of(info.sessionId(), at, action, info.project().name()));
    }
    
    public void sampleSources(Map<String, String> classSources, Instant at) {
        SampleDiff diff = sources.sample(classSources, at);
        activity.closeInterval(at, diff);
    }
   
    public void recordEdit(String className, int removedChars, int insertedChars, Instant at) {
        activity.recordEdit(className, removedChars, insertedChars, at);
    }
    
    public void handleCompileSignal(CompileSignal signal) {
        compilations.handle(signal);
    }

    public boolean compileInProgress() {
        return compilations.inProgress();
    }
   
    public void recordInvocation(InvocationResult invocation) {
        executions.record(invocation);
    }

    public void flush(Instant at) {
        activity.flush(at);
    }
    public List<TelemetryEvent> events() {
        return collector.snapshot();
    }
}
