package group18.facade;

import java.util.List;
import java.util.concurrent.CompletableFuture;


public interface ExecutionTelemetry {


    CompletableFuture<Void> invocationFinished(String projectKey, String className, String methodName,String exitType, String result, List<String> consolePanes);
}
