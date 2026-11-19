package group18;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Main generates the sample reports")
class MainTest {

    @Test
    void writesAndListsSixReportFiles(@TempDir Path folder) throws IOException {
        ByteArrayOutputStream text = new ByteArrayOutputStream();
        List<Path> files = Main.run(folder, new PrintStream(text, true, StandardCharsets.UTF_8));
        assertEquals(6, files.size());
        assertTrue(files.stream().allMatch(Files::isRegularFile));
        String printed = text.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("FULLY_CORRECT (Calculator)"));
        assertTrue(printed.contains("StudentGrades_2026-11-09_19-30-00.pdf"));
    }

    @Test
    void mainUsesTheFolderGivenOnTheCommandLine(@TempDir Path folder) throws IOException {
        PrintStream console = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
        try {
            Main.main(new String[]{folder.toString()});
        } finally {
            System.setOut(console);
        }
        assertTrue(Files.isRegularFile(folder.resolve("Calculator/telemetry/Calculator_2026-11-09_14-00-00.json")));
        assertEquals("telemetry-demo", Main.DEFAULT_OUTPUT);
    }
}
