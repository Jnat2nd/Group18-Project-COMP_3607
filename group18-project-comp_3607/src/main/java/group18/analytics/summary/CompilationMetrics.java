package group18.analytics.summary;

import java.util.Map;

public record CompilationMetrics(
    int attempts, 
    int successes, 
    int failures, 
    Map<String, Long> errorFrequency) {
    
        public double successRate() {
        return attempts == 0 ? 0.0 : (double) successes/attempts;
    }
}
