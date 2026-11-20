package group18.facade;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Finished method and constructor calls, used by BlueJ's invocation listener. */
public interface ExecutionTelemetry {

    /**
     * A method or constructor call made from BlueJ finished.
     *
     * @param projectKey   the project's key
     * @param className    the class called
     * @param methodName   the method called, or {@code null} for a constructor
     * @param exitType     BlueJ's exit type name, for example {@code NORMAL_EXIT}
     * @param result       BlueJ's text for the returned value, or {@code null}
     * @param consolePanes the text of each terminal pane after the call; empty if unreadable
     * @return completes when recorded
     */
    CompletableFuture<Void> invocationFinished(String projectKey, String className, String methodName,
                                               String exitType, String result, List<String> consolePanes);
}
