package group18.facade;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;


public interface ReportTelemetry {
    CompletableFuture<List<Path>> exportNow(String projectKey);
}
