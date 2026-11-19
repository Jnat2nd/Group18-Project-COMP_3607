package group18.export;

import java.io.IOException;
import java.nio.file.path;
import java.util.ArrayList;
import java.util.List;

public final class CompositeExport extends ExportStrategy{
 private final List<ExportStrategy> exporters = new ArrayList<>();

    public CompositeExportStrategy addExporter(ExportStrategy exporter) {
        exporters.add(exporter);
        return this;
    }

    public int size() {
        return exporters.size();
    }

    @Override
    public List<Path> export(SessionReport report, Path outputDirectory) throws IOException {
        List<Path> written = new ArrayList<>();
        IOException failure = null;
        for (ExportStrategy exporter : exporters) {
            try {
                written.addAll(exporter.export(report, outputDirectory));
            } catch (IOException | RuntimeException e) {
                if (failure == null) {
                    failure = new IOException("One or more exports has failed", e);
                } else {failure.addSuppressed(e);}
            }
        }
        if (failure != null) {
            throw failure;
        }
        return written;
    }

}

