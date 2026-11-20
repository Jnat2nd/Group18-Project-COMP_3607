package group18.model;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Identifies a BlueJ project without depending on the BlueJ API.
 *
 * @param key       a stable identifier for the project: its normalised absolute folder path
 * @param name      the project name shown by BlueJ
 * @param directory the project folder
 */
public record ProjectRef(String key, String name, Path directory) {

    /**
     * Validates the components.
     *
     * @throws NullPointerException if any component is null
     */
    public ProjectRef {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(directory, "directory");
    }

    /**
     * Creates a reference whose key is the normalised absolute path of the folder.
     *
     * @param name      the project name
     * @param directory the project folder
     * @return the reference
     */
    public static ProjectRef of(String name, Path directory) {
        Path normalised = directory.toAbsolutePath().normalize();
        return new ProjectRef(keyOf(normalised), name, normalised);
    }

    /**
     * The key used for a project folder, so callers can look a project up from a folder path.
     *
     * @param directory the project folder
     * @return the normalised absolute path as text
     */
    public static String keyOf(Path directory) {
        return directory.toAbsolutePath().normalize().toString();
    }

    /**
     * Whether a file lies inside this project's folder.
     *
     * @param file any file path; relative paths are resolved against the working directory
     * @return {@code true} if the file is inside the project folder
     */
    public boolean contains(Path file) {
        return file != null && file.toAbsolutePath().normalize().startsWith(directory);
    }
}
