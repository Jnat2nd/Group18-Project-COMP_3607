package group18.listener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@DisplayName("ErrorCodes classifies compiler messages")
class ErrorCodesTest {

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @CsvFileSource(resources = "/compiler-messages.csv", numLinesToSkip = 1)
    void mapsRealJavacMessagesToTheirDiagnosticCodes(String message, String expectedCode) {
        assertEquals(expectedCode, ErrorCodes.code(message));
    }

    @Test
    void readsTheFirstLineOfAMultiLineMessage() {
        assertEquals("compiler.err.cant.resolve",
                ErrorCodes.code("cannot find symbol\n  symbol:   variable balanse\n  location: class BankAccount"));
    }

    @Test
    void ignoresLetterCase() {
        assertEquals("compiler.err.cant.resolve", ErrorCodes.code("Cannot Find Symbol"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void missingMessagesAreUnclassified(String message) {
        assertEquals(ErrorCodes.UNCLASSIFIED, ErrorCodes.code(message));
    }

    @Test
    void everyCodeHasAReadableLabel() {
        assertEquals("Cannot find symbol", ErrorCodes.label("compiler.err.cant.resolve"));
        assertEquals("Missing return statement", ErrorCodes.label("compiler.err.missing.ret.stmt"));
        assertEquals("Other error", ErrorCodes.label(ErrorCodes.UNCLASSIFIED));
        assertEquals("Other error", ErrorCodes.label("no.such.code"));
        assertNotEquals(ErrorCodes.label("compiler.err.expected"), ErrorCodes.label("compiler.err.expected4"));
    }
}
