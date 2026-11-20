package group18.command;

import group18.model.LifecycleEvent.Action;
import group18.model.SessionInfo;
import group18.session.ProjectSession;
import group18.session.SessionRegistry;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * Starts a session for a project that was opened in BlueJ and records whether the project was opened or newly
 * created. Does nothing if the project already has a session, so it is safe to send more than once.
 */
public final class OpenProjectCommand implements TelemetryCommand {

    private final SessionRegistry registry;
    private final SessionInfo info;
    private final BiPredicate<Path, Instant> isNewProject;

    /**
     * Creates the command.
     *
     * @param registry     the open sessions
     * @param info         the session to start, including the project and start time
     * @param isNewProject decides from the project folder and opening time whether the project was just created
     */
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
