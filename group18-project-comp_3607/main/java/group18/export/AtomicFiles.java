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
        /**
         * Writes the content.
         *
         * @param out the stream to write to
         * @throws IOException if writing fails
         */
        void writeTo(OutputStream out) throws IOException;
    }

    private AtomicFiles() {
    }

    /**
     * Writes a file atomically, replacing any existing file.
     *
     * @param target  the file to write
     * @param content writes the bytes of the file
     * @throws IOException if the file cannot be written; the target is then left unchanged
     */
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
