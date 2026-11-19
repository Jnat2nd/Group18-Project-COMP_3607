package group18.listener;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;


public record CompileSignal(Phase phase, List<Path> files, int lineNumber, String message, Instant timestamp) {


    public enum Phase {

        STARTED,
        ERROR,
        SUCCEEDED,
        FAILED
    }


    public CompileSignal {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(timestamp, "timestamp");
        files = files == null ? List.of() : List.copyOf(files);
    }


    public static CompileSignal started(List<Path> files, Instant at) {
        return new CompileSignal(Phase.STARTED, files, 0, null, at);
    }


    public static CompileSignal error(Path file, int line, String message, Instant at) {
        return new CompileSignal(Phase.ERROR, file == null ? List.of() : List.of(file), line, message, at);
    }

    public static CompileSignal finished(List<Path> files, boolean success, Instant at) {
        return new CompileSignal(success ? Phase.SUCCEEDED : Phase.FAILED, files, 0, null, at);
    }

    public boolean isFinish() {
        return phase == Phase.SUCCEEDED || phase == Phase.FAILED;
    }
}
