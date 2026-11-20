package group18.command;

import group18.session.SessionRegistry;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Gives a project's session a new snapshot of its source code, which detects lifecycle changes and closes the
 * current activity interval. */
public final class SampleSourcesCommand implements TelemetryCommand {

    private final SessionRegistry registry;
    private final String projectKey;
    private final Map<String, String> sources;
    private final Instant at;

    /**
     * Creates the command.
     *
     * @param registry   the open sessions
     * @param projectKey the project the snapshot belongs to
     * @param sources    source text by class name; copied
     * @param at         when the snapshot was taken
     */
    public SampleSourcesCommand(SessionRegistry registry, String projectKey, Map<String, String> sources,
                                Instant at) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.projectKey = Objects.requireNonNull(projectKey, "projectKey");
        this.sources = new HashMap<>(sources);
        this.at = Objects.requireNonNull(at, "at");
    }

    @Override
    public void execute() {
        registry.find(projectKey).ifPresent(session -> session.sampleSources(sources, at));
    }
}
