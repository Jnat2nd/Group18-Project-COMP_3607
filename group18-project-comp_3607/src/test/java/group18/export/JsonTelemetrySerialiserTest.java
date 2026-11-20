package group18.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import group18.model.SessionReport;
import group18.model.TelemetryEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("The JSON serialiser turns reports into valid, well-structured JSON")
class JsonTelemetrySerialiserTest {

    private final ObjectMapper reader = new ObjectMapper();
    private SessionReport report;
    private String json;
    private JsonNode root;

    @BeforeEach
    void setUp() throws Exception {
        report = SampleReports.typical("816000123");
        json = new JsonTelemetrySerialiser().serialise(report);
        root = reader.readTree(json);
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        Iterator<String> iterator = node.fieldNames();
        iterator.forEachRemaining(names::add);
        return names;
    }

    @Test
    void outputIsValidIndentedJsonWithTheFourTopLevelSections() {
        assertTrue(root.isObject());
        assertEquals(List.of("metadata", "summary", "timeline", "events"), fieldNames(root));
        assertTrue(json.contains("\n  \"metadata\" : {"), "output should be indented for readability");
    }

    @Test
    void metadataHasTheSessionDetailsTheHandoutAsksFor() {
        JsonNode m = root.get("metadata");
        assertAll(
                () -> assertEquals("1.0", m.get("schemaVersion").asText()),
                () -> assertEquals("816000123", m.get("studentId").asText()),
                () -> assertEquals("Shapes", m.get("projectName").asText()),
                () -> assertEquals("2026-11-09T18:00:00Z", m.get("startTime").asText()),
                () -> assertEquals("2026-11-09T18:02:05Z", m.get("endTime").asText()),
                () -> assertEquals(125, m.get("sessionDurationSeconds").asLong()),
                () -> assertEquals(2, m.get("totalCompilationAttempts").asInt()),
                () -> assertEquals(report.events().size(), m.get("totalEvents").asInt()),
                () -> assertEquals("MANUAL_EXPORT", m.get("exportReason").asText()));
    }

    @Test
    void summaryNumbersMatchTheReport() {
        JsonNode s = root.get("summary");
        assertEquals(report.summary().time().activeSeconds(), s.at("/time/activeSeconds").asLong());
        assertEquals(report.summary().time().idlePercent(), s.at("/time/idlePercent").asDouble());
        assertEquals(report.summary().editing().typingCadenceCharsPerMinute(),
                s.at("/editing/typingCadenceCharsPerMinute").asDouble());
        assertEquals(50.0, s.at("/compilation/successRatePercent").asDouble());
        assertEquals("compiler.err.cant.resolve", s.at("/compilation/errorFrequency/0/errorCode").asText());
        assertEquals(1, s.at("/execution/runtimeExceptions").asInt());
        assertEquals(2, s.at("/lifecycle/methodsAdded").asInt());
    }

    @Test
    void everyEventIsTimeStampedAndTypedWithoutRepeatingTheSessionId() {
        JsonNode events = root.get("events");
        assertEquals(report.events().size(), events.size());
        for (int i = 0; i < events.size(); i++) {
            TelemetryEvent event = report.events().get(i);
            JsonNode node = events.get(i);
            assertEquals(event.eventType().name(), node.get("eventType").asText());
            assertEquals(event.timestamp().toString(), node.get("timestamp").asText());
            assertEquals(List.of("eventType", "timestamp"), fieldNames(node).subList(0, 2));
            assertFalse(node.has("sessionId"));
        }
    }

    @Test
    void eventsCarryTheirDetailsAndDerivedValues() {
        JsonNode editor = find("EDITOR_INTERACTION", 1);
        assertEquals(25, editor.get("charactersTyped").asInt());
        assertEquals(40, editor.get("charactersPasted").asInt());
        assertEquals(150.0, editor.get("typingCadenceCharsPerMinute").asDouble());
        assertEquals("Circle", editor.get("classesEdited").get(0).asText());

        JsonNode failed = find("COMPILATION", 0);
        assertFalse(failed.get("successful").asBoolean());
        assertEquals(2, failed.get("errorCount").asInt());
        assertEquals(7, failed.at("/errors/0/lineNumber").asInt());
        assertEquals("compiler.err.cant.resolve", failed.at("/errors/0/errorCode").asText());

        JsonNode crash = find("EXECUTION", 1);
        assertEquals("Circle.scale", crash.get("invocation").asText());
        assertFalse(crash.get("successful").asBoolean());
        assertEquals("java.lang.ArithmeticException", crash.at("/exception/type").asText());
        assertEquals("Circle.java:12", crash.at("/exception/location").asText());
        assertTrue(find("EXECUTION", 0).get("exception").isNull());
    }

    @Test
    void specialCharactersAreEscapedAndSurviveARoundTrip() {
        assertEquals(SampleReports.TRICKY_MESSAGE, find("COMPILATION", 0).at("/errors/0/message").asText());
    }

    @Test
    void timelineHoldsTheMilestones() {
        JsonNode timeline = root.get("timeline");
        assertEquals(report.timeline().size(), timeline.size());
        assertEquals("METHOD_ADDED", timeline.get(2).get("action").asText());
        assertEquals("area()", timeline.get(2).get("detail").asText());
        assertTrue(timeline.get(0).get("detail").isNull());
    }

    @Test
    void emptySessionStillProducesTheFullStructure() throws Exception {
        JsonNode empty = reader.readTree(new JsonTelemetrySerialiser().serialise(SampleReports.empty()));
        assertEquals("STUDENT_ID_PLACEHOLDER", empty.at("/metadata/studentId").asText());
        assertEquals(0, empty.get("events").size());
        assertEquals(0.0, empty.at("/summary/compilation/successRatePercent").asDouble());
        assertTrue(empty.at("/summary/compilation/errorFrequency").isArray());
    }

    @Test
    void exportStrategyWritesTheJsonFileNamedForTheSession(@TempDir Path folder) throws Exception {
        ExportFileNames names = new ExportFileNames(ZoneId.of("America/Port_of_Spain"));
        List<Path> written = new JsonExportStrategy(new JsonTelemetrySerialiser(), names)
                .export(report, folder.resolve("telemetry"));
        assertEquals(List.of(folder.resolve("telemetry").resolve("Shapes_2026-11-09_14-00-00.json")), written);
        assertEquals(json, Files.readString(written.get(0), StandardCharsets.UTF_8));
        try (var files = Files.list(folder.resolve("telemetry"))) {
            assertEquals(1, files.count(), "no temporary files are left behind");
        }
    }

    private JsonNode find(String type, int occurrence) {
        int seen = 0;
        for (JsonNode event : root.get("events")) {
            if (type.equals(event.get("eventType").asText()) && seen++ == occurrence) {
                return event;
            }
        }
        throw new AssertionError("No " + type + " event number " + occurrence);
    }
}
