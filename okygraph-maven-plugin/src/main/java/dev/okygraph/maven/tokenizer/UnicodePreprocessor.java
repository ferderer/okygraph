package dev.okygraph.maven.tokenizer;

/**
 * Preprocessor for Unicode escapes in Java source code.
 *
 * According to the Java Language Specification (JLS §3.3), Unicode escapes
 * are processed before tokenization. This class handles the conversion of
 * Unicode escape sequences (backslash-u-XXXX) to their corresponding Unicode characters.
 *
 * Example:
 * <pre>
 * String source = "\\u0048\\u0065\\u006C\\u006C\\u006F"; // Hello
 * String processed = UnicodePreprocessor.process(source);
 * // processed = "Hello"
 * </pre>
 *
 * @author Okygraph Team
 */
public class UnicodePreprocessor {

    /**
     * Processes Unicode escapes in the given source code.
     *
     * @param source The source code with potential Unicode escapes
     * @return The source code with Unicode escapes converted to characters
     */
    public static String process(String source) {
        if (source == null || source.isEmpty()) {
            return source;
        }

        StringBuilder result = new StringBuilder(source.length());
        int i = 0;

        while (i < source.length()) {
            // Check for Unicode escape: backslash-u-XXXX
            if (isUnicodeEscape(source, i)) {
                int codePoint = parseUnicodeEscape(source, i);
                result.append((char) codePoint);
                i += 6; // Skip backslash-u-XXXX
            } else {
                result.append(source.charAt(i));
                i++;
            }
        }

        return result.toString();
    }

    /**
     * Checks if there's a valid Unicode escape at the given position.
     *
     * @param source The source code
     * @param pos The position to check
     * @return true if there's a valid Unicode escape (backslash-u-XXXX) at pos
     */
    private static boolean isUnicodeEscape(String source, int pos) {
        // Need at least 6 characters: backslash-u-XXXX
        if (pos + 5 >= source.length()) {
            return false;
        }

        // Check for backslash-u
        if (source.charAt(pos) != '\\' || source.charAt(pos + 1) != 'u') {
            return false;
        }

        // Check for 4 hex digits
        for (int i = 2; i < 6; i++) {
            char ch = source.charAt(pos + i);
            if (!isHexDigit(ch)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Parses a Unicode escape sequence starting at the given position.
     *
     * @param source The source code
     * @param pos The position of the backslash
     * @return The Unicode code point
     */
    private static int parseUnicodeEscape(String source, int pos) {
        String hexString = source.substring(pos + 2, pos + 6);
        return Integer.parseInt(hexString, 16);
    }

    /**
     * Checks if a character is a hexadecimal digit.
     */
    private static boolean isHexDigit(char ch) {
        return (ch >= '0' && ch <= '9') ||
               (ch >= 'a' && ch <= 'f') ||
               (ch >= 'A' && ch <= 'F');
    }
}
