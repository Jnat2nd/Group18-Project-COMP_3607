package group18.demo;

import java.time.LocalTime;
import java.util.function.Consumer;

/**
 * The three kinds of student session used to demonstrate and test the plugin, as the project handout asks:
 * fully correct, partially correct and completely incorrect interaction with the IDE.
 */
public enum DemoScenario {

    /** High activity; everything compiles and runs. */
    FULLY_CORRECT("Calculator", "DEMO-STUDENT-01", LocalTime.of(14, 0), DemoScripts::fullyCorrect,
            "High activity: steady typing, every compilation succeeds and every run returns normally."),

    /** Medium activity; mistakes are made and fixed. */
    PARTIALLY_CORRECT("BankAccount", "DEMO-STUDENT-02", LocalTime.of(16, 0), DemoScripts::partiallyCorrect,
            "Medium activity: compiler errors and a runtime exception, all fixed by the end."),

    /** Low activity; nothing works. The student ID is left unset to show the placeholder. */
    COMPLETELY_INCORRECT("StudentGrades", "", LocalTime.of(19, 30), DemoScripts::completelyIncorrect,
            "Low activity: long idle periods, most compilations fail and every run throws an exception.");

    private final String projectName;
    private final String studentId;
    private final LocalTime startTime;
    private final Consumer<DemoSession> script;
    private final String description;

    DemoScenario(String projectName, String studentId, LocalTime startTime, Consumer<DemoSession> script,
                 String description) {
        this.projectName = projectName;
        this.studentId = studentId;
        this.startTime = startTime;
        this.script = script;
        this.description = description;
    }

    /**
     * The BlueJ project the scenario works in.
     *
     * @return the project name
     */
    public String projectName() {
        return projectName;
    }

    /**
     * The student ID configured for the scenario.
     *
     * @return the ID, or an empty string for "not set"
     */
    public String studentId() {
        return studentId;
    }

    /**
     * The local time of day the scenario's session starts.
     *
     * @return the start time
     */
    public LocalTime startTime() {
        return startTime;
    }

    /**
     * What happens in the scenario.
     *
     * @return a one-sentence description
     */
    public String description() {
        return description;
    }

    /**
     * Plays the scenario's script. The project must not be closed by the script; the caller closes it.
     *
     * @param session drives the facade
     */
    public void play(DemoSession session) {
        script.accept(session);
    }
}
