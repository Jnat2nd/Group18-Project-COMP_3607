package group18.model;

public class LifecycleEvent extends TelemetryEvent{
    public enum ActionType { CLASS_CREATED, CLASS_MODIFIED, CLASS_DELETED }

    private final ActionType actionType;
    private final String targetName;

    public LifecycleEvent(String sessionId, ActionType actionType, String targetName) {
        super(sessionId);
        this.actionType = actionType;
        this.targetName = targetName;
    }

    @Override 
    public String getEventType() {
        return "LIFECYCLE";
    }

    public ActionType getActionType() {
        return actionType;
    }

    public String getTargetName() {
        return targetName;
    }
    
}
