package group18.bluej;

// Sources:
//  https://www.bluej.org/doc/extensionsAPI2/bluej/extensions2/editor/JavaEditor.html (addDocumentListener, API 3.3)
//  https://www.bluej.org/doc/extensionsAPI2/bluej/extensions2/editor/DocumentListener.html

import bluej.extensions2.editor.DocumentListener;
import bluej.extensions2.editor.JavaEditor;
import group18.facade.EditorTelemetry;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <b>Adapter</b>: attaches a document listener to each open Java editor so every keystroke, deletion or paste is
 * reported to the telemetry facade as it happens. Must be used on the JavaFX thread.
 *
 * <p>Each class gets one listener object that is reused. BlueJ removes every registration of a listener object
 * when it is removed, so "remove, then add" can be repeated on every sample and still leaves exactly one
 * registration, even after an editor is closed and opened again. On BlueJ versions without document listeners
 * (before 5.0.3), edits are still measured by comparing source snapshots.</p>
 */
final class EditorListeners {

    /** Receives the edits. */
    private final EditorTelemetry telemetry;

    /** One listener per project and class. */
    private final Map<String, DocumentListener> listeners = new HashMap<>();

    /** Set when the running BlueJ has no document listener support. */
    private boolean unsupported;

    /**
     * Creates the registry.
     *
     * @param telemetry receives the edits
     */
    EditorListeners(EditorTelemetry telemetry) {
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry");
    }

    /**
     * Makes sure an open editor reports its edits.
     *
     * @param projectKey the project's key
     * @param className  the class shown in the editor
     * @param editor     the open editor
     */
    void attach(String projectKey, String className, JavaEditor editor) {
        if (unsupported || editor == null) {
            return;
        }
        DocumentListener listener = listeners.computeIfAbsent(projectKey + "#" + className,
                key -> (start, replaced, replacement, linesRemoved, linesAdded) ->
                        telemetry.textEdited(projectKey, className, length(replaced), length(replacement)));
        try {
            editor.removeDocumentListener(listener);
            editor.addDocumentListener(listener);
        } catch (LinkageError e) {
            unsupported = true;
        }
    }

    private static int length(String text) {
        return text == null ? 0 : text.length();
    }
}
