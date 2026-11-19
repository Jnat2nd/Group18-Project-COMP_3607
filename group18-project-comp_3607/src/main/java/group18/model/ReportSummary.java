package group18.model;

import java.util.List;


public record ReportSummary(TimeSummary time, EditingSummary editing, CompilationSummary compilation,ExecutionSummary execution, LifecycleSummary lifecycle) {


    public record TimeSummary(long activeSeconds, long idleSeconds, double activePercent, double idlePercent,int idlePeriods, long longestIdleSeconds) {
    }

    public record EditingSummary(int editCount, long charactersTyped, long charactersPasted, long charactersDeleted,double typingCadenceCharsPerMinute, double editsPerActiveMinute) {
    }


    public record CompilationSummary(int attempts, int successful, int failed, double successRatePercent,double errorRatePercent, int totalErrors, double averageErrorsPerFailedAttempt, double averageSecondsBetweenAttempts, List<ErrorFrequency> errorFrequency) {
        public CompilationSummary {
            errorFrequency = errorFrequency == null ? List.of() : List.copyOf(errorFrequency);
        }
    }

   
    public record ErrorFrequency(String errorCode, String label, int count, double percentOfTotal,String exampleMessage) {
    }

    
    public record ExecutionSummary(int runs, int successfulRuns, int runtimeExceptions, double runSuccessPercent,List<ExceptionFrequency> exceptionFrequency) {
        public ExecutionSummary {
            exceptionFrequency = exceptionFrequency == null ? List.of() : List.copyOf(exceptionFrequency);
        }
    }


    public record ExceptionFrequency(String exceptionType, int count) {
    }


    public record LifecycleSummary(int classesCreated, int classesModified, int classesDeleted, int methodsAdded,int methodsRemoved) {
    }
}
