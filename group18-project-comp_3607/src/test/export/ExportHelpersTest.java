package group18.export;

import group18.model.ExportReason;
import group18.model.SessionMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("File naming, atomic writing and report formatting")
class ExportHelpersTest {

    private static final Instant START = Instant.parse("2026-11-09T18:00:05Z");

    private static SessionMetadata metadata(String projectName) {
        return new SessionMetadata("1.0", "g", "s", "id", projectName, START, START, 0, 0, 0,
                ExportReason.MANUAL_EXPORT);
    }

    @Test
    void filesAreNamedAfterTheProjectAndTheSessionStartInLocalTime() {
        ExportFileNames names = new ExportFileNames(ZoneId.of("America/Port_of_Spain"));
        assertEquals("Calculator_2026-11-09_14-00-05", names.baseName(metadata("Calculator")));
        assertEquals("Calculator_2026-11-09_14-00-05.json", names.json(metadata("Calculator")));
        assertEquals("Calculator_2026-11-09_14-00-05.pdf", names.pdf(metadata("Calculator")));
        assertEquals("Calculator_2026-11-09_18-00-05.json",
                new ExportFileNames(ZoneId.of("UTC")).json(metadata("Calculator")));
    }

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @CsvSource({
            "My Project, My_Project",
            "lab 3/part:a, lab_3_part_a",
            "..hidden.., hidden",
            "'Bank Account (v2)', Bank_Account_v2",
            "Triángulo, Tri_ngulo"
    })
    void projectNamesAreMadeSafeForFileNames(String name, String expected) {
        assertEquals(expected, ExportFileNames.safe(name));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "///", "..."})
    void unusableNamesBecomeProject(String name) {
        assertEquals("project", ExportFileNames.safe(name));
    }

    @Test
    void veryLongNamesAreShortened() {
        assertEquals(60, ExportFileNames.safe("x".repeat(200)).length());
    }

    @Test
    void atomicWriteReplacesTheTargetAndLeavesNoTemporaryFile(@TempDir Path folder) throws IOException {
        Path target = folder.resolve("report.json");
        AtomicFiles.write(target, out -> out.write("one".getBytes(StandardCharsets.UTF_8)));
        AtomicFiles.write(target, out -> out.write("two".getBytes(StandardCharsets.UTF_8)));
        assertEquals("two", Files.readString(target));
        try (Stream<Path> files = Files.list(folder)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void failedWriteKeepsTheOldFile(@TempDir Path folder) throws IOException {
        Path target = folder.resolve("report.json");
        Files.writeString(target, "old");
        assertThrows(IOException.class, () -> AtomicFiles.write(target, out -> {
            out.write("half".getBytes(StandardCharsets.UTF_8));
            throw new IOException("disk full");
        }));
        assertEquals("old", Files.readString(target));
        try (Stream<Path> files = Files.list(folder)) {
            assertEquals(1, files.count());
        }
    }

    @ParameterizedTest(name = "{0}s -> {1}")
    @CsvSource({"0, 0s", "45, 45s", "60, 1m 00s", "245, 4m 05s", "3600, 1h 00m 00s", "3723, 1h 02m 03s",
            "-5, 0s"})
    void durationsAreReadable(long seconds, String expected) {
        assertEquals(expected, ReportFormat.duration(seconds));
    }

    @Test
    void numbersAndTimesAreFormatted() {
        ReportFormat format = new ReportFormat(ZoneId.of("America/Port_of_Spain"));
        assertEquals("73.2%", ReportFormat.percent(73.24));
        assertEquals("12.5", ReportFormat.decimal(12.46));
        assertEquals("14:00:05", format.time(START));
        assertEquals("14:00", format.shortTime(START));
        assertEquals("Mon 9 Nov 2026, 14:00:05", format.dateTime(START));
        assertEquals("America/Port_of_Spain", format.zoneName());
    }

    @Test
    void cellTextIsCleanedAndShortened() {
        assertEquals("", ReportFormat.clean(null, 10));
        assertEquals("a b / c", ReportFormat.clean("a\tb\n   c", 20));
        assertEquals("abc...", ReportFormat.clean("abcdef", 3));
        assertTrue(ReportFormat.clean("x\r\ny", 10).contains("/"));
    }
}
