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

final class StudentIdPreferences implements PreferenceGenerator {

    static final String PROPERTY = "studentId";

    private final BlueJ bluej;
    private final TextField field = new TextField();
    private final VBox pane;

    private volatile String studentId;

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

    String studentId() {
        return studentId;
    }
}
