package group18.model;

public record CompilationError (
    int lineNumber,
    String errorCode, 
    String errorMessage) {}
