package group18.session;

import group18.export.ExportFileNames;
import group18.model.SessionInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Decides where a session's reports are written: a {@code telemetry} folder inside the BlueJ project folder, so
 * the JSON and PDF stay with the project they describe. BlueJ only shows folders that contain a
 * {@code package.bluej} file, so this folder does not appear in the class diagram.
 *
 * <p>If the project folder no longer exists or cannot be written to (for example the project was deleted, or
 * opened from a read-only location), the reports go to a per-project folder under a fallback location instead.</p>
 */
public final class OutputDirectories {

    /** Name of the report folder created inside each project. */
    public static final String FOLDER_NAME = "telemetry";

    /** Folder used when the project folder cannot be written to. */
    private final Path fallbackRoot;

    /**
     * Creates the policy.
     *
     * @param fallbackRoot folder used when a project folder cannot be written to
     */
    public OutputDirectories(Path fallbackRoot) {
        this.fallbackRoot = Objects.requireNonNull(fallbackRoot, "fallbackRoot");
    }

    /**
     * The folder for a session's reports.
     *
     * @param info the session
     * @return {@code <project>/telemetry}, or {@code <fallback>/<project name>} if the project folder cannot be
     *         written to
     */
    public Path forSession(SessionInfo info) {
        Path projectDirectory = info.project().directory();
        if (Files.isDirectory(projectDirectory) && Files.isWritable(projectDirectory)) {
            return projectDirectory.resolve(FOLDER_NAME);
        }
        return fallbackRoot.resolve(ExportFileNames.safe(info.project().name()));
    }
}
