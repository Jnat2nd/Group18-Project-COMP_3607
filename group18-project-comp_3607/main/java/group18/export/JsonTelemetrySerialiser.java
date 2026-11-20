package group18.export;

// Sources:
//  https://stackoverflow.com/questions/4105795/pretty-print-json-in-java (pretty printing)
//  https://www.youtube.com/watch?v=JDL73kNTNrc (Jackson ObjectMapper and JavaTimeModule)

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import group18.model.CompilationEvent;
import group18.model.EditorEvent;
import group18.model.ExecutionEvent;
import group18.model.LifecycleEvent;
import group18.model.SessionReport;
import group18.model.TelemetryEvent;

/**
 * Converts a {@link SessionReport} to indented JSON text.
 *
 * <p>Times are written as ISO-8601 UTC strings (for example {@code 2026-11-09T18:00:00Z}) and every event carries
 * an {@code eventType} field naming its category. All JSON-specific choices are made here through Jackson
 * "mix-in" classes, so the model classes stay plain Java with no library annotations.</p>
 */
public final class JsonTelemetrySerialiser {

    /** The configured Jackson mapper; thread-safe once built. */
    private final ObjectMapper mapper;

    /** Creates a serialiser with the plugin's JSON layout. */
    public JsonTelemetrySerialiser() {
        this.mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .addMixIn(TelemetryEvent.class, TelemetryEventMixIn.class)
                .addMixIn(EditorEvent.class, EditorEventMixIn.class)
                .addMixIn(CompilationEvent.class, CompilationEventMixIn.class)
                .addMixIn(ExecutionEvent.class, ExecutionEventMixIn.class)
                .build();
    }

    /**
     * Converts a report to JSON.
     *
     * @param report the report
     * @return indented JSON text
     * @throws JsonProcessingException if the report cannot be converted
     */
    public String serialise(SessionReport report) throws JsonProcessingException {
        return mapper.writeValueAsString(report);
    }

    /**
     * Adds an {@code eventType} field to every event and leaves out the session ID, which is already in the
     * metadata of the file.
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "eventType")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = LifecycleEvent.class, name = "LIFECYCLE"),
            @JsonSubTypes.Type(value = EditorEvent.class, name = "EDITOR_INTERACTION"),
            @JsonSubTypes.Type(value = CompilationEvent.class, name = "COMPILATION"),
            @JsonSubTypes.Type(value = ExecutionEvent.class, name = "EXECUTION")})
    @JsonIgnoreProperties({"sessionId"})
    @JsonPropertyOrder({"timestamp"})
    private abstract static class TelemetryEventMixIn {
    }

    /** Adds the typing cadence of the interval. */
    private abstract static class EditorEventMixIn {
        @JsonProperty("typingCadenceCharsPerMinute")
        abstract double typingCadenceCharsPerMinute();
    }

    /** Adds the number of errors in the attempt. */
    private abstract static class CompilationEventMixIn {
        @JsonProperty("errorCount")
        abstract int errorCount();
    }

    /** Adds whether the call succeeded and a readable description of it. */
    private abstract static class ExecutionEventMixIn {
        @JsonProperty("invocation")
        abstract String invocation();

        @JsonProperty("successful")
        abstract boolean successful();
    }
}
