package group18.export;

import group18.model.SessionMetadata;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public final class ExportFileNames {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private static final int MAX_NAME_LENGTH = 60;

    private final ZoneId zone;

    public ExportFileNames(ZoneId zone) {
        this.zone = Objects.requireNonNull(zone, "zone");
    }

    public String baseName(SessionMetadata metadata) {
        return safe(metadata.projectName()) + "_" + STAMP.format(metadata.startTime().atZone(zone));
    }

    public String json(SessionMetadata metadata) {
        return baseName(metadata) + ".json";
    }

    public String pdf(SessionMetadata metadata) {
        return baseName(metadata) + ".pdf";
    }

    public static String safe(String name) {
        String cleaned = name == null ? "" : name.strip().replaceAll("[^A-Za-z0-9._-]+", "_")
                .replaceAll("^[._]+|[._]+$", "");
        if (cleaned.length() > MAX_NAME_LENGTH) {
            cleaned = cleaned.substring(0, MAX_NAME_LENGTH);
        }
        return cleaned.isEmpty() ? "project" : cleaned;
    }
}