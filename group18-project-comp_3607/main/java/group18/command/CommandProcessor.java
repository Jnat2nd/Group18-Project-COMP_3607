package group18.command;

// Source: https://www.w3schools.com/java/java_threads.asp (threads)

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runs {@link TelemetryCommand}s in the order they are submitted: the invoker of the Command pattern.
 *
 * <p>A failing command is logged and reported through its future; it never stops later commands from running.</p>
 */
public final class CommandProcessor {

    private static final Logger LOG = Logger.getLogger(CommandProcessor.class.getName());

    /** Runs the commands; a single thread keeps them in order. */
    private final ExecutorService executor;

    /**
     * Creates a processor that runs commands on the given executor. The executor must run tasks one at a time and
     * in order for session data to stay consistent.
     *
     * @param executor the executor
     */
    public CommandProcessor(ExecutorService executor) {
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    /**
     * Creates a processor with its own background thread. The thread is a daemon, so it never keeps BlueJ running.
     *
     * @return the processor
     */
    public static CommandProcessor singleThreaded() {
        return new CommandProcessor(Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "telemetry-commands");
            thread.setDaemon(true);
            return thread;
        }));
    }

    /**
     * Queues a command.
     *
     * @param command the command
     * @return completes when the command has run; completes exceptionally if it failed or the processor was shut
     *         down
     */
    public CompletableFuture<Void> submit(TelemetryCommand command) {
        Objects.requireNonNull(command, "command");
        CompletableFuture<Void> result = new CompletableFuture<>();
        try {
            executor.execute(() -> run(command, result));
        } catch (RejectedExecutionException e) {
            result.completeExceptionally(e);
        }
        return result;
    }

    /**
     * Stops accepting commands and waits for the queued ones to finish.
     *
     * @param timeout the longest time to wait
     * @return {@code true} if every queued command finished in time
     */
    public boolean shutdown(Duration timeout) {
        executor.shutdown();
        try {
            return executor.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static void run(TelemetryCommand command, CompletableFuture<Void> result) {
        try {
            command.execute();
            result.complete(null);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Telemetry command failed: " + command.getClass().getSimpleName(), e);
            result.completeExceptionally(e);
        } catch (Error e) {
            result.completeExceptionally(e);
            throw e;
        }
    }
}
