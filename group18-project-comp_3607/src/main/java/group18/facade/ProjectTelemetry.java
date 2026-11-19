package group18.facade;

import group18.model.ProjectRef;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;


public interface ProjectTelemetry {

    CompletableFuture<Void> projectOpened(ProjectRef project);

    CompletableFuture<List<Path>> projectClosed(String projectKey);

   
    CompletableFuture<List<Path>> openProjectsChanged(Set<String> openProjectKeys);
}
