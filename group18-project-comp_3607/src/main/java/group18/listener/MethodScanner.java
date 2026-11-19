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

public final class MethodScanner {

    private static final Set<String> MODIFIER_WORDS = Set.of("public", "protected", "private", "static", "final", "abstract", "synchronized", "native", "default", "strictfp", "transient", "volatile");

    private static final String MODIFIERS = "(?:(?:public|protected|private|static|final|abstract|synchronized|native|default|strictfp)\\s+)*";

    private static final String TYPE_PARAMETERS = "(?:<[^<>]*(?:<[^<>]*>[^<>]*)*>\\s*)?";

    private static final String THROWS = "(?:\\s*throws\\s+[\\w$.,\\s<>]+)?";

    private static final Pattern TYPE_DECLARATION = Pattern.compile("^(?:[\\w-]+\\s+)*?(?:class|interface|enum|record|@interface)\\s+([A-Za-z_$][\\w$]*)");

    private static final Pattern METHOD = Pattern.compile("^" + MODIFIERS + TYPE_PARAMETERS+ "([\\w$.]+(?:\\s*<.*>)?(?:\\s*\\[\\s*\\])*)\\s+([A-Za-z_$][\\w$]*)\\s*\\(([^()]*)\\)"+ "(?:\\s*\\[\\s*\\])*" + THROWS + "$");

    private static final Pattern CONSTRUCTOR = Pattern.compile("^" + MODIFIERS + TYPE_PARAMETERS  + "([A-Za-z_$][\\w$]*)\\s*\\(([^()]*)\\)" + THROWS + "$");

    private static final Pattern ANNOTATION = Pattern.compile("@(?!interface\\b)[\\w$.]+(?:\\s*\\([^()]*\\))?");

    private static final Pattern PARAMETER = Pattern.compile("^(.*?)([A-Za-z_$][\\w$]*)\\s*((?:\\[\\s*\\])*)$");

    private MethodScanner() {
    }

    private record Scope(boolean isType, String name) {
    }

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

    
    private static void record(Set<String> found, Deque<Scope> scopes, String signature) {
        if (signature == null) {
            return;
        }
        String prefix = prefix(scopes);
        if (prefix != null) {
            found.add(prefix + signature);
        }
    }

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
    static String parameterTypes(String parameters) {
        if (parameters.isBlank()) {
            return "";
        }
        List<String> types = new ArrayList<>();
        for (String parameter : splitTopLevel(parameters)) {
            String text = parameter.strip().replaceAll("\\bfinal\\s+", "");
            Matcher m = PARAMETER.matcher(text);
            String type = m.matches() && !m.group(1).isBlank() ? m.group(1) + m.group(3) : text;
            types.add(type.replaceAll("\\s+", " ").strip().replaceAll("\\s*([<>\\[\\],])\\s*", "$1") .replaceAll("\\s*\\.\\.\\.", "...") .replace(",", ", "));
        }
        return String.join(", ", types);
    }
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

    private static int skipLiteral(String source, int start, char quote) {
        int i = start + 1;
        while (i < source.length() && source.charAt(i) != quote && source.charAt(i) != '\n') {
            i += source.charAt(i) == '\\' ? 2 : 1;
        }
        return Math.min(i + 1, source.length());
    }
}
