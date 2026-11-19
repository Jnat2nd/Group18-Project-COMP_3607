package group18.facade;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface CompileTelemetry {

    
    CompletableFuture<Void> compileStarted(List<Path> files);


    CompletableFuture<Void> compileError(Path file, int lineNumber, String message);

    CompletableFuture<Void> compileFinished(List<Path> files, boolean successful);
}
