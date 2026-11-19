package group18.demo;

import java.time.LocalTime;
import java.util.function.Consumer;

public enum DemoScenario {

    FULLY_CORRECT("Calculator", "DEMO-STUDENT-01", LocalTime.of(14, 0), DemoScripts::fullyCorrect, "High activity: steady typing, every compilation succeeds and every run returns normally."),

    PARTIALLY_CORRECT("BankAccount", "DEMO-STUDENT-02", LocalTime.of(16, 0), DemoScripts::partiallyCorrect,"Medium activity: compiler errors and a runtime exception, all fixed by the end."),

    COMPLETELY_INCORRECT("StudentGrades", "", LocalTime.of(19, 30), DemoScripts::completelyIncorrect, "Low activity: long idle periods, most compilations fail and every run throws an exception.");

    private final String projectName;
    private final String studentId;
    private final LocalTime startTime;
    private final Consumer<DemoSession> script;
    private final String description;

    DemoScenario(String projectName, String studentId, LocalTime startTime, Consumer<DemoSession> script, String description) {
        this.projectName = projectName;
        this.studentId = studentId;
        this.startTime = startTime;
        this.script = script;
        this.description = description;
    }

    public String projectName() {
        return projectName;
    }

    public String studentId() {
        return studentId;
    }

    public LocalTime startTime() {
        return startTime;
    }

    public String description() {
        return description;
    }

    public void play(DemoSession session) {
        script.accept(session);
    }
}