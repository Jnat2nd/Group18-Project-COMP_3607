package group18.listener;

// Source: https://www.javacodegeeks.com/getting-started-with-java-diff-utils.html

public final class TextDiff {

    private TextDiff() {
    }

    //Characters inserted or removed between two texts: the part left after dropping the shared start and end. //
    public static int changedCharacters(String before, String after) {
        String a = before == null ? "" : before;
        String b = after == null ? "" : after;
        if (a.equals(b)) {
            return 0;
        }

        int max = Math.min(a.length(), b.length());
        int prefix = 0;
        while (prefix < max && a.charAt(prefix) == b.charAt(prefix)) {
            prefix++;
        }
        int suffix = 0;
        while (suffix < max - prefix && a.charAt(a.length() - 1 - suffix) == b.charAt(b.length() - 1 - suffix)) {
            suffix++;
        }
        int removed = a.length() - prefix - suffix;
        int inserted = b.length() - prefix - suffix;
        return Math.max(1, Math.max(removed, inserted));
    }
}
