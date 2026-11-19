package group18.listener;

// Sources:
//  https://www.bluej.org/doc/extensionsAPI2/bluej/extensions2/event/CompileListener.html

import group18.model.CompilationError;
import group18.model.CompilationEvent;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class CompilationRecorder {

    private final String sessionId;
    private final EventSink sink;
    private final Set<String> files = new LinkedHashSet<>();
    private final List<CompilationError> errors = new ArrayList<>();
    private boolean inProgress;

    public CompilationRecorder(String sessionId, EventSink sink) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    public void handle(CompileSignal signal) {
        switch (signal.phase()) {
            case STARTED -> start(signal);
            case ERROR -> error(signal);
            case SUCCEEDED, FAILED -> finish(signal);
        }
    }

    public boolean inProgress() {
        return inProgress;
    }

    private void start(CompileSignal signal) {
        files.clear();
        errors.clear();
        addFiles(signal.files());
        inProgress = true;
    }

    private void error(CompileSignal signal) {
        String file = signal.files().isEmpty() ? "" : fileName(signal.files().get(0));
        if (!file.isEmpty()) {
            files.add(file);
        }
        String message = signal.message() == null ? "" : signal.message().strip();
        errors.add(new CompilationError(file, Math.max(0, signal.lineNumber()), ErrorCodes.code(message), message));
        inProgress = true;
    }

    private void finish(CompileSignal signal) {
        addFiles(signal.files());
        boolean success = signal.phase() == CompileSignal.Phase.SUCCEEDED;
        sink.publish(new CompilationEvent(sessionId, signal.timestamp(), success, new ArrayList<>(files), errors));
        files.clear();
        errors.clear();
        inProgress = false;
    }

    private void addFiles(List<Path> paths) {
        for (Path path : paths) {
            files.add(fileName(path));
        }
    }

    private static String fileName(Path path) {
        Path name = path.getFileName();
        return name == null ? path.toString() : name.toString();
    }
}