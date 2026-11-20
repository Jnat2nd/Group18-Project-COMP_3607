package group18.listener;

// Idea adapted from: https://www.javacodegeeks.com/getting-started-with-java-diff-utils.html

/**
 * Measures how much text changed between two versions of a source file.
 *
 * <p>The common start and common end of both versions are skipped; whatever is left in the old version was removed
 * and whatever is left in the new version was inserted. This is exact for the single edit that typically happens
 * between two samples and a close estimate for several edits.</p>
 */
public final class TextDiff {

    private TextDiff() {
    }

    /**
     * Characters removed and inserted by a change.
     *
     * @param removed  characters that were in the old text but not in the new one
     * @param inserted characters that are in the new text but were not in the old one
     */
    public record Change(int removed, int inserted) {

        /** No change at all. */
        public static final Change NONE = new Change(0, 0);

        /**
         * Whether nothing changed.
         *
         * @return {@code true} if no character was removed or inserted
         */
        public boolean isEmpty() {
            return removed == 0 && inserted == 0;
        }
    }

    /**
     * Compares two versions of a text.
     *
     * @param before the old text; {@code null} is treated as empty
     * @param after  the new text; {@code null} is treated as empty
     * @return the characters removed and inserted
     */
    public static Change between(String before, String after) {
        String a = before == null ? "" : before;
        String b = after == null ? "" : after;
        if (a.equals(b)) {
            return Change.NONE;
        }
        int shorter = Math.min(a.length(), b.length());
        int prefix = 0;
        while (prefix < shorter && a.charAt(prefix) == b.charAt(prefix)) {
            prefix++;
        }
        int suffix = 0;
        while (suffix < shorter - prefix
                && a.charAt(a.length() - 1 - suffix) == b.charAt(b.length() - 1 - suffix)) {
            suffix++;
        }
        return new Change(a.length() - prefix - suffix, b.length() - prefix - suffix);
    }
}
