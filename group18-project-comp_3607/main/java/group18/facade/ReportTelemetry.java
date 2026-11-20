package group18.facade;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Manual report export, used by the Tools menu item. */
public interface ReportTelemetry {

    /**
     * Writes the JSON and PDF reports of an open session now. The session continues afterwards.
     *
     * @param projectKey the project to export, or {@code null} for every open project
     * @return the report files written; completes exceptionally if there is nothing to export or writing fails
     */
    CompletableFuture<List<Path>> exportNow(String projectKey);
}
