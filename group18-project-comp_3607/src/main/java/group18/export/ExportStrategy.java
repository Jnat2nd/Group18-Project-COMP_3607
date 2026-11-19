package group18.export;

import group18.model.SessionReport;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface ExportStrategy {

    List<Path> export(SessionReport report, Path outputDirectory) throws IOException;
}