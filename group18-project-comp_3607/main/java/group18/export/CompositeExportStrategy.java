package group18.export;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import group18.model.SessionReport;


public final class CompositeExportStrategy implements ExportStrategy {

    /** The strategies run by this composite, in order. */
    private final List<ExportStrategy> children = new ArrayList<>();

    /** Creates an empty composite; add strategies with {@link #add(ExportStrategy)}. */
    public CompositeExportStrategy() {
    }

    /**
     * Adds a strategy to run.
     *
     * @param child the strategy; may itself be a composite
     * @return this composite, so calls can be chained
     */
    public CompositeExportStrategy add(ExportStrategy child) {
        children.add(child);
        return this;
    }

    /**
     * The number of strategies directly inside this composite.
     *
     * @return the child count
     */
    public int size() {
        return children.size();
    }

    /**
     * Runs every child strategy.
     *
     * @return every file written by the children
     * @throws IOException if any child failed; the other children still ran, and each failure is attached as a
     *                     suppressed exception
     */
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
