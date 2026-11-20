package group18.facade;

import group18.model.ProjectRef;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Project lifecycle notifications, used by BlueJ's package listener and the source sampler. */
public interface ProjectTelemetry {

    /**
     * A project was opened (or created and opened). Starts its session; repeated calls are ignored.
     *
     * @param project the project
     * @return completes when the session exists
     */
    CompletableFuture<Void> projectOpened(ProjectRef project);

    /**
     * A project is closing. Ends its session and writes the final reports.
     *
     * @param projectKey the project's key
     * @return the report files written
     */
    CompletableFuture<List<Path>> projectClosed(String projectKey);

    /**
     * The set of projects open in BlueJ. Sessions of projects not in the set are ended and their reports written.
     *
     * @param openProjectKeys keys of the projects BlueJ currently has open
     * @return the report files written
     */
    CompletableFuture<List<Path>> openProjectsChanged(Set<String> openProjectKeys);
}
