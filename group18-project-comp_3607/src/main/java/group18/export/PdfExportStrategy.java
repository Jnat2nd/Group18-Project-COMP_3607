package group18.export;

// Source: https://stackoverflow.com/questions/25166533/java-write-to-pdf-with-color (OpenPDF tables and colours)

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfWriter;
import group18.model.SessionMetadata;
import group18.model.SessionReport;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

public final class PdfExportStrategy implements ExportStrategy {

    private final ExportFileNames names;

    private final ZoneId zone;

    public PdfExportStrategy(ExportFileNames names, ZoneId zone) {
        this.names = Objects.requireNonNull(names, "names");
        this.zone = Objects.requireNonNull(zone, "zone");
    }

    @Override
    public List<Path> export(SessionReport report, Path outputDirectory) throws IOException {
        Path target = outputDirectory.resolve(names.pdf(report.metadata()));
        AtomicFiles.write(target, out -> write(report, out));
        return List.of(target);
    }

    void write(SessionReport report, OutputStream out) {
        SessionMetadata metadata = report.metadata();
        Document document = new Document(PageSize.A4, 36, 36, 40, 54);
        PdfWriter writer = PdfWriter.getInstance(document, out);
        writer.setPageEvent(new PageFooter(metadata.projectName() + "  ·  " + metadata.studentId() + "  ·  session " + metadata.sessionId()));
        document.addTitle("BlueJ telemetry report: " + metadata.projectName());
        document.addSubject("Session " + metadata.sessionId());
        document.addAuthor(metadata.studentId());
        document.addCreator(metadata.generator());
        document.open();
        try {
            new PdfReportBuilder(report, new ReportFormat(zone), document, writer).build();
        } catch (RuntimeException e) {
            try {
                document.close();
            } catch (RuntimeException closing) {
                e.addSuppressed(closing);
            }
            throw e;
        }
        document.close();
    }
}