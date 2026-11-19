package group18.export;

import group18.model.SessionReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CompositeExportStrategy runs several export strategies as one")
class CompositeExportStrategyTest {

    private final SessionReport report = SampleReports.empty();
    private final Path folder = Path.of("out");
    private final List<String> calls = new ArrayList<>();

    private ExportStrategy writes(String name) {
        return (r, dir) -> {
            calls.add(name);
            return List.of(dir.resolve(name));
        };
    }

    private ExportStrategy fails(String name, Exception failure) {
        return (r, dir) -> {
            calls.add(name);
            if (failure instanceof IOException io) {
                throw io;
            }
            throw (RuntimeException) failure;
        };
    }

    @Test
    void runsEveryChildInOrderAndReturnsAllFiles() throws IOException {
        CompositeExportStrategy composite = new CompositeExportStrategy().add(writes("a.json")).add(writes("b.pdf"));
        assertEquals(2, composite.size());
        assertEquals(List.of(folder.resolve("a.json"), folder.resolve("b.pdf")), composite.export(report, folder));
        assertEquals(List.of("a.json", "b.pdf"), calls);
    }

    @Test
    void compositesCanContainComposites() throws IOException {
        CompositeExportStrategy inner = new CompositeExportStrategy().add(writes("b")).add(writes("c"));
        CompositeExportStrategy outer = new CompositeExportStrategy().add(writes("a")).add(inner);
        assertEquals(3, outer.export(report, folder).size());
        assertEquals(List.of("a", "b", "c"), calls);
    }

    @Test
    void aFailingChildDoesNotStopTheOthersAndAllFailuresAreReported() {
        IOException disk = new IOException("disk full");
        IllegalStateException bug = new IllegalStateException("bug");
        CompositeExportStrategy composite = new CompositeExportStrategy()
                .add(fails("pdf", disk)).add(writes("json")).add(fails("csv", bug));

        IOException thrown = assertThrows(IOException.class, () -> composite.export(report, folder));
        assertEquals(List.of("pdf", "json", "csv"), calls);
        assertEquals(disk, thrown.getCause());
        assertEquals(1, thrown.getSuppressed().length);
        assertEquals(bug, thrown.getSuppressed()[0]);
    }

    @Test
    void emptyCompositeWritesNothing() throws IOException {
        assertTrue(new CompositeExportStrategy().export(report, folder).isEmpty());
    }
}
