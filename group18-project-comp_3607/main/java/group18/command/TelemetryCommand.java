package group18.command;

// Reference: https://stackoverflow.com/questions/18198176/java-8-lambda-function-that-throws-exception

/**
 * A request to change telemetry state, packaged as an object: the <b>Command</b> pattern.
 *
 * <p>BlueJ calls the plugin on its user-interface thread. Instead of doing the work there, each request becomes a
 * command that the {@link CommandProcessor} (the invoker) queues and runs, one at a time, on a background thread.
 * This keeps BlueJ responsive while PDFs are written and means session data is only ever touched by one thread,
 * so no locks are needed. New operations are added as new command classes without changing the processor.</p>
 */
@FunctionalInterface
public interface TelemetryCommand {

    /**
     * Carries out the request.
     *
     * @throws Exception if the request fails; the processor reports the failure through the command's future
     */
    void execute() throws Exception;
}
