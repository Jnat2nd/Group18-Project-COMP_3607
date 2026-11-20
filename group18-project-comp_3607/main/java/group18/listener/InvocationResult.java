package group18.listener;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * A finished method or constructor call as reported by BlueJ, expressed without BlueJ types.
 *
 * @param className    the class the call was made on
 * @param methodName   the method called, or {@code null} for a constructor
 * @param exitType     BlueJ's exit type name: {@code NORMAL_EXIT}, {@code EXCEPTION_EXIT}, {@code TERMINATED_EXIT}
 *                     or {@code UNKNOWN_EXIT}
 * @param result       BlueJ's text for the returned value, or {@code null}
 * @param consolePanes the full text of each pane of BlueJ's terminal window after the call (standard output first,
 *                     then standard error); empty if the terminal could not be read
 * @param timestamp    when the call finished
 */
public record InvocationResult(String className, String methodName, String exitType, String result,
                               List<String> consolePanes, Instant timestamp) {

    /**
     * Validates the components and copies the pane list.
     *
     * @throws NullPointerException if {@code className} or {@code timestamp} is null
     */
    public InvocationResult {
        Objects.requireNonNull(className, "className");
        Objects.requireNonNull(timestamp, "timestamp");
        consolePanes = consolePanes == null ? List.of()
                : consolePanes.stream().map(text -> text == null ? "" : text).toList();
    }
}
