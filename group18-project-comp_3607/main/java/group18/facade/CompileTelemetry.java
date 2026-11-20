package group18.facade;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** The steps of a user-requested compilation, used by BlueJ's compile listener. */
public interface CompileTelemetry {

    /**
     * A compilation began.
     *
     * @param files the source files being compiled
     * @return completes when recorded
     */
    CompletableFuture<Void> compileStarted(List<Path> files);

    /**
     * The compiler reported an error.
     *
     * @param file       the file with the error
     * @param lineNumber the line of the error
     * @param message    the compiler message
     * @return completes when recorded
     */
    CompletableFuture<Void> compileError(Path file, int lineNumber, String message);

    /**
     * A compilation finished.
     *
     * @param files      the source files that were compiled
     * @param successful whether it succeeded
     * @return completes when recorded
     */
    CompletableFuture<Void> compileFinished(List<Path> files, boolean successful);
}
