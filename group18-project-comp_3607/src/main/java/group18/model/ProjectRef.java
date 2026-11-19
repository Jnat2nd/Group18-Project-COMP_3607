package group18.model;

import java.nio.file.Path;
import java.util.Objects;

public record ProjectRef(String key, String name, Path directory) {
    public ProjectRef {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(directory, "directory");
    }

    public static ProjectRef of(String name, Path directory) {
        Path normalised = directory.toAbsolutePath().normalize();
        return new ProjectRef(keyOf(normalised), name, normalised);
    }
    public static String keyOf(Path directory) {
        return directory.toAbsolutePath().normalize().toString();
    }
    public boolean contains(Path file) {
        return file != null && file.toAbsolutePath().normalize().startsWith(directory);
    }
}
