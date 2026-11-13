package group18;

public record CompilationError (
    int lineNumber,
    String errorCode, 
    String errorMessage) {}
