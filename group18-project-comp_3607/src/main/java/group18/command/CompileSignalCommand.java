package group18.command;

import java.util.List;
import java.util.Objects;

import group18.listener.CompileSignal;
import group18.session.ProjectSession;
import group18.session.SessionRegistry;


public final class CompileSignalCommand implements TelemetryCommand {

    private final SessionRegistry registry;
    private final CompileSignal signal;


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
