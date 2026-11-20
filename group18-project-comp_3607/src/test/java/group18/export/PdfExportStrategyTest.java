package group18.export;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import group18.model.LifecycleEvent.Action;
import group18.model.SessionReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("The PDF exporter writes a readable report with every required section")
class PdfExportStrategyTest {

    private static final ZoneId ZONE = ZoneId.of("America/Port_of_Spain");

    @TempDir
    Path folder;

    private final PdfExportStrategy strategy = new PdfExportStrategy(new ExportFileNames(ZONE), ZONE);

    private Path export(SessionReport report) throws IOException {
        List<Path> written = strategy.export(report, folder);
        assertEquals(1, written.size());
        return written.get(0);
    }

    private static String text(Path pdf) throws IOException {
        PdfReader reader = new PdfReader(Files.readAllBytes(pdf));
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(extractor.getTextFromPage(page)).append('\n');
            }
            return text.toString();
        } finally {
            reader.close();
        }
    }

    @Test
    void writesAPdfFileNamedForTheSession() throws IOException {
        Path pdf = export(SampleReports.typical("816000123"));
        assertEquals("Shapes_2026-11-09_14-00-00.pdf", pdf.getFileName().toString());
        byte[] bytes = Files.readAllBytes(pdf);
        assertEquals("%PDF-", new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII));
    }

    @Test
    void containsTheSummariesTheHandoutAsksFor() throws IOException {
        String text = text(export(SampleReports.typical("816000123")));
        for (String expected : List.of(
                "BlueJ Telemetry Session Report", "Project: Shapes", "816000123", "Session session-1",
                "Session overview", "Active coding time vs idle time", "Activity across the session",
                "Editor interactions", "Typing cadence",
                "Compilation", "Frequency of compilation errors", "Cannot find symbol", "compiler.err.cant.resolve",
                "Compilation attempts", "Shape.java line 7",
                "Program runs", "java.lang.ArithmeticException", "Circle.scale",
                "Progression timeline", "Class created", "Method added", "scale(double)",
                "America/Port_of_Spain", "Page 1")) {
            assertTrue(text.contains(expected), "PDF should contain: " + expected);
        }
        assertFalse(text.contains("Student ID not set"));
    }

    @Test
    void warnsWhenTheStudentIdWasNotSet() throws IOException {
        String text = text(export(SampleReports.typical(null)));
        assertTrue(text.contains("STUDENT_ID_PLACEHOLDER"));
        assertTrue(text.contains("Student ID not set"));
    }

    @Test
    void emptySessionExplainsThatNothingHappened() throws IOException {
        String text = text(export(SampleReports.empty()));
        assertTrue(text.contains("No compilations were requested in this session."));
        assertTrue(text.contains("No methods or constructors were run from BlueJ in this session."));
        assertTrue(text.contains("No project, class or method milestones were recorded."));
    }

    @Test
    void longListsAreShortenedWithANote() throws IOException {
        int attempts = PdfReportBuilder.MAX_LIST_ROWS + 5;
        String text = text(export(SampleReports.manyCompilations(attempts)));
        assertTrue(text.contains("Showing the first " + PdfReportBuilder.MAX_LIST_ROWS + " of " + attempts
                + " compilation attempts"));
    }

    @Test
    void replacesTheReportOfTheSameSession() throws IOException {
        Path first = export(SampleReports.empty());
        Path second = export(SampleReports.typical(null));
        assertEquals(first, second);
        assertTrue(text(second).contains("Circle.scale"));
    }

    @Test
    void everyLifecycleActionHasAReadableLabel() {
        for (Action action : Action.values()) {
            String label = PdfReportBuilder.actionLabel(action);
            assertFalse(label.isBlank());
            assertFalse(label.contains("_"));
        }
    }
}
