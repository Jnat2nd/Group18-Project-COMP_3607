package group18.model;


public enum ExportReason {

    MANUAL_EXPORT("Manual export from the Tools menu"),

    PROJECT_CLOSED("Session ended: project closed"),

    BLUEJ_EXIT("Session ended: BlueJ closed");

    private final String description;

    ExportReason(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
