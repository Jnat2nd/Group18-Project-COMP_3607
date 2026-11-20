package group18.command;

import group18.listener.CompileSignal;
import group18.session.ProjectSession;
import group18.session.SessionRegistry;

import java.util.List;
import java.util.Objects;

/**
 * Routes one step of a compilation to the session of the project being compiled, found from the paths of the
 * files involved. A step without files (BlueJ sometimes sends none) goes to the session that is mid-compilation.
 */
public final class CompileSignalCommand implements TelemetryCommand {

    private final SessionRegistry registry;
    private final CompileSignal signal;

    /**
     * Creates the command.
     *
     * @param registry the open sessions
     * @param signal   the compilation step
     */
    public CompileSignalCommand(SessionRegistry registry, CompileSignal signal) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.signal = Objects.requireNonNull(signal, "signal");
    }

    @Override
    public void execute() {
        List<ProjectSession> targets = registry.owning(signal.files());
        if (targets.isEmpty() && signal.phase() != CompileSignal.Phase.STARTED) {
            targets = registry.all().stream().filter(ProjectSession::compileInProgress).toList();
        }
        for (ProjectSession session : targets) {
            session.handleCompileSignal(signal);
        }
    }
}
