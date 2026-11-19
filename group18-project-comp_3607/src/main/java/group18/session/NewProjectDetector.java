package group18.session;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.stream.Stream;

public final class NewProjectDetector {

    public static final Duration DEFAULT_WINDOW = Duration.ofMinutes(2);

    private final Duration window;

    public NewProjectDetector() {
        this(DEFAULT_WINDOW);
    }

    public NewProjectDetector(Duration window) {
        this.window = Objects.requireNonNull(window, "window");
    }

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

    boolean isRecent(Instant created, Instant now) {
        return Duration.between(created, now).compareTo(window) <= 0;
    }
}
