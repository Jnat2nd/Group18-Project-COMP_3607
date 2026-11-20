package group18.listener;

// Sources:
//  https://www.bluej.org/doc/extensionsAPI2/bluej/extensions2/event/CompileEvent.html (BlueJ gives the message only)
//  javac's compiler.properties for the diagnostic keys

import java.util.List;
import java.util.regex.Pattern;

/**
 * Recovers javac's diagnostic code (for example {@code compiler.err.cant.resolve}) from a compiler message.
 *
 * <p>BlueJ reports the text of each compiler error but not its code. Grouping errors by code lets the report count
 * how often each kind of mistake happens, independent of variable names in the message.</p>
 */
public final class ErrorCodes {

    /** Code used when no rule matches the message. */
    public static final String UNCLASSIFIED = "UNCLASSIFIED";

    /** Label used for {@link #UNCLASSIFIED}. */
    private static final String UNCLASSIFIED_LABEL = "Other error";

    /**
     * One classification rule.
     *
     * @param pattern matches the message
     * @param code    the javac diagnostic key
     * @param label   a short readable name for reports
     */
    private record Rule(Pattern pattern, String code, String label) {
    }

    /** Rules in priority order; the first match wins. */
    private static final List<Rule> RULES = List.of(
            rule("'.{1,12}?' expected|<identifier> expected", "compiler.err.expected",
                    "Missing symbol (e.g. ';' or ')')"),
            rule("class, interface, enum,? or record expected|class, interface, or enum expected",
                    "compiler.err.expected4", "Code outside a class body"),
            rule("cannot find symbol", "compiler.err.cant.resolve", "Cannot find symbol"),
            rule("incompatible types|unexpected return value", "compiler.err.prob.found.req",
                    "Incompatible types"),
            rule("missing return statement", "compiler.err.missing.ret.stmt", "Missing return statement"),
            rule("reached end of file while parsing", "compiler.err.premature.eof",
                    "Unbalanced braces (end of file)"),
            rule("unreachable statement", "compiler.err.unreachable.stmt", "Unreachable statement"),
            rule("might not have been initiali[sz]ed", "compiler.err.var.might.not.have.been.initialized",
                    "Variable not initialised"),
            rule("might already have been assigned", "compiler.err.var.might.already.be.assigned",
                    "Final variable assigned twice"),
            rule("is already defined in", "compiler.err.already.defined", "Duplicate definition"),
            rule("non-static .* cannot be referenced from a static context", "compiler.err.non-static.cant.be.ref",
                    "Non-static member used from static context"),
            rule("cannot be applied to", "compiler.err.cant.apply.symbol", "Wrong arguments in call"),
            rule("no suitable (method|constructor) found", "compiler.err.cant.apply.symbols",
                    "No matching method or constructor"),
            rule("illegal start of expression", "compiler.err.illegal.start.of.expr", "Illegal start of expression"),
            rule("illegal start of type", "compiler.err.illegal.start.of.type", "Illegal start of type"),
            rule("not a statement", "compiler.err.not.stmt", "Not a statement"),
            rule("unclosed string literal", "compiler.err.unclosed.str.lit", "Unclosed string literal"),
            rule("unclosed character literal", "compiler.err.unclosed.char.lit", "Unclosed character literal"),
            rule("empty character literal", "compiler.err.empty.char.lit", "Empty character literal"),
            rule("unclosed comment", "compiler.err.unclosed.comment", "Unclosed comment"),
            rule("unreported exception", "compiler.err.unreported.exception.need.to.catch.or.throw",
                    "Checked exception not handled"),
            rule("is public, should be declared in a file named", "compiler.err.class.public.should.be.in.file",
                    "Class name does not match file name"),
            rule("has private access", "compiler.err.report.access", "Private member accessed"),
            rule("bad operand types? for binary operator", "compiler.err.operator.cant.be.applied.1",
                    "Bad operand types for operator"),
            rule("bad operand type .* for unary operator", "compiler.err.operator.cant.be.applied",
                    "Bad operand type for operator"),
            rule("cannot be dereferenced", "compiler.err.cant.deref", "Primitive value dereferenced"),
            rule("array required, but .* found", "compiler.err.array.req.but.found", "Array required"),
            rule("cannot assign a value to final variable", "compiler.err.cant.assign.val.to.var",
                    "Final variable reassigned"),
            rule("is abstract; cannot be instantiated", "compiler.err.abstract.cant.be.instantiated",
                    "Abstract class instantiated"),
            rule("does not override abstract method", "compiler.err.does.not.override.abstract",
                    "Abstract method not implemented"),
            rule("invalid method declaration; return type required",
                    "compiler.err.invalid.meth.decl.ret.type.req", "Missing return type"),
            rule("missing method body, or declare abstract", "compiler.err.missing.meth.body.or.decl.abstract",
                    "Missing method body"),
            rule("'else' without 'if'", "compiler.err.else.without.if", "'else' without 'if'"),
            rule("break outside (switch or loop|of loop or switch)", "compiler.err.break.outside.switch.loop",
                    "break outside a loop or switch"),
            rule("illegal character", "compiler.err.illegal.char", "Illegal character"),
            rule("variable declaration not allowed here", "compiler.err.variable.not.allowed",
                    "Declaration not allowed here"),
            rule("duplicate class", "compiler.err.duplicate.class", "Duplicate class"));

    private ErrorCodes() {
    }

    private static Rule rule(String regex, String code, String label) {
        return new Rule(Pattern.compile(regex, Pattern.CASE_INSENSITIVE), code, label);
    }

    /**
     * Classifies a compiler message.
     *
     * @param message the message reported by BlueJ; may be {@code null}
     * @return the javac diagnostic key, or {@link #UNCLASSIFIED} if no rule matches
     */
    public static String code(String message) {
        if (message != null) {
            for (Rule rule : RULES) {
                if (rule.pattern().matcher(message).find()) {
                    return rule.code();
                }
            }
        }
        return UNCLASSIFIED;
    }

    /**
     * A short readable name for a diagnostic code, used in the PDF report.
     *
     * @param code a code returned by {@link #code(String)}
     * @return the label, or "Other error" for unknown codes
     */
    public static String label(String code) {
        for (Rule rule : RULES) {
            if (rule.code().equals(code)) {
                return rule.label();
            }
        }
        return UNCLASSIFIED_LABEL;
    }
}
