package group18.listener;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record InvocationResult(String className, String methodName, String exitType, String result, List<String> consolePanes, Instant timestamp) {

    public InvocationResult {
        Objects.requireNonNull(className, "className");
        Objects.requireNonNull(timestamp, "timestamp");
        consolePanes = consolePanes == null ? List.of() : consolePanes.stream().map(text -> text == null ? "" : text).toList();
    }
}
