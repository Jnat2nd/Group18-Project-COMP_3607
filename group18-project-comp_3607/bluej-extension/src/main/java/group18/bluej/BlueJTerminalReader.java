package group18.bluej;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToolBar;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads the text of BlueJ's terminal window by walking its JavaFX scene graph. Must be called on the JavaFX thread.
 *
 * <p>The extension API has no access to the terminal, so the window is found by its title
 * ({@code "BlueJ: Terminal Window - <project>"}). BlueJ's terminal panes have a public {@code getLines()} method
 * that returns all of their text, including lines scrolled out of view; it is called by reflection so the plugin
 * does not depend on BlueJ's internal classes. If that method is not available, the visible text nodes are read
 * instead.</p>
 */
final class BlueJTerminalReader implements ConsoleTextSource {

    /** Creates a reader. It keeps no state; each call searches the open windows again. */
    BlueJTerminalReader() {
    }

    @Override
    public List<String> readPanes(String projectName) {
        Stage terminal = findTerminal(projectName);
        if (terminal == null || terminal.getScene() == null) {
            return List.of();
        }
        List<String> panes = new ArrayList<>();
        collectPanes(terminal.getScene().getRoot(), panes);
        if (panes.isEmpty()) {
            StringBuilder visible = new StringBuilder();
            collectVisibleText(terminal.getScene().getRoot(), visible);
            panes.add(visible.toString());
        }
        return panes;
    }

    /** The terminal window of the project, or of any project if none matches by name. */
    private static Stage findTerminal(String projectName) {
        Stage anyTerminal = null;
        for (Window window : Window.getWindows()) {
            if (window instanceof Stage stage && stage.getTitle() != null
                    && stage.getTitle().toLowerCase(Locale.ROOT).contains("terminal")) {
                if (projectName != null && stage.getTitle().endsWith(" - " + projectName)) {
                    return stage;
                }
                anyTerminal = anyTerminal == null ? stage : anyTerminal;
            }
        }
        return anyTerminal;
    }

    /** Adds the full text of every terminal pane found under {@code node}. */
    private static void collectPanes(Node node, List<String> panes) {
        if (node == null) {
            return;
        }
        Method getLines = linesMethod(node.getClass());
        if (getLines != null) {
            try {
                Object lines = getLines.invoke(node);
                if (lines instanceof List<?> list) {
                    List<String> text = new ArrayList<>();
                    list.forEach(line -> text.add(String.valueOf(line)));
                    panes.add(String.join("\n", text));
                    return;
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
            }
        }
        if (node instanceof TextInputControl input) {
            panes.add(input.getText() == null ? "" : input.getText());
            return;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectPanes(child, panes);
            }
        }
    }

    /** Finds a public no-argument {@code getLines()} declared by a public class in the node's class hierarchy. */
    private static Method linesMethod(Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            if (!Modifier.isPublic(current.getModifiers())) {
                continue;
            }
            try {
                Method method = current.getDeclaredMethod("getLines");
                if (Modifier.isPublic(method.getModifiers()) && List.class.isAssignableFrom(method.getReturnType())) {
                    return method;
                }
            } catch (NoSuchMethodException e) {
            }
        }
        return null;
    }

    /** Fallback: the text of the visible text nodes, skipping menus, tool bars and labels. */
    private static void collectVisibleText(Node node, StringBuilder out) {
        if (node == null || node instanceof MenuBar || node instanceof ToolBar || node instanceof Labeled) {
            return;
        }
        if (node instanceof Text text && text.getText() != null && !text.getText().isEmpty()) {
            out.append(text.getText());
            if (!text.getText().endsWith("\n")) {
                out.append('\n');
            }
            return;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectVisibleText(child, out);
            }
        }
    }
}
