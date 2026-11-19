package group18.session;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import group18.export.ExportFileNames;
import group18.model.SessionInfo;


public final class OutputDirectories {
    public static final String FOLDER_NAME = "telemetry";
    private final Path fallbackRoot;


    public OutputDirectories(Path fallbackRoot) {
        this.fallbackRoot = Objects.requireNonNull(fallbackRoot, "fallbackRoot");
    }


    public Path forSession(SessionInfo info) {
        Path projectDirectory = info.project().directory();
        if (Files.isDirectory(projectDirectory) && Files.isWritable(projectDirectory)) {
            return projectDirectory.resolve(FOLDER_NAME);
        }
        return fallbackRoot.resolve(ExportFileNames.safe(info.project().name()));
    }
}
