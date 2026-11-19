package group18.export;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

final class AtomicFiles {

    @FunctionalInterface
    interface Content {
        void writeTo(OutputStream out) throws IOException;
    }

    private AtomicFiles() {
    }

    static void write(Path target, Content content) throws IOException {
        Path folder = target.toAbsolutePath().getParent();
        Files.createDirectories(folder);
        Path temp = folder.resolve("." + target.getFileName() + "." + UUID.randomUUID() + ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(temp, StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE)) {
                content.writeTo(out);
            }
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}