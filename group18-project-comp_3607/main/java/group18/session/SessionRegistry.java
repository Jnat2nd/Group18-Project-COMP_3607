package group18.session;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The sessions of the projects currently open in BlueJ, keyed by {@link group18.model.ProjectRef#key()}.
 *
 * <p>Not thread-safe by design: only commands running on the single command thread use it, which keeps every
 * session free of locks.</p>
 */
public final class SessionRegistry {

    /** Open sessions by project key, in the order they were opened. */
    private final Map<String, ProjectSession> sessions = new LinkedHashMap<>();

    /** Creates an empty registry. */
    public SessionRegistry() {
    }

    /**
     * Finds the session of a project.
     *
     * @param projectKey the project key
     * @return the session, or empty if the project has no open session
     */
    public Optional<ProjectSession> find(String projectKey) {
        return Optional.ofNullable(sessions.get(projectKey));
    }

    /**
     * Whether a project has an open session.
     *
     * @param projectKey the project key
     * @return {@code true} if it has one
     */
    public boolean contains(String projectKey) {
        return sessions.containsKey(projectKey);
    }

    /**
     * Adds a session unless its project already has one.
     *
     * @param session the new session
     * @return {@code true} if it was added
     */
    public boolean add(ProjectSession session) {
        return sessions.putIfAbsent(session.project().key(), session) == null;
    }

    /**
     * Removes the session of a project.
     *
     * @param projectKey the project key
     * @return the removed session, or empty if there was none
     */
    public Optional<ProjectSession> remove(String projectKey) {
        return Optional.ofNullable(sessions.remove(projectKey));
    }

    /**
     * All open sessions.
     *
     * @return a copy of the sessions in the order they were opened
     */
    public List<ProjectSession> all() {
        return new ArrayList<>(sessions.values());
    }

    /**
     * The keys of all open sessions.
     *
     * @return a copy of the keys in the order the sessions were opened
     */
    public List<String> keys() {
        return new ArrayList<>(sessions.keySet());
    }

    /**
     * The sessions whose project folder contains at least one of the files.
     *
     * @param files source files, for example those being compiled
     * @return the matching sessions
     */
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
