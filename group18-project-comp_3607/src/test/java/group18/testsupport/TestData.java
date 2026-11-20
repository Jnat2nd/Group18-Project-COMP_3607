package group18.testsupport;

import group18.model.CompilationError;
import group18.model.CompilationEvent;
import group18.model.EditorEvent;
import group18.model.ExecutionEvent;
import group18.model.ExecutionEvent.ConsoleOutput;
import group18.model.ExecutionEvent.ExceptionDetails;
import group18.model.ExecutionEvent.ExitType;
import group18.model.LifecycleEvent;
import group18.model.LifecycleEvent.Action;
import group18.model.ProjectRef;
import group18.model.SessionInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/**
 * Shared test data: a fixed session start time, a session, and short factory methods for events at a number of
 * seconds after the start.
 */
public final class TestData {

    /** Start of every test session: 9 Nov 2026, 14:00 in Trinidad. */
    public static final Instant START = Instant.parse("2026-11-09T18:00:00Z");

    /** Session ID used by test events. */
    public static final String SESSION = "session-1";

    private TestData() {
    }

    /**
     * A time relative to {@link #START}.
     *
     * @param seconds seconds after the start
     * @return the time
     */
    public static Instant at(double seconds) {
        return START.plusMillis(Math.round(seconds * 1000));
    }

    /**
     * A session for a project in the given folder.
     *
     * @param projectDirectory the project folder
     * @return the session information
     */
    public static SessionInfo session(Path projectDirectory) {
        return new SessionInfo(SESSION, "816000123", ProjectRef.of("Shapes", projectDirectory), START);
    }

    /**
     * A lifecycle event.
     *
     * @param seconds seconds after the start
     * @param action  the action
     * @param target  the class or project
     * @param detail  the method signature or {@code null}
     * @return the event
     */
    public static LifecycleEvent lifecycle(double seconds, Action action, String target, String detail) {
        return new LifecycleEvent(SESSION, at(seconds), action, target, detail);
    }

    /**
     * An editor interval ending at the given time.
     *
     * @param seconds  end of the interval, seconds after the start
     * @param activeMs active milliseconds
     * @param idleMs   idle milliseconds
     * @param edits    number of edits
     * @param typed    characters typed
     * @return the event
     */
    public static EditorEvent editor(double seconds, long activeMs, long idleMs, int edits, int typed) {
        return new EditorEvent(SESSION, at(seconds), activeMs + idleMs, activeMs, idleMs, edits, typed, 0, 0,
                List.of());
    }

    /**
     * A compilation attempt.
     *
     * @param seconds    when it finished, seconds after the start
     * @param successful whether it succeeded
     * @param errors     its errors
     * @return the event
     */
    public static CompilationEvent compile(double seconds, boolean successful, CompilationError... errors) {
        return new CompilationEvent(SESSION, at(seconds), successful, List.of("Shape.java"), List.of(errors));
    }

    /**
     * A compiler error.
     *
     * @param line    the line
     * @param code    the diagnostic code
     * @param message the message
     * @return the error
     */
    public static CompilationError error(int line, String code, String message) {
        return new CompilationError("Shape.java", line, code, message);
    }

    /**
     * A call that returned normally.
     *
     * @param seconds when it finished, seconds after the start
     * @param method  the method called
     * @return the event
     */
    public static ExecutionEvent run(double seconds, String method) {
        return new ExecutionEvent(SESSION, at(seconds), "Shape", method, ExitType.NORMAL_EXIT, null,
                ConsoleOutput.of("done", 200));
    }

    /**
     * A call that threw an exception.
     *
     * @param seconds       when it finished, seconds after the start
     * @param method        the method called
     * @param exceptionType the exception class
     * @return the event
     */
    public static ExecutionEvent crash(double seconds, String method, String exceptionType) {
        return new ExecutionEvent(SESSION, at(seconds), "Shape", method, ExitType.EXCEPTION_EXIT,
                new ExceptionDetails(exceptionType, "boom", "Shape.java:3"), ConsoleOutput.NONE);
    }

    /**
     * Reads a test resource as text.
     *
     * @param name the resource name, for example {@code terminal/arithmetic-exception.txt}
     * @return the content
     */
    public static String resource(String name) {
        try (InputStream in = TestData.class.getClassLoader().getResourceAsStream(name)) {
            if (in == null) {
                throw new IllegalArgumentException("Missing test resource " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
