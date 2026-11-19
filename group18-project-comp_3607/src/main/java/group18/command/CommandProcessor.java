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


public final class CommandProcessor {

    private static final Logger LOG = Logger.getLogger(CommandProcessor.class.getName());

    private final ExecutorService executor;


    public CommandProcessor(ExecutorService executor) {
        this.executor = Objects.requireNonNull(executor, "executor");
    }


    public static CommandProcessor singleThreaded() {
        return new CommandProcessor(Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "telemetry-commands");
            thread.setDaemon(true);
            return thread;
        }));
    }


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
