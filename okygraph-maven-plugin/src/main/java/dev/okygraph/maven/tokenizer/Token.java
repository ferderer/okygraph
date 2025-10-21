package dev.okygraph.maven.tokenizer;

/**
 * Represents a single token in the source code.
 *
 * @param type The type of token
 * @param value The actual text value of the token
 * @param line Line number (1-based)
 * @param column Column number (1-based)
 */
public record Token(TokenType type, String value, int line, int column) {

    /**
     * Creates a token with the given type and value.
     */
    public Token {
        if (type == null) {
            throw new IllegalArgumentException("Token type cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Token value cannot be null");
        }
        if (line < 1) {
            throw new IllegalArgumentException("Line number must be >= 1");
        }
        if (column < 1) {
            throw new IllegalArgumentException("Column number must be >= 1");
        }
    }

    @Override
    public String toString() {
        return String.format("%s('%s') at %d:%d", type, value, line, column);
    }

    /**
     * Returns true if this token is of the given type.
     */
    public boolean is(TokenType type) {
        return this.type == type;
    }

    /**
     * Returns true if this token is one of the given types.
     */
    public boolean isOneOf(TokenType... types) {
        for (TokenType t : types) {
            if (this.type == t) {
                return true;
            }
        }
        return false;
    }
}
