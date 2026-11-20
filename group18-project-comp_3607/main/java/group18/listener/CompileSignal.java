package group18.listener;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * One step of a compilation as reported by BlueJ, expressed without BlueJ types.
 *
 * <p>BlueJ reports a compilation in several callbacks: it starts, each error is reported separately, and then it
 * succeeds or fails. The {@link CompilationRecorder} combines the steps into one {@code CompilationEvent}.</p>
 *
 * @param phase      which step this is
 * @param files      the source files involved; for an error, the file that has the error
 * @param lineNumber the error line for {@link Phase#ERROR}, otherwise 0
 * @param message    the error message for {@link Phase#ERROR}, otherwise {@code null}
 * @param timestamp  when BlueJ reported the step
 */
public record CompileSignal(Phase phase, List<Path> files, int lineNumber, String message, Instant timestamp) {

    /** The steps of a compilation. */
    public enum Phase {
        /** The compilation began. */
        STARTED,
        /** The compiler reported an error. */
        ERROR,
        /** The compilation finished without errors. */
        SUCCEEDED,
        /** The compilation finished with errors. */
        FAILED
    }

    /**
     * Validates the components and copies the file list.
     *
     * @throws NullPointerException if {@code phase} or {@code timestamp} is null
     */
    public CompileSignal {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(timestamp, "timestamp");
        files = files == null ? List.of() : List.copyOf(files);
    }

    /**
     * A compilation began.
     *
     * @param files the files being compiled
     * @param at    when it began
     * @return the signal
     */
    public static CompileSignal started(List<Path> files, Instant at) {
        return new CompileSignal(Phase.STARTED, files, 0, null, at);
    }

    /**
     * The compiler reported an error.
     *
     * @param file    the file with the error, or {@code null} if unknown
     * @param line    the line of the error
     * @param message the error message
     * @param at      when it was reported
     * @return the signal
     */
    public static CompileSignal error(Path file, int line, String message, Instant at) {
        return new CompileSignal(Phase.ERROR, file == null ? List.of() : List.of(file), line, message, at);
    }

    /**
     * A compilation finished.
     *
     * @param files   the files that were compiled
     * @param success whether it succeeded
     * @param at      when it finished
     * @return the signal
     */
    public static CompileSignal finished(List<Path> files, boolean success, Instant at) {
        return new CompileSignal(success ? Phase.SUCCEEDED : Phase.FAILED, files, 0, null, at);
    }

    /**
     * Whether this signal ends a compilation.
     *
     * @return {@code true} for {@link Phase#SUCCEEDED} and {@link Phase#FAILED}
     */
    public boolean isFinish() {
        return phase == Phase.SUCCEEDED || phase == Phase.FAILED;
    }
}
