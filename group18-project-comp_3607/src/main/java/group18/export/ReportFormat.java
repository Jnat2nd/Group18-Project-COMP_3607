package group18.export;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

final class ReportFormat {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ENGLISH);

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm:ss", Locale.ENGLISH);

    private static final DateTimeFormatter SHORT_TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

    private final ZoneId zone;

    ReportFormat(ZoneId zone) {
        this.zone = Objects.requireNonNull(zone, "zone");
    }

    String zoneName() {
        return zone.getId();
    }

    String time(Instant instant) {
        return TIME.format(instant.atZone(zone));
    }

    String shortTime(Instant instant) {
        return SHORT_TIME.format(instant.atZone(zone));
    }

    String dateTime(Instant instant) {
        return DATE_TIME.format(instant.atZone(zone));
    }

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

    static String percent(double value) {
        return String.format(Locale.ROOT, "%.1f%%", value);
    }

    static String decimal(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    static String clean(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        String single = text.replaceAll("[\\t\\x0B\\f\\r]+", " ").replaceAll("\\n\\s*", " / ").strip();
        return single.length() > maxChars ? single.substring(0, maxChars) + "..." : single;
    }
}