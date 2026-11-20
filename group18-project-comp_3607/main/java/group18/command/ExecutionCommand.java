package group18.command;

import group18.listener.InvocationResult;
import group18.session.SessionRegistry;

import java.util.Objects;

/** Records a finished method or constructor call, with a summary of its console output, in the project's session. */
public final class ExecutionCommand implements TelemetryCommand {

    private final SessionRegistry registry;
    private final String projectKey;
    private final InvocationResult invocation;

    /**
     * Creates the command.
     *
     * @param registry   the open sessions
     * @param projectKey the project the call was made in
     * @param invocation the finished call
     */
    public ExecutionCommand(SessionRegistry registry, String projectKey, InvocationResult invocation) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.projectKey = Objects.requireNonNull(projectKey, "projectKey");
        this.invocation = Objects.requireNonNull(invocation, "invocation");
    }

    @Override
    public void execute() {
        registry.find(projectKey).ifPresent(session -> session.recordInvocation(invocation));
    }
}
