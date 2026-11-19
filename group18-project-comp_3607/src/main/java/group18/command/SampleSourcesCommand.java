package group18.command;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import group18.session.SessionRegistry;


public final class SampleSourcesCommand implements TelemetryCommand {

    private final SessionRegistry registry;
    private final String projectKey;
    private final Map<String, String> sources;
    private final Instant at;


    public SampleSourcesCommand(SessionRegistry registry, String projectKey, Map<String, String> sources,Instant at) {
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
