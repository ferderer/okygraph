package de.ferderer.okygraph.maven;

import java.util.regex.Matcher;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NumberLiteralPatternTest {

    private Matcher matcher(TokenPattern pattern, String input, int start) {
        return pattern.pattern.matcher(input).region(start, input.length());
    }

    @Test
    void testNumberLiteralType() {
        assertEquals(TokenType.JAVA_TOKEN, TokenPattern.NUMBER.type);
    }

    // Hexadecimal floating-point
    @Test
    void testHexFloatLiterals() {
        String[][] tests = {
            {"0x1.8p1", "0", "0x1.8p1"},           // with decimal point
            {"0X1.8P1", "0", "0X1.8P1"},           // uppercase
            {"0x1p10", "0", "0x1p10"},             // without decimal point
            {"0x1.0p-5f", "0", "0x1.0p-5f"},       // with suffix
            {"0x1_2.3_4p5_6", "0", "0x1_2.3_4p5_6"}, // with underscores
            {"x=0x1.8p1", "2", "0x1.8p1"},         // in code
        };
        for (String[] test : tests) {
            Matcher m = matcher(TokenPattern.NUMBER, test[0], Integer.parseInt(test[1]));
            assertTrue(m.lookingAt(), "Should match: " + test[0]);
            assertEquals(test[2], m.group());
        }
    }

    // Decimal floating-point
    @Test
    void testDecimalFloatLiterals() {
        String[][] tests = {
            {"1.5", "0", "1.5"},                   // simple
            {"1.5e10", "0", "1.5e10"},             // with exponent
            {"1.5E-10", "0", "1.5E-10"},           // negative exponent
            {"1.5f", "0", "1.5f"},                 // float suffix
            {"1.5d", "0", "1.5d"},                 // double suffix
            {".5", "0", ".5"},                     // leading dot
            {".5e10", "0", ".5e10"},               // leading dot with exp
            {"1e10", "0", "1e10"},                 // no decimal, with exp
            {"1f", "0", "1f"},                     // just suffix
            {"1_000.5_5", "0", "1_000.5_5"},       // with underscores
            {"x=1.5", "2", "1.5"},                 // in code
        };
        for (String[] test : tests) {
            Matcher m = matcher(TokenPattern.NUMBER, test[0], Integer.parseInt(test[1]));
            assertTrue(m.lookingAt(), "Should match: " + test[0]);
            assertEquals(test[2], m.group());
        }
    }

    // Integer literals
    @Test
    void testIntegerLiterals() {
        String[][] tests = {
            {"42", "0", "42"},                     // decimal
            {"42L", "0", "42L"},                   // long
            {"1_000_000", "0", "1_000_000"},       // with underscores
            {"0x1F", "0", "0x1F"},                 // hex
            {"0X1f", "0", "0X1f"},                 // hex uppercase
            {"0x1FL", "0", "0x1FL"},               // hex long
            {"0b101", "0", "0b101"},               // binary
            {"0B101", "0", "0B101"},               // binary uppercase
            {"0b101L", "0", "0b101L"},             // binary long
            {"0755", "0", "0755"},                 // octal
            {"0755L", "0", "0755L"},               // octal long
            {"x=42", "2", "42"},                   // in code
        };
        for (String[] test : tests) {
            Matcher m = matcher(TokenPattern.NUMBER, test[0], Integer.parseInt(test[1]));
            assertTrue(m.lookingAt(), "Should match: " + test[0]);
            assertEquals(test[2], m.group());
        }
    }

    @ParameterizedTest
    @CsvSource(delimiterString = "|", value = {
        "x42|0",                    // wrong start position
        "0xG|0",                    // invalid hex digit
        "0b2|0",                    // invalid binary digit  
        ".e10|0",                   // missing digits before exponent
    })
    void testNumberLiteralNoMatch(String input, int start) {
        Matcher m = matcher(TokenPattern.NUMBER, input, start);
        assertFalse(m.lookingAt(), "Should not match at position " + start + ": " + input);
    }

    @Test
    void testNumberLiteralAtEnd() {
        String line = "code";
        Matcher m = matcher(TokenPattern.NUMBER, line, line.length());
        assertFalse(m.lookingAt(), "Should not match at end of string");
    }
}
