package group18.command;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.function.BiPredicate;

import group18.model.LifecycleEvent.Action;
import group18.model.SessionInfo;
import group18.session.ProjectSession;
import group18.session.SessionRegistry;


public final class OpenProjectCommand implements TelemetryCommand {

    private final SessionRegistry registry;
    private final SessionInfo info;
    private final BiPredicate<Path, Instant> isNewProject;


    public OpenProjectCommand(SessionRegistry registry, SessionInfo info, BiPredicate<Path, Instant> isNewProject) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.info = Objects.requireNonNull(info, "info");
        this.isNewProject = Objects.requireNonNull(isNewProject, "isNewProject");
    }

    @Override
    public void execute() {
        if (registry.contains(info.project().key())) {
            return;
        }
        ProjectSession session = new ProjectSession(info);
        registry.add(session);
        boolean created = isNewProject.test(info.project().directory(), info.startTime());
        session.recordProjectAction(created ? Action.PROJECT_CREATED : Action.PROJECT_OPENED, info.startTime());
    }
}
