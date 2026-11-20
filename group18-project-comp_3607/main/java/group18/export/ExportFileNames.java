package group18.export;

import group18.model.SessionMetadata;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Names report files after their session: the project name followed by the session's start time, for example
 * {@code Calculator_2026-11-09_14-00-00.json}. The JSON and PDF of one session share the same base name, and a new
 * session always gets new files, so earlier reports are never overwritten.
 */
public final class ExportFileNames {

    /** Format of the time stamp in file names. Contains no characters that are invalid in Windows file names. */
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    /** Longest project-name part kept in a file name. */
    private static final int MAX_NAME_LENGTH = 60;

    /** Time zone used for the time stamp. */
    private final ZoneId zone;

    /**
     * Creates a naming scheme.
     *
     * @param zone the time zone used for the time stamp in file names
     */
    public ExportFileNames(ZoneId zone) {
        this.zone = Objects.requireNonNull(zone, "zone");
    }

    /**
     * The shared part of the file names for a session.
     *
     * @param metadata the session metadata
     * @return the base name, for example {@code Calculator_2026-11-09_14-00-00}
     */
    public String baseName(SessionMetadata metadata) {
        return safe(metadata.projectName()) + "_" + STAMP.format(metadata.startTime().atZone(zone));
    }

    /**
     * The JSON file name for a session.
     *
     * @param metadata the session metadata
     * @return the file name ending in {@code .json}
     */
    public String json(SessionMetadata metadata) {
        return baseName(metadata) + ".json";
    }

    /**
     * The PDF file name for a session.
     *
     * @param metadata the session metadata
     * @return the file name ending in {@code .pdf}
     */
    public String pdf(SessionMetadata metadata) {
        return baseName(metadata) + ".pdf";
    }

    /**
     * Makes a project name safe to use in a file name on every operating system.
     *
     * @param name the project name; may be {@code null}
     * @return the name with unsafe characters replaced by underscores, or {@code project} if nothing is left
     */
    public static String safe(String name) {
        String cleaned = name == null ? "" : name.strip().replaceAll("[^A-Za-z0-9._-]+", "_")
                .replaceAll("^[._]+|[._]+$", "");
        if (cleaned.length() > MAX_NAME_LENGTH) {
            cleaned = cleaned.substring(0, MAX_NAME_LENGTH);
        }
        return cleaned.isEmpty() ? "project" : cleaned;
    }
}
