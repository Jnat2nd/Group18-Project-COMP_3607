package group18.demo;

import group18.facade.TelemetryPluginFacade;
import group18.model.ProjectRef;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

/**
 * Plays the part of BlueJ for a scripted session: it makes exactly the facade calls the BlueJ adapters make, at
 * simulated times, including a source snapshot every ten seconds like the real sampler.
 */
public final class DemoSession {

    /** Same sampling period as the BlueJ extension. */
    public static final Duration SAMPLE_PERIOD = Duration.ofSeconds(10);

    /** Pause between the start and the end of a compilation. */
    private static final Duration COMPILE_TIME = Duration.ofMillis(800);

    private final TelemetryPluginFacade facade;
    private final SimulatedClock clock;
    private final ProjectRef project;

    /** The project's classes as the next snapshot will see them. */
    private final Map<String, String> sources = new TreeMap<>();

    /** When the sampler fires next. */
    private Instant nextSample;

    /** Everything printed to standard output so far, as BlueJ's terminal would show it. */
    private final StringBuilder standardOutput = new StringBuilder();

    /** Everything printed to standard error so far. */
    private final StringBuilder standardError = new StringBuilder();

    /**
     * A compiler error to report during {@link #compile(List, CompileError...)}.
     *
     * @param file    the source file name, for example {@code Dog.java}
     * @param line    the line number
     * @param message the compiler message
     */
    public record CompileError(String file, int line, String message) {
    }

    /**
     * Creates the session driver.
     *
     * @param facade  the facade to drive
     * @param clock   the simulated clock the facade uses
     * @param project the project being simulated
     */
    public DemoSession(TelemetryPluginFacade facade, SimulatedClock clock, ProjectRef project) {
        this.facade = facade;
        this.clock = clock;
        this.project = project;
        this.nextSample = clock.instant().plus(SAMPLE_PERIOD);
    }

    /** Opens the project, which starts the session. */
    public void open() {
        facade.projectOpened(project);
    }

    /**
     * Lets time pass, taking source snapshots on schedule.
     *
     * @param duration how long
     */
    public void waitFor(Duration duration) {
        Instant end = clock.instant().plus(duration);
        while (!nextSample.isAfter(end)) {
            clock.advance(Duration.between(clock.instant(), nextSample));
            facade.sourcesSampled(project.key(), sources);
            nextSample = nextSample.plus(SAMPLE_PERIOD);
        }
        clock.advance(Duration.between(clock.instant(), end));
    }

    /**
     * Makes a class's source text what the next snapshot sees: creating the class if it is new.
     *
     * @param className the class
     * @param source    its source text
     */
    public void setSource(String className, String source) {
        sources.put(className, source);
    }

    /**
     * Removes a class from the project.
     *
     * @param className the class
     */
    public void deleteClass(String className) {
        sources.remove(className);
    }

    /**
     * Types characters one at a time.
     *
     * @param className the class being edited
     * @param characters how many characters
     * @param perCharacter time between keystrokes
     */
    public void type(String className, int characters, Duration perCharacter) {
        for (int i = 0; i < characters; i++) {
            waitFor(perCharacter);
            facade.textEdited(project.key(), className, 0, 1);
        }
    }

    /**
     * Deletes characters one at a time with backspace.
     *
     * @param className  the class being edited
     * @param characters how many characters
     * @param perCharacter time between keystrokes
     */
    public void delete(String className, int characters, Duration perCharacter) {
        for (int i = 0; i < characters; i++) {
            waitFor(perCharacter);
            facade.textEdited(project.key(), className, 1, 0);
        }
    }

    /**
     * Pastes a block of text in one edit.
     *
     * @param className  the class being edited
     * @param characters how many characters are pasted
     */
    public void paste(String className, int characters) {
        waitFor(Duration.ofSeconds(1));
        facade.textEdited(project.key(), className, 0, characters);
    }

    /**
     * Compiles, reporting the given errors. With no errors the compilation succeeds.
     *
     * @param files  the files being compiled, for example {@code Dog.java}
     * @param errors the errors the compiler reports
     */
    public void compile(List<String> files, CompileError... errors) {
        List<Path> paths = files.stream().map(project.directory()::resolve).toList();
        facade.compileStarted(paths);
        waitFor(COMPILE_TIME);
        for (CompileError error : errors) {
            facade.compileError(project.directory().resolve(error.file()), error.line(), error.message());
        }
        facade.compileFinished(paths, errors.length == 0);
    }

    /**
     * Runs a method or constructor, printing to the terminal.
     *
     * @param className  the class
     * @param methodName the method, or {@code null} for a constructor
     * @param exitType   BlueJ's exit type name, for example {@code NORMAL_EXIT}
     * @param output     text printed to standard output
     * @param error      text printed to standard error, such as a stack trace
     * @param result     the returned value as BlueJ shows it, or {@code null}
     */
    public void run(String className, String methodName, String exitType, String output, String error,
                    String result) {
        waitFor(Duration.ofSeconds(4));
        standardOutput.append(output);
        standardError.append(error);
        facade.invocationFinished(project.key(), className, methodName, exitType, result,
                List.of(standardOutput.toString(), standardError.toString()));
    }

    /**
     * Closes the project, ending the session and writing its reports.
     *
     * @return the report files written
     */
    public CompletableFuture<List<Path>> close() {
        return facade.projectClosed(project.key());
    }
}
