package group18.listener;

// Idea adapted from: https://www.javacodegeeks.com/getting-started-with-java-diff-utils.html

public final class TextDiff {

    private TextDiff() {
    }

    public record Change(int removed, int inserted) {
        public static final Change NONE = new Change(0, 0);
       
        public boolean isEmpty() {
            return removed == 0 && inserted == 0;
        }
    }


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
