package group18.listener;
// Sources:
// https://www.bluej.org/doc/extensionsAPI2/bluej/extensions2/event/CompileEvent.html

import java.util.List;
import java.util.regex.Pattern;

// BlueJ gives only the error message text, so javac's diagnostic code is recovered from it. //
public final class ErrorCodes {

    public static final String UNCLASSIFIED = "UNCLASSIFIED";

    private record Rule(Pattern pattern, String code) {
    }

    private static final List<Rule> RULES = List.of(
            rule("'.{1,12}?' expected|<identifier> expected", "compiler.err.expected"),
            rule("class, interface, enum,? or record expected", "compiler.err.expected4"),
            rule("cannot find symbol", "compiler.err.cant.resolve"),
            rule("incompatible types|unexpected return value", "compiler.err.prob.found.req"),
            rule("missing return statement", "compiler.err.missing.ret.stmt"),
            rule("reached end of file while parsing", "compiler.err.premature.eof"),
            rule("unreachable statement", "compiler.err.unreachable.stmt"),
            rule("might not have been initiali[sz]ed", "compiler.err.var.might.not.have.been.initialized"),
            rule("is already defined in", "compiler.err.already.defined"),
            rule("non-static .* cannot be referenced from a static context", "compiler.err.non-static.cant.be.ref"),
            rule("cannot be applied to|no suitable (method|constructor) found", "compiler.err.cant.apply.symbol"),
            rule("illegal start of expression", "compiler.err.illegal.start.of.expr"),
            rule("illegal start of type", "compiler.err.illegal.start.of.type"),
            rule("not a statement", "compiler.err.not.stmt"),
            rule("unclosed string literal", "compiler.err.unclosed.str.lit"),
            rule("unreported exception", "compiler.err.unreported.exception.need.to.catch.or.throw"),
            rule("is public, should be declared in a file named", "compiler.err.class.public.should.be.in.file"),
            rule("possible lossy conversion", "compiler.err.possible.loss.of.precision"),
            rule("has private access", "compiler.err.report.access"));

    private ErrorCodes() {
    }

    private static Rule rule(String regex, String code) {
        return new Rule(Pattern.compile(regex, Pattern.CASE_INSENSITIVE), code);
    }

    /** The javac code for a compiler message, or {@link #UNCLASSIFIED} if no rule matches. */
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
}
