package group18.model;

public record CompilationError(String fileName, int lineNumber, String errorCode, String message) {
}
