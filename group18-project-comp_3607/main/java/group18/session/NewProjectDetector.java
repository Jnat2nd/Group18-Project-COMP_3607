package group18.session;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Decides whether a project that was just opened was also just created.
 *
 * <p>BlueJ has no "project created" event: choosing Project &gt; New Project creates a folder and then opens it.
 * A project therefore counts as newly created when its folder was created within the last two minutes, it holds
 * no {@code .java} files yet, and the plugin has never written reports into it. Opening an existing project, even
 * one unzipped a moment ago, normally fails the second or third test.</p>
 */
public final class NewProjectDetector {

    /** How recently the folder must have been created. */
    public static final Duration DEFAULT_WINDOW = Duration.ofMinutes(2);

    /** How recently the folder must have been created. */
    private final Duration window;

    /** Creates a detector with the default two-minute window. */
    public NewProjectDetector() {
        this(DEFAULT_WINDOW);
    }

    /**
     * Creates a detector.
     *
     * @param window how recently the folder must have been created
     */
    public NewProjectDetector(Duration window) {
        this.window = Objects.requireNonNull(window, "window");
    }

    /**
     * Checks a project folder.
     *
     * @param projectDirectory the project folder
     * @param openedAt         when the project was opened
     * @return {@code true} if the project looks newly created
     */
    public boolean isNewlyCreated(Path projectDirectory, Instant openedAt) {
        if (!Files.isDirectory(projectDirectory)
                || Files.exists(projectDirectory.resolve(OutputDirectories.FOLDER_NAME))) {
            return false;
        }
        try (Stream<Path> files = Files.list(projectDirectory)) {
            if (files.anyMatch(file -> file.getFileName().toString().endsWith(".java"))) {
                return false;
            }
            Instant created = Files.readAttributes(projectDirectory, BasicFileAttributes.class)
                    .creationTime().toInstant();
            return isRecent(created, openedAt);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Whether a folder created at {@code created} is recent at time {@code now}.
     *
     * @param created when the folder was created
     * @param now     the time to compare with
     * @return {@code true} if the folder is at most the window old (a creation time slightly in the future, caused
     *         by clock differences, also counts as recent)
     */
    boolean isRecent(Instant created, Instant now) {
        return Duration.between(created, now).compareTo(window) <= 0;
    }
}
