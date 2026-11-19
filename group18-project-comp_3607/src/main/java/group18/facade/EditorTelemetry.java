package group18.facade;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface EditorTelemetry {
    CompletableFuture<Void> sourcesSampled(String projectKey, Map<String, String> classSources);
    CompletableFuture<Void> textEdited(String projectKey, String className, int removedChars, int insertedChars);
}
