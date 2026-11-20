package group18.export;

import group18.model.SessionReport;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * A way of writing a session report to disk: the <b>Strategy</b> pattern.
 *
 * <p>{@link JsonExportStrategy} and {@link PdfExportStrategy} are interchangeable implementations, and
 * {@link CompositeExportStrategy} combines several of them. Code that triggers an export only depends on this
 * interface, so a new format (CSV, HTML, ...) is added by writing one new class, without changing any existing
 * code.</p>
 */
public interface ExportStrategy {

    /**
     * Writes the report into a folder.
     *
     * @param report          the report to write
     * @param outputDirectory the folder to write into; created if it does not exist
     * @return the files that were written
     * @throws IOException if a file cannot be written
     */
    List<Path> export(SessionReport report, Path outputDirectory) throws IOException;
}
