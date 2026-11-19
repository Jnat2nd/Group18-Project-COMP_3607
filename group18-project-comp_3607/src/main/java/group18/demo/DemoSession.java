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

public final class DemoSession {

    public static final Duration SAMPLE_PERIOD = Duration.ofSeconds(10);

    private static final Duration COMPILE_TIME = Duration.ofMillis(800);

    private final TelemetryPluginFacade facade;
    private final SimulatedClock clock;
    private final ProjectRef project;

    private final Map<String, String> sources = new TreeMap<>();

    private Instant nextSample;

    private final StringBuilder standardOutput = new StringBuilder();

    private final StringBuilder standardError = new StringBuilder();

    public record CompileError(String file, int line, String message) {
    }

    public DemoSession(TelemetryPluginFacade facade, SimulatedClock clock, ProjectRef project) {
        this.facade = facade;
        this.clock = clock;
        this.project = project;
        this.nextSample = clock.instant().plus(SAMPLE_PERIOD);
    }

    public void open() {
        facade.projectOpened(project);
    }

    public void waitFor(Duration duration) {
        Instant end = clock.instant().plus(duration);
        while (!nextSample.isAfter(end)) {
            clock.advance(Duration.between(clock.instant(), nextSample));
            facade.sourcesSampled(project.key(), sources);
            nextSample = nextSample.plus(SAMPLE_PERIOD);
        }
        clock.advance(Duration.between(clock.instant(), end));
    }

    public void setSource(String className, String source) {
        sources.put(className, source);
    }

    public void deleteClass(String className) {
        sources.remove(className);
    }

    public void type(String className, int characters, Duration perCharacter) {
        for (int i = 0; i < characters; i++) {
            waitFor(perCharacter);
            facade.textEdited(project.key(), className, 0, 1);
        }
    }

    public void delete(String className, int characters, Duration perCharacter) {
        for (int i = 0; i < characters; i++) {
            waitFor(perCharacter);
            facade.textEdited(project.key(), className, 1, 0);
        }
    }

    public void paste(String className, int characters) {
        waitFor(Duration.ofSeconds(1));
        facade.textEdited(project.key(), className, 0, characters);
    }

    public void compile(List<String> files, CompileError... errors) {
        List<Path> paths = files.stream().map(project.directory()::resolve).toList();
        facade.compileStarted(paths);
        waitFor(COMPILE_TIME);
        for (CompileError error : errors) {
            facade.compileError(project.directory().resolve(error.file()), error.line(), error.message());
        }
        facade.compileFinished(paths, errors.length == 0);
    }

    public void run(String className, String methodName, String exitType, String output, String error,
                    String result) {
        waitFor(Duration.ofSeconds(4));
        standardOutput.append(output);
        standardError.append(error);
        facade.invocationFinished(project.key(), className, methodName, exitType, result,
                List.of(standardOutput.toString(), standardError.toString()));
    }

    public CompletableFuture<List<Path>> close() {
        return facade.projectClosed(project.key());
    }
}