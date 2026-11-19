package group18.command;

import java.time.Instant;
import java.util.Objects;

import group18.session.SessionRegistry;


public final class TextEditCommand implements TelemetryCommand {

    private final SessionRegistry registry;
    private final String projectKey;
    private final String className;
    private final int removedChars;
    private final int insertedChars;
    private final Instant at;


    public TextEditCommand(SessionRegistry registry, String projectKey, String className, int removedChars,
                           int insertedChars, Instant at) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.projectKey = Objects.requireNonNull(projectKey, "projectKey");
        this.className = Objects.requireNonNull(className, "className");
        this.removedChars = removedChars;
        this.insertedChars = insertedChars;
        this.at = Objects.requireNonNull(at, "at");
    }

    @Override
    public void execute() {
        registry.find(projectKey).ifPresent(session ->
                session.recordEdit(className, removedChars, insertedChars, at));
    }
}
