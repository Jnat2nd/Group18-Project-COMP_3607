package group18.model;

public class EditorEvent extends TelemetryEvent{
    private final long activeTimeMs;
    private final long idleTimeMs;
    private final int characterEditCount;
    //TODO: Property for typing cadence?

    public EditorEvent(String sessionId, long activeTimeMs, long idleTimeMs, int characterEditCount) {
        super(sessionId);
        this.activeTimeMs = activeTimeMs;
        this.idleTimeMs = idleTimeMs;
        this.characterEditCount = characterEditCount;
    }

    @Override 
    public String getEventType() {
        return "EDITOR_INTERACTION";
    }

    public long getActiveTimeMs() {
        return activeTimeMs;
    }

    public long getIdleTimeMs() {
        return idleTimeMs;
    }

    public int getCharacterEditCount() {
        return characterEditCount;
    }

    //TODO: getter for cadence property?
}
