package group18.command;

import group18.session.SessionRegistry;

import java.time.Instant;
import java.util.Objects;

/** Records one edit made in BlueJ's editor (a keystroke, deletion or paste) in the project's session. */
public final class TextEditCommand implements TelemetryCommand {

    private final SessionRegistry registry;
    private final String projectKey;
    private final String className;
    private final int removedChars;
    private final int insertedChars;
    private final Instant at;

    /**
     * Creates the command.
     *
     * @param registry      the open sessions
     * @param projectKey    the project being edited
     * @param className     the class being edited
     * @param removedChars  characters removed by the edit
     * @param insertedChars characters inserted by the edit
     * @param at            when the edit happened
     */
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
