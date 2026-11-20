package group18.listener;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds the methods and constructors declared in a Java source file, without compiling it.
 *
 * <p>Students' code is often half-typed and does not compile, so a compiler cannot be used. Instead the scanner
 * removes comments and string literals, follows the curly braces to know whether it is directly inside a class
 * body, and matches each declaration header against regular expressions. Methods of nested types are prefixed
 * with the nested type's name, for example {@code Node.getValue()}.</p>
 *
 * <p>Signatures contain the method name and the parameter <em>types</em> only, so renaming a parameter is not
 * reported as removing one method and adding another.</p>
 */
public final class MethodScanner {

    /** Words that may come before a method's return type. */
    private static final Set<String> MODIFIER_WORDS = Set.of("public", "protected", "private", "static", "final",
            "abstract", "synchronized", "native", "default", "strictfp", "transient", "volatile");

    private static final String MODIFIERS =
            "(?:(?:public|protected|private|static|final|abstract|synchronized|native|default|strictfp)\\s+)*";

    /** Optional generic type parameters such as {@code <T extends Comparable<T>>}. */
    private static final String TYPE_PARAMETERS = "(?:<[^<>]*(?:<[^<>]*>[^<>]*)*>\\s*)?";

    private static final String THROWS = "(?:\\s*throws\\s+[\\w$.,\\s<>]+)?";

    private static final Pattern TYPE_DECLARATION =
            Pattern.compile("^(?:[\\w-]+\\s+)*?(?:class|interface|enum|record|@interface)\\s+([A-Za-z_$][\\w$]*)");

    private static final Pattern METHOD = Pattern.compile("^" + MODIFIERS + TYPE_PARAMETERS
            + "([\\w$.]+(?:\\s*<.*>)?(?:\\s*\\[\\s*\\])*)\\s+([A-Za-z_$][\\w$]*)\\s*\\(([^()]*)\\)"
            + "(?:\\s*\\[\\s*\\])*" + THROWS + "$");

    private static final Pattern CONSTRUCTOR = Pattern.compile("^" + MODIFIERS + TYPE_PARAMETERS
            + "([A-Za-z_$][\\w$]*)\\s*\\(([^()]*)\\)" + THROWS + "$");

    private static final Pattern ANNOTATION = Pattern.compile("@(?!interface\\b)[\\w$.]+(?:\\s*\\([^()]*\\))?");

    private static final Pattern PARAMETER =
            Pattern.compile("^(.*?)([A-Za-z_$][\\w$]*)\\s*((?:\\[\\s*\\])*)$");

    private MethodScanner() {
    }

    /** A brace-delimited region: either the body of a type or any other block. */
    private record Scope(boolean isType, String name) {
    }

