import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorCodesTest {

    @Test
    void mapsCommonJavacMessagesToTheirCodes() {
        assertEquals("compiler.err.cant.resolve", ErrorCodes.code("cannot find symbol\n  symbol: variable x"));
        assertEquals("compiler.err.expected", ErrorCodes.code("';' expected"));
        assertEquals("compiler.err.expected", ErrorCodes.code("<identifier> expected"));
        assertEquals("compiler.err.expected4", ErrorCodes.code("class, interface, enum, or record expected"));
        assertEquals("compiler.err.prob.found.req", ErrorCodes.code("incompatible types: String cannot be converted to int"));
        assertEquals("compiler.err.missing.ret.stmt", ErrorCodes.code("missing return statement"));
        assertEquals("compiler.err.premature.eof", ErrorCodes.code("reached end of file while parsing"));
    }

    @Test
    void matchingIgnoresCase() {
        assertEquals("compiler.err.cant.resolve", ErrorCodes.code("Cannot Find Symbol"));
    }

    @Test
    void unknownOrMissingMessagesAreUnclassified() {
        assertEquals(ErrorCodes.UNCLASSIFIED, ErrorCodes.code("something new from a future javac"));
        assertEquals(ErrorCodes.UNCLASSIFIED, ErrorCodes.code(""));
        assertEquals(ErrorCodes.UNCLASSIFIED, ErrorCodes.code(null));
    }
}
