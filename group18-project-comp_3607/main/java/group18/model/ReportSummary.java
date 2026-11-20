package group18.model;

import java.util.List;

/**
 * Figures calculated from a session's events. Both the JSON {@code summary} object and the tables and charts of
 * the PDF report are built from this one object, so the two exports always agree.
 *
 * @param time        active coding time versus idle time
 * @param editing     typing cadence and edit frequency
 * @param compilation compilation outcomes and error frequencies
 * @param execution   program runs and runtime exceptions
 * @param lifecycle   class and method creation counts
 */
public record ReportSummary(TimeSummary time, EditingSummary editing, CompilationSummary compilation,
                            ExecutionSummary execution, LifecycleSummary lifecycle) {

    /**
     * Active coding time versus idle time.
     *
     * @param activeSeconds      seconds counted as active coding
     * @param idleSeconds        seconds counted as idle
     * @param activePercent      active share of the measured time, 0 to 100, one decimal place
     * @param idlePercent        idle share of the measured time, 0 to 100, one decimal place
     * @param idlePeriods        number of separate idle stretches
     * @param longestIdleSeconds length of the longest idle stretch
     */
    public record TimeSummary(long activeSeconds, long idleSeconds, double activePercent, double idlePercent,
                              int idlePeriods, long longestIdleSeconds) {
    }

    /**
     * Editor interaction figures.
     *
     * @param editCount                   number of separate edits
     * @param charactersTyped             characters typed
     * @param charactersPasted            characters pasted
     * @param charactersDeleted           characters deleted
     * @param typingCadenceCharsPerMinute characters typed per active minute
     * @param editsPerActiveMinute        edits per active minute (edit frequency)
     */
    public record EditingSummary(int editCount, long charactersTyped, long charactersPasted, long charactersDeleted,
                                 double typingCadenceCharsPerMinute, double editsPerActiveMinute) {
    }

    /**
     * Compilation figures.
     *
     * @param attempts                      compilations the user requested
     * @param successful                    attempts that succeeded
     * @param failed                        attempts that failed
     * @param successRatePercent            successful attempts as a percentage of all attempts
     * @param errorRatePercent              failed attempts as a percentage of all attempts
     * @param totalErrors                   errors reported across all attempts
     * @param averageErrorsPerFailedAttempt errors per failed attempt
     * @param averageSecondsBetweenAttempts mean gap between consecutive attempts
     * @param errorFrequency                errors grouped by code, most frequent first
     */
    public record CompilationSummary(int attempts, int successful, int failed, double successRatePercent,
                                     double errorRatePercent, int totalErrors, double averageErrorsPerFailedAttempt,
                                     double averageSecondsBetweenAttempts, List<ErrorFrequency> errorFrequency) {

        /** Makes the frequency list immutable. */
        public CompilationSummary {
            errorFrequency = errorFrequency == null ? List.of() : List.copyOf(errorFrequency);
        }
    }

    /**
     * How often one kind of compiler error occurred.
     *
     * @param errorCode      the javac diagnostic key
     * @param label          a short readable name for the error
     * @param count          how many times it occurred
     * @param percentOfTotal its share of all errors, 0 to 100
     * @param exampleMessage the first message seen with this code
     */
    public record ErrorFrequency(String errorCode, String label, int count, double percentOfTotal,
                                 String exampleMessage) {
    }

    /**
     * Program run figures.
     *
     * @param runs               method and constructor calls made from BlueJ
     * @param successfulRuns     calls that returned normally
     * @param runtimeExceptions  calls that ended with an exception
     * @param runSuccessPercent  successful calls as a percentage of all calls
     * @param exceptionFrequency exceptions grouped by type, most frequent first
     */
    public record ExecutionSummary(int runs, int successfulRuns, int runtimeExceptions, double runSuccessPercent,
                                   List<ExceptionFrequency> exceptionFrequency) {

        /** Makes the frequency list immutable. */
        public ExecutionSummary {
            exceptionFrequency = exceptionFrequency == null ? List.of() : List.copyOf(exceptionFrequency);
        }
    }

    /**
     * How often one kind of runtime exception occurred.
     *
     * @param exceptionType the exception class name
     * @param count         how many times it occurred
     */
    public record ExceptionFrequency(String exceptionType, int count) {
    }

    /**
     * Project structure changes.
     *
     * @param classesCreated  classes that appeared
     * @param classesModified source changes detected in existing classes
     * @param classesDeleted  classes removed
     * @param methodsAdded    methods and constructors that appeared
     * @param methodsRemoved  methods and constructors removed from existing classes
     */
    public record LifecycleSummary(int classesCreated, int classesModified, int classesDeleted, int methodsAdded,
                                   int methodsRemoved) {
    }
}