    /**
     * Lists the methods and constructors declared in a source file.
     *
     * @param source Java source code; may be incomplete or {@code null}
     * @return signatures such as {@code add(int, int)} or {@code Calculator()}, sorted alphabetically
     */
    public static Set<String> scan(String source) {
        Set<String> found = new TreeSet<>();
        if (source == null || source.isBlank()) {
            return found;
        }
        String code = stripCommentsAndLiterals(source);
        Deque<Scope> scopes = new ArrayDeque<>();
        StringBuilder header = new StringBuilder();
        int parentheses = 0;
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if (c == '(') {
                parentheses++;
            } else if (c == ')') {
                parentheses = Math.max(0, parentheses - 1);
            }
            boolean structural = parentheses == 0 && (c == '{' || c == '}' || c == ';');
            if (!structural) {
                header.append(c);
                continue;
            }
            String text = normalise(header);
            header.setLength(0);
            Scope current = scopes.peek();
            boolean inTypeBody = current != null && current.isType();
            if (c == '{') {
                Matcher type = TYPE_DECLARATION.matcher(text);
                if (type.find()) {
                    scopes.push(new Scope(true, type.group(1)));
                } else {
                    if (inTypeBody) {
                        record(found, scopes, declaration(text, current.name(), true));
                    }
                    scopes.push(new Scope(false, null));
                }
            } else if (c == ';') {
                if (inTypeBody) {
                    record(found, scopes, declaration(text, current.name(), false));
                }
            } else if (!scopes.isEmpty()) {
                scopes.pop();
            }
        }
        return found;
    }

    /** Adds a signature with its nested-type prefix, unless it belongs to a local or anonymous class. */
    private static void record(Set<String> found, Deque<Scope> scopes, String signature) {
        if (signature == null) {
            return;
        }
        String prefix = prefix(scopes);
        if (prefix != null) {
            found.add(prefix + signature);
        }
    }

    /**
     * Turns a declaration header into a signature.
     *
     * @param header       the text before a brace or semicolon, normalised
     * @param typeName     the name of the enclosing type
     * @param hasBody      {@code true} if the header is followed by a brace, {@code false} for a semicolon
     * @return the signature, or {@code null} if the header does not declare a method or constructor
     */
    private static String declaration(String header, String typeName, boolean hasBody) {
        if (header.isEmpty() || header.contains("=")) {
            return null;
        }
        Matcher method = METHOD.matcher(header);
        if (method.matches() && !MODIFIER_WORDS.contains(method.group(1)) && !"new".equals(method.group(1))) {
            return method.group(2) + "(" + parameterTypes(method.group(3)) + ")";
        }
        if (hasBody) {
            Matcher constructor = CONSTRUCTOR.matcher(header);
            if (constructor.matches() && constructor.group(1).equals(typeName)) {
                return typeName + "(" + parameterTypes(constructor.group(2)) + ")";
            }
        }
        return null;
    }

    /**
     * Names of the enclosing nested types below the top-level type, for example {@code "Inner."}.
     *
     * @return the prefix, an empty string for the top-level type, or {@code null} inside a method body (local and
     *         anonymous classes are not part of the class's own structure)
     */
    private static String prefix(Deque<Scope> scopes) {
        List<String> names = new ArrayList<>();
        Iterator<Scope> outermostFirst = scopes.descendingIterator();
        while (outermostFirst.hasNext()) {
            Scope scope = outermostFirst.next();
            if (!scope.isType()) {
                return null;
            }
            names.add(scope.name());
        }
        if (names.size() <= 1) {
            return "";
        }
        return String.join(".", names.subList(1, names.size())) + ".";
    }

    /**
     * Reduces a parameter list to its types: {@code "final int a, String... rest"} becomes {@code "int, String..."}.
     *
     * @param parameters the text between the parentheses
     * @return the comma-separated parameter types
     */
    static String parameterTypes(String parameters) {
        if (parameters.isBlank()) {
            return "";
        }
        List<String> types = new ArrayList<>();
        for (String parameter : splitTopLevel(parameters)) {
            String text = parameter.strip().replaceAll("\\bfinal\\s+", "");
            Matcher m = PARAMETER.matcher(text);
            String type = m.matches() && !m.group(1).isBlank() ? m.group(1) + m.group(3) : text;
            types.add(type.replaceAll("\\s+", " ").strip()
                    .replaceAll("\\s*([<>\\[\\],])\\s*", "$1")
                    .replaceAll("\\s*\\.\\.\\.", "...")
                    .replace(",", ", "));
        }
        return String.join(", ", types);
    }

    /** Splits on commas that are not inside generic angle brackets. */
    private static List<String> splitTopLevel(String text) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth = Math.max(0, depth - 1);
            } else if (c == ',' && depth == 0) {
                parts.add(text.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(text.substring(start));
        return parts;
    }

    private static String normalise(CharSequence header) {
        String text = ANNOTATION.matcher(header).replaceAll(" ");
        return text.replaceAll("\\s+", " ").strip();
    }

    /**
     * Removes comments and empties string, character and text-block literals, so braces or semicolons inside them
     * are not mistaken for code structure.
     *
     * @param source Java source code
     * @return the code with comments removed and literals emptied
     */
    static String stripCommentsAndLiterals(String source) {
        StringBuilder out = new StringBuilder(source.length());
        int i = 0;
        int n = source.length();
        while (i < n) {
            char c = source.charAt(i);
            char next = i + 1 < n ? source.charAt(i + 1) : '\0';
            if (c == '/' && next == '/') {
                while (i < n && source.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == '/' && next == '*') {
                int end = source.indexOf("*/", i + 2);
                i = end < 0 ? n : end + 2;
                out.append(' ');
            } else if (source.startsWith("\"\"\"", i)) {
                int end = source.indexOf("\"\"\"", i + 3);
                i = end < 0 ? n : end + 3;
                out.append("\"\"");
            } else if (c == '"' || c == '\'') {
                i = skipLiteral(source, i, c);
                out.append(c).append(c);
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    /** Returns the index just after a string or character literal that starts at {@code start}. */
    private static int skipLiteral(String source, int start, char quote) {
        int i = start + 1;
        while (i < source.length() && source.charAt(i) != quote && source.charAt(i) != '\n') {
            i += source.charAt(i) == '\\' ? 2 : 1;
        }
        return Math.min(i + 1, source.length());
    }
}
