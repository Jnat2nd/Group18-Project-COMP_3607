package group18.model;

/**
 * One compiler error reported during a compilation attempt.
 *
 * @param fileName   name of the source file that has the error, for example {@code Dog.java}
 * @param lineNumber the line BlueJ reported for the error (1-based), or 0 if unknown
 * @param errorCode  the javac diagnostic key recovered from the message, for example
 *                   {@code compiler.err.cant.resolve}, or {@code UNCLASSIFIED}
 * @param message    the compiler message exactly as BlueJ reported it
 */
public record CompilationError(String fileName, int lineNumber, String errorCode, String message) {
}
