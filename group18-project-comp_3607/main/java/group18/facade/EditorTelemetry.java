package group18.facade;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Source snapshots and editor edits, used by the source sampler and the editor document listeners. */
public interface EditorTelemetry {

    /**
     * A periodic snapshot of a project's source code.
     *
     * @param projectKey   the project's key
     * @param classSources source text by class name
     * @return completes when the snapshot has been processed
     */
    CompletableFuture<Void> sourcesSampled(String projectKey, Map<String, String> classSources);

    /**
     * One edit in BlueJ's editor: a keystroke, deletion or paste.
     *
     * @param projectKey    the project's key
     * @param className     the class being edited
     * @param removedChars  characters removed
     * @param insertedChars characters inserted
     * @return completes when the edit has been recorded
     */
    CompletableFuture<Void> textEdited(String projectKey, String className, int removedChars, int insertedChars);
}
