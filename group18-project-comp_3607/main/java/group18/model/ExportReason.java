package group18.model;

/** Why a report was written. Recorded in the session metadata of every export. */
public enum ExportReason {

    /** The student chose "Export Telemetry Report" from BlueJ's Tools menu. */
    MANUAL_EXPORT("Manual export from the Tools menu"),

    /** The project was closed, which ends its session. */
    PROJECT_CLOSED("Session ended: project closed"),

    /** BlueJ was closed while the project was still open. */
    BLUEJ_EXIT("Session ended: BlueJ closed");

    /** Text shown in the PDF report. */
    private final String description;

    ExportReason(String description) {
        this.description = description;
    }

    /**
     * A readable description for the PDF report.
     *
     * @return the description
     */
    public String description() {
        return description;
    }
}
