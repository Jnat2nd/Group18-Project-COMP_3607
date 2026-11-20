package group18.bluej;

// Source: https://www.bluej.org/extensions/writingextensions2.html (PreferenceGenerator)

import bluej.extensions2.BlueJ;
import bluej.extensions2.PreferenceGenerator;
import group18.model.SessionInfo;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Adds a "Student ID" field to BlueJ's Tools &gt; Preferences &gt; Extensions tab. The ID is stored in BlueJ's
 * own settings and written into every report; until it is set, reports contain
 * {@link SessionInfo#STUDENT_ID_PLACEHOLDER}.
 */
final class StudentIdPreferences implements PreferenceGenerator {

    /** Name of the BlueJ setting that holds the ID. */
    static final String PROPERTY = "studentId";

    private final BlueJ bluej;
    private final TextField field = new TextField();
    private final VBox pane;

    /** The saved ID; read from the command thread, so kept in a volatile field. */
    private volatile String studentId;

    /**
     * Creates the preference panel and loads the saved ID.
     *
     * @param bluej BlueJ's extension API, used to load and save the setting
     */
    StudentIdPreferences(BlueJ bluej) {
        this.bluej = Objects.requireNonNull(bluej, "bluej");
        this.studentId = bluej.getExtensionPropertyString(PROPERTY, "");
        field.setPromptText("e.g. 816000000");
        Label hint = new Label("Written into the JSON and PDF telemetry reports of new sessions.");
        hint.setWrapText(true);
        pane = new VBox(6, new Label("Student ID"), field, hint);
    }

    @Override
    public Pane getWindow() {
        return pane;
    }

    @Override
    public void loadValues() {
        field.setText(studentId);
    }

    @Override
    public void saveValues() {
        String value = field.getText() == null ? "" : field.getText().strip();
        bluej.setExtensionPropertyString(PROPERTY, value);
        studentId = value;
    }

    /**
     * The saved student ID.
     *
     * @return the ID, or an empty string if none was saved (the session then uses the placeholder)
     */
    String studentId() {
        return studentId;
    }
}
