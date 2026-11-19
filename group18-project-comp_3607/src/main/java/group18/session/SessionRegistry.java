package group18.session;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public final class SessionRegistry {
    private final Map<String, ProjectSession> sessions = new LinkedHashMap<>();

    public SessionRegistry() {

    }
    public Optional<ProjectSession> find(String projectKey) {
        return Optional.ofNullable(sessions.get(projectKey));
    }

    public boolean contains(String projectKey) {
        return sessions.containsKey(projectKey);
    }


    public boolean add(ProjectSession session) {
        return sessions.putIfAbsent(session.project().key(), session) == null;
    }


    public Optional<ProjectSession> remove(String projectKey) {
        return Optional.ofNullable(sessions.remove(projectKey));
    }


    public List<ProjectSession> all() {
        return new ArrayList<>(sessions.values());
    }

    public List<String> keys() {
        return new ArrayList<>(sessions.keySet());
    }


    public List<ProjectSession> owning(Collection<Path> files) {
        List<ProjectSession> owners = new ArrayList<>();
        for (ProjectSession session : sessions.values()) {
            if (files.stream().anyMatch(session.project()::contains)) {
                owners.add(session);
            }
        }
        return owners;
    }
}
