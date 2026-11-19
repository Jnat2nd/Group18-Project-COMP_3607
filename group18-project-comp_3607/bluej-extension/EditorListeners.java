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

final class EditorListeners {

    
    private final EditorTelemetry telemetry;

    private final Map<String, DocumentListener> listeners = new HashMap<>();

    private boolean unsupported;

    EditorListeners(EditorTelemetry telemetry) {
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry");
    }

 
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
