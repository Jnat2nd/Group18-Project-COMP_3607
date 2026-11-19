package group18.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


@DisplayName("Demo scenarios: three kinds of student session produce distinct reports")
class DemoScenarioTest {

    @TempDir
    Path workspace;

    private JsonNode play(DemoScenario scenario) throws IOException {
        List<Path> files = new DemoRunner().run(scenario, workspace);
        assertEquals(2, files.size());
        assertTrue(files.get(1).toString().endsWith(".pdf"));
        assertTrue(Files.size(files.get(1)) > 2000);
        return new ObjectMapper().readTree(files.get(0).toFile());
    }

    @Test
    void fullyCorrectSessionCompilesAndRunsEverything() throws IOException {
        JsonNode s = play(DemoScenario.FULLY_CORRECT).get("summary");
        assertAll(
                () -> assertEquals(100.0, s.at("/compilation/successRatePercent").asDouble()),
                () -> assertEquals(0, s.at("/compilation/totalErrors").asInt()),
                () -> assertEquals(100.0, s.at("/execution/runSuccessPercent").asDouble()),
                () -> assertEquals(0, s.at("/execution/runtimeExceptions").asInt()),
                () -> assertTrue(s.at("/time/activePercent").asDouble() > 75),
                () -> assertTrue(s.at("/editing/typingCadenceCharsPerMinute").asDouble() > 100),
                () -> assertTrue(s.at("/lifecycle/methodsAdded").asInt() >= 6));
    }

    @Test
    void partiallyCorrectSessionHasErrorsThatGetFixed() throws IOException {
        JsonNode report = play(DemoScenario.PARTIALLY_CORRECT);
        JsonNode s = report.get("summary");
        double success = s.at("/compilation/successRatePercent").asDouble();
        assertAll(
                () -> assertTrue(success > 0 && success < 100),
                () -> assertEquals(4, s.at("/compilation/errorFrequency").size()),
                () -> assertEquals("java.lang.IllegalArgumentException",
                        s.at("/execution/exceptionFrequency/0/exceptionType").asText()),
                () -> assertEquals(1, s.at("/time/idlePeriods").asInt()),
                () -> assertTrue(s.at("/editing/charactersPasted").asInt() > 0),
                () -> assertEquals(2, s.at("/lifecycle/classesCreated").asInt()));
        JsonNode lastCompilation = null;
        for (JsonNode event : report.get("events")) {
            if ("COMPILATION".equals(event.get("eventType").asText())) {
                lastCompilation = event;
            }
        }
        assertTrue(lastCompilation != null && lastCompilation.get("successful").asBoolean());
    }

    @Test
    void completelyIncorrectSessionIsMostlyIdleAndFailing() throws IOException {
        JsonNode report = play(DemoScenario.COMPLETELY_INCORRECT);
        JsonNode s = report.get("summary");
        assertAll(
                () -> assertEquals("STUDENT_ID_PLACEHOLDER", report.at("/metadata/studentId").asText()),
                () -> assertTrue(s.at("/compilation/successRatePercent").asDouble() <= 20),
                () -> assertTrue(s.at("/execution/runSuccessPercent").asDouble() < 50),
                () -> assertTrue(s.at("/time/idlePercent").asDouble() > 50),
                () -> assertTrue(s.at("/time/longestIdleSeconds").asLong() >= 300),
                () -> assertTrue(s.at("/editing/charactersPasted").asInt() > s.at("/editing/charactersTyped").asInt()));
    }

    @Test
    void scenariosAreDescribed() {
        for (DemoScenario scenario : DemoScenario.values()) {
            assertFalse(scenario.description().isBlank());
            assertFalse(scenario.projectName().isBlank());
            assertTrue(scenario.startTime().getHour() >= 8);
        }
        assertNotEquals(DemoScenario.FULLY_CORRECT.studentId(), DemoScenario.PARTIALLY_CORRECT.studentId());
    }

    @Test
    void runAllWritesTwoFilesPerScenario() throws IOException {
        assertEquals(2 * DemoScenario.values().length, new DemoRunner().runAll(workspace).size());
    }

    @Test
    void simulatedClockOnlyMovesForward() {
        Instant start = Instant.parse("2026-11-09T18:00:00Z");
        SimulatedClock clock = new SimulatedClock(start, ZoneId.of("UTC"));
        clock.advance(Duration.ofMinutes(5));
        assertEquals(start.plusSeconds(300), clock.instant());
        assertThrows(IllegalArgumentException.class, () -> clock.advance(Duration.ofSeconds(-1)));
        assertEquals(ZoneId.of("UTC"), clock.getZone());
        assertEquals(ZoneId.of("America/Port_of_Spain"), clock.withZone(ZoneId.of("America/Port_of_Spain")).getZone());
        assertEquals(clock.instant(), clock.withZone(ZoneId.of("Europe/London")).instant());
    }
}
