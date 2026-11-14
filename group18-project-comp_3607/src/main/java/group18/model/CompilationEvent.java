package group18.model;

import java.util.List;

public class CompilationEvent extends TelemetryEvent{
    private final boolean successful;
    private final List<CompilationError> errors;
    
    public CompilationEvent(String sessionId, boolean successful, List<CompilationError> errors) {
        super(sessionId);
        this.successful = successful;
        this.errors  = errors != null ? errors : List.of(); 
    }

    @Override 
    public String getEventType() { 
        return "COMPILATION"; 
    }

    public boolean isSuccessful() { 
        return successful; 
    }
    
    public List<CompilationError> getErrors() { 
        return errors;
    }
}
