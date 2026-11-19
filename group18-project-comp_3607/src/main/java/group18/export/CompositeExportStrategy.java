package group18.export;

import group18.model.SessionReport;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CompositeExportStrategy implements ExportStrategy {

    private final List<ExportStrategy> children = new ArrayList<>();

    public CompositeExportStrategy() {
    }

    public CompositeExportStrategy add(ExportStrategy child) {
        children.add(child);
        return this;
    }

    public int size() {
        return children.size();
    }

    @Override
    public List<Path> export(SessionReport report, Path outputDirectory) throws IOException {
        List<Path> written = new ArrayList<>();
        IOException failure = null;
        for (ExportStrategy child : children) {
            try {
                written.addAll(child.export(report, outputDirectory));
            } catch (IOException | RuntimeException e) {
                if (failure == null) {
                    failure = new IOException("One or more report formats could not be written", e);
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
        return written;
    }
}