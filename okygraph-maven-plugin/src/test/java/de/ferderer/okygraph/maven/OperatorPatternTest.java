package de.ferderer.okygraph.maven;

import java.util.regex.Matcher;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class OperatorSeparatorPatternTest {

    private Matcher matcher(TokenPattern pattern, String input, int start) {
        return pattern.pattern.matcher(input).region(start, input.length());
    }

    @Test
    void testOperatorSeparatorType() {
        assertEquals(TokenType.JAVA_TOKEN, TokenPattern.OPERATOR.type);
    }

    @Test
    void testOperatorSeparatorMatches() {
        String[][] tests = {
            {">>>=", "0", ">>>="},  // 3-char
            {">>", "0", ">>"},      // 2-char
            {"+", "0", "+"},        // 1-char operator
            {";", "0", ";"},        // separator
            {"...", "0", "..."},    // varargs
            {"x+y", "1", "+"},      // in code
        };
        for (String[] test : tests) {
            Matcher m = matcher(TokenPattern.OPERATOR, test[0], Integer.parseInt(test[1]));
            assertTrue(m.lookingAt(), "Should match: " + test[0]);
            assertEquals(test[2], m.group());
        }
    }

    @Test
    void testOperatorSeparatorAtEnd() {
        String line = "code";
        Matcher m = matcher(TokenPattern.OPERATOR, line, line.length());
        assertFalse(m.lookingAt());
    }
}
