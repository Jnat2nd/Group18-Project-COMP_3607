package group18.export;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/** Formats durations, percentages and times for the PDF report. */
final class ReportFormat {

    /** Time of day, for tables. */
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ENGLISH);

    /** Date and time, for the report header. */
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm:ss", Locale.ENGLISH);

    /** Short time of day, for chart axes. */
    private static final DateTimeFormatter SHORT_TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

    /** The time zone the report is shown in. */
    private final ZoneId zone;

    /**
     * Creates a formatter for a time zone.
     *
     * @param zone the time zone the report is shown in
     */
    ReportFormat(ZoneId zone) {
        this.zone = Objects.requireNonNull(zone, "zone");
    }

    /**
     * The time zone's identifier.
     *
     * @return for example {@code America/Port_of_Spain}
     */
    String zoneName() {
        return zone.getId();
    }

    /**
     * Formats a time of day.
     *
     * @param instant the time
     * @return for example {@code 14:05:09}
     */
    String time(Instant instant) {
        return TIME.format(instant.atZone(zone));
    }

    /**
     * Formats a time of day without seconds.
     *
     * @param instant the time
     * @return for example {@code 14:05}
     */
    String shortTime(Instant instant) {
        return SHORT_TIME.format(instant.atZone(zone));
    }

    /**
     * Formats a date and time.
     *
     * @param instant the time
     * @return for example {@code Mon 9 Nov 2026, 14:05:09}
     */
    String dateTime(Instant instant) {
        return DATE_TIME.format(instant.atZone(zone));
    }

    /**
     * A duration such as {@code 45s}, {@code 4m 05s} or {@code 1h 02m 03s}.
     *
     * @param seconds the duration in seconds
     * @return the formatted duration
     */
    static String duration(long seconds) {
        long s = Math.max(0, seconds);
        if (s < 60) {
            return s + "s";
        }
        if (s < 3600) {
            return String.format(Locale.ROOT, "%dm %02ds", s / 60, s % 60);
        }
        return String.format(Locale.ROOT, "%dh %02dm %02ds", s / 3600, (s % 3600) / 60, s % 60);
    }

    /**
     * Formats a percentage.
     *
     * @param value the percentage, 0 to 100
     * @return for example {@code 73.2%}
     */
    static String percent(double value) {
        return String.format(Locale.ROOT, "%.1f%%", value);
    }

    /**
     * Formats a number with one decimal place.
     *
     * @param value the number
     * @return for example {@code 12.5}
     */
    static String decimal(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    /**
     * Makes text safe and short enough for a table cell.
     *
     * @param text     any text; may be {@code null}
     * @param maxChars the longest text to keep before shortening with "..."
     * @return the cleaned text
     */
    static String clean(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        String single = text.replaceAll("[\\t\\x0B\\f\\r]+", " ").replaceAll("\\n\\s*", " / ").strip();
        return single.length() > maxChars ? single.substring(0, maxChars) + "..." : single;
    }
}
