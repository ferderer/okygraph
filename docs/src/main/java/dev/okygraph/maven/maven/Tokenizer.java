package dev.okygraph.maven;

import java.util.*;
import java.util.regex.*;

public class Tokenizer {

    enum TokenType { 
        // Literals
        STRING, 
        TEXT_BLOCK_START, 
        TEXT_BLOCK_LINE, 
        TEXT_BLOCK_END, 
        CHARACTER,
        NUMBER,

        // Comments
        COMMENT, 
        MULTILINE_COMMENT_START, 
        MULTILINE_COMMENT_LINE, 
        MULTILINE_COMMENT_END,

        // Identifiers and Keywords
        KEYWORD, 
        IDENTIFIER,

        // Template tokens
        TEMPLATE_START,    // {`
        TEMPLATE_END,      // `}

        // Operators and Separators
        OPERATOR, 
        SEPARATOR,

        // Other
        WHITESPACE
    }

    private record TokenSpec(TokenType type, Pattern pattern) {}

    private enum ParseMode {
        JAVA(List.of(
            // String literals (must come before other quotes)
            of(TokenType.STRING, "\"(\\\\.|[^\"\\\\])*\""),

            // Text blocks (Java 13+)
            of(TokenType.TEXT_BLOCK_START, "\"\"\""),

            // Character literals
            of(TokenType.CHARACTER, "'(\\\\.|[^'\\\\])'"),

            // Comments
            of(TokenType.COMMENT, "//.*"),                           // Single-line comment
            of(TokenType.COMMENT, "/\\*.*?\\*/"),                    // Multi-line comment on single line
            of(TokenType.MULTILINE_COMMENT_START, "/\\*(?!.*\\*/).*"), // Multi-line comment start

            // Whitespace
            of(TokenType.WHITESPACE, "\\s+"),

            // Numbers (order matters - more specific patterns first)
            // Hexadecimal floating-point literals
            of(TokenType.NUMBER, "0[xX][0-9a-fA-F]+(_[0-9a-fA-F]+)*\\.[0-9a-fA-F]+(_[0-9a-fA-F]+)*[pP][+-]?[0-9]+(_[0-9]+)*[fFdD]?"),
            of(TokenType.NUMBER, "0[xX][0-9a-fA-F]+(_[0-9a-fA-F]+)*[pP][+-]?[0-9]+(_[0-9]+)*[fFdD]?"),

            // Decimal floating-point literals
            of(TokenType.NUMBER, "[0-9]+(_[0-9]+)*\\.[0-9]+(_[0-9]+)*([eE][+-]?[0-9]+(_[0-9]+)*)?[fFdD]?"),
            of(TokenType.NUMBER, "\\.[0-9]+(_[0-9]+)*([eE][+-]?[0-9]+(_[0-9]+)*)?[fFdD]?"),
            of(TokenType.NUMBER, "[0-9]+(_[0-9]+)*[eE][+-]?[0-9]+(_[0-9]+)*[fFdD]?"),
            of(TokenType.NUMBER, "[0-9]+(_[0-9]+)*[fFdD]"),

            // Integer literals
            of(TokenType.NUMBER, "0[xX][0-9a-fA-F]+(_[0-9a-fA-F]+)*[lL]?"),  // Hexadecimal
            of(TokenType.NUMBER, "0[bB][01]+(_[01]+)*[lL]?"),                // Binary
            of(TokenType.NUMBER, "0[0-7]+(_[0-7]+)*[lL]?"),                  // Octal
            of(TokenType.NUMBER, "[0-9]+(_[0-9]+)*[lL]?"),                   // Decimal

            // Keywords (all Java keywords including modern ones)
            of(TokenType.KEYWORD, "\\b(abstract|assert|boolean|break|byte|case|catch|char|class|const|continue)\\b"),
            of(TokenType.KEYWORD, "\\b(default|do|double|else|enum|extends|final|finally|float|for|goto|if)\\b"),
            of(TokenType.KEYWORD, "\\b(implements|import|instanceof|int|interface|long|native|new|package)\\b"),
            of(TokenType.KEYWORD, "\\b(private|protected|public|return|short|static|strictfp|super|switch)\\b"),
            of(TokenType.KEYWORD, "\\b(synchronized|this|throw|throws|transient|try|void|volatile|while)\\b"),
            of(TokenType.KEYWORD, "\\b(true|false|null)\\b"),  // Literal keywords
            of(TokenType.KEYWORD, "\\b(record|sealed|non-sealed|permits|yield|var|when)\\b"), // Newer keywords
            of(TokenType.KEYWORD, "\\b(module|requires|exports|opens|uses|provides|to|with|open|transitive)\\b"), // Module keywords

            // Identifiers (after keywords to avoid conflicts)
            of(TokenType.IDENTIFIER, "[a-zA-Z_$\\p{L}][a-zA-Z0-9_$\\p{L}]*"),

            // Template markers (must come before regular braces)
            of(TokenType.TEMPLATE_START, "\\{`"),
            of(TokenType.TEMPLATE_END, "`\\}"),

            // Operators (longest first to avoid partial matches)
            // Assignment operators
            of(TokenType.OPERATOR, ">>>="),
            of(TokenType.OPERATOR, "<<="),
            of(TokenType.OPERATOR, ">>="),
            of(TokenType.OPERATOR, "\\+="),
            of(TokenType.OPERATOR, "-="),
            of(TokenType.OPERATOR, "\\*="),
            of(TokenType.OPERATOR, "/="),
            of(TokenType.OPERATOR, "%="),
            of(TokenType.OPERATOR, "&="),
            of(TokenType.OPERATOR, "\\|="),
            of(TokenType.OPERATOR, "\\^="),

            // Shift operators
            of(TokenType.OPERATOR, ">>>"),
            of(TokenType.OPERATOR, "<<"),
            of(TokenType.OPERATOR, ">>"),

            // Relational operators
            of(TokenType.OPERATOR, "<="),
            of(TokenType.OPERATOR, ">="),
            of(TokenType.OPERATOR, "=="),
            of(TokenType.OPERATOR, "!="),

            // Logical operators
            of(TokenType.OPERATOR, "&&"),
            of(TokenType.OPERATOR, "\\|\\|"),

            // Increment/decrement
            of(TokenType.OPERATOR, "\\+\\+"),
            of(TokenType.OPERATOR, "--"),

            // Lambda and method reference
            of(TokenType.OPERATOR, "->"),
            of(TokenType.OPERATOR, "::"),

            // Other operators
            of(TokenType.OPERATOR, "[+\\-*/%]"),    // Arithmetic
            of(TokenType.OPERATOR, "[<>]"),         // Comparison
            of(TokenType.OPERATOR, "[&|\\^~]"),     // Bitwise
            of(TokenType.OPERATOR, "!"),            // Logical NOT
            of(TokenType.OPERATOR, "="),            // Assignment
            of(TokenType.OPERATOR, "\\?"),          // Ternary conditional
            of(TokenType.OPERATOR, ":"),            // Ternary colon (also used in switch expressions)

            // Separators
            of(TokenType.SEPARATOR, "\\.\\.\\."),   // Varargs
            of(TokenType.SEPARATOR, "\\."),         // Dot
            of(TokenType.SEPARATOR, ","),           // Comma
            of(TokenType.SEPARATOR, ";"),           // Semicolon
            of(TokenType.SEPARATOR, "\\("),         // Left parenthesis
            of(TokenType.SEPARATOR, "\\)"),         // Right parenthesis
            of(TokenType.SEPARATOR, "\\["),         // Left bracket
            of(TokenType.SEPARATOR, "\\]"),         // Right bracket
            of(TokenType.SEPARATOR, "\\{"),         // Left brace
            of(TokenType.SEPARATOR, "\\}"),         // Right brace
            of(TokenType.SEPARATOR, "@")            // Annotation marker
        )),

        TEMPLATE(List.of(
            // In template mode, you'll need different tokenization rules
            // This is a placeholder - implement based on your template syntax
            of(TokenType.TEMPLATE_END, "`\\}")
            // Add other template-specific tokens here
        )),

        MULTILINE_COMMENT(List.of(
            of(TokenType.MULTILINE_COMMENT_END, ".*?\\*/"),     // Capture up to and including */
            of(TokenType.MULTILINE_COMMENT_LINE, ".*")          // Capture entire line if no end marker
        )),

        TEXT_BLOCK(List.of(
            of(TokenType.TEXT_BLOCK_END, ".*?\"\"\""),          // Capture up to and including """
            of(TokenType.TEXT_BLOCK_LINE, ".*")                 // Capture entire line if no end marker
        ));

        private final List<TokenSpec> spec;

        ParseMode(List<TokenSpec> spec) {
            this.spec = spec;
        }

        public List<TokenSpec> getSpec() {
            return spec;
        }

        private static TokenSpec of(TokenType type, String pattern) {
            return new TokenSpec(type, Pattern.compile(pattern));
        }
    }

    private record Token(String type, String value, int line, int column) {}
    
    // Current state
    private final String source;
    private final String[] lines;
    private int currentLine;
    private int currentPosition;
    private final Deque<ParseMode> modeStack;
    private final Token currentToken;
    
    // Initialize token specifications for each parse mode
    private static void initializeTokenSpecs() {
        // JAVA_CODE mode - recognize Java tokens and template boundaries
        TOKEN_SPECS.put(ParseMode.JAVA_CODE, List.of(
            new TokenSpec(Pattern.compile("^\\s+"), "WHITESPACE"),
            new TokenSpec(Pattern.compile("^//.*$"), "LINE_COMMENT"),
            new TokenSpec(Pattern.compile("^/\\*"), "BLOCK_COMMENT_START"),
            new TokenSpec(Pattern.compile("^\"\"\""), "TEXT_BLOCK_START"),
            new TokenSpec(Pattern.compile("^\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\""), "STRING_LITERAL"),
            new TokenSpec(Pattern.compile("^'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'"), "CHAR_LITERAL"),
            new TokenSpec(Pattern.compile("^\\d+(\\.\\d+)?([eE][+-]?\\d+)?"), "NUMBER"),
            new TokenSpec(Pattern.compile("^(abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|goto|if|implements|import|instanceof|int|interface|long|native|new|null|package|private|protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while|true|false)\\b"), "KEYWORD"),
            new TokenSpec(Pattern.compile("^[a-zA-Z_$][a-zA-Z0-9_$]*"), "IDENTIFIER"),
            new TokenSpec(Pattern.compile("^(\\+\\+|--|==|!=|<=|>=|&&|\\|\\||<<|>>|>>>|\\+=|-=|\\*=|/=|%=|&=|\\|=|\\^=|<<=|>>=|>>>=)"), "OPERATOR"),
            new TokenSpec(Pattern.compile("^[+\\-*/%=<>&|^~!?:]"), "OPERATOR"),
            new TokenSpec(Pattern.compile("^[(){}\\[\\];,.]"), "DELIMITER"),
            new TokenSpec(Pattern.compile("^\\{`"), "TEMPLATE_START"),
            new TokenSpec(Pattern.compile("^."), "UNKNOWN")
        ));
        
        // HTML_CONTENT mode - recognize HTML content and template control
        TOKEN_SPECS.put(ParseMode.HTML_CONTENT, List.of(
            new TokenSpec(Pattern.compile("^\\{"), "EXPR_START"),
            new TokenSpec(Pattern.compile("^`if`"), "IF_START"),
            new TokenSpec(Pattern.compile("^`for`"), "FOR_START"),
            new TokenSpec(Pattern.compile("^`each`"), "EACH_START"),
            new TokenSpec(Pattern.compile("^`while`"), "WHILE_START"),
            new TokenSpec(Pattern.compile("^`}`"), "JAVA_CONTINUE"),
            new TokenSpec(Pattern.compile("^`}``"), "BLOCK_END"),
            new TokenSpec(Pattern.compile("^<!--"), "HTML_COMMENT_START"),
            new TokenSpec(Pattern.compile("^[^{`<]+"), "HTML_TEXT"),
            new TokenSpec(Pattern.compile("^."), "HTML_CHAR")
        ));
        
        // JAVA_EXPRESSION mode - inside {expression}
        TOKEN_SPECS.put(ParseMode.JAVA_EXPRESSION, List.of(
            new TokenSpec(Pattern.compile("^\\s+"), "WHITESPACE"),
            new TokenSpec(Pattern.compile("^\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\""), "STRING_LITERAL"),
            new TokenSpec(Pattern.compile("^'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'"), "CHAR_LITERAL"),
            new TokenSpec(Pattern.compile("^\\d+(\\.\\d+)?([eE][+-]?\\d+)?"), "NUMBER"),
            new TokenSpec(Pattern.compile("^[a-zA-Z_$][a-zA-Z0-9_$]*"), "IDENTIFIER"),
            new TokenSpec(Pattern.compile("^(\\+\\+|--|==|!=|<=|>=|&&|\\|\\||<<|>>)"), "OPERATOR"),
            new TokenSpec(Pattern.compile("^[+\\-*/%=<>&|^~!?:()\\[\\].,]"), "OPERATOR"),
            new TokenSpec(Pattern.compile("^\\}"), "EXPR_END"),
            new TokenSpec(Pattern.compile("^."), "UNKNOWN")
        ));
        
        // JAVA_CONTROL mode - inside `if (condition) {` etc.
        TOKEN_SPECS.put(ParseMode.JAVA_CONTROL, List.of(
            new TokenSpec(Pattern.compile("^\\s+"), "WHITESPACE"),
            new TokenSpec(Pattern.compile("^\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\""), "STRING_LITERAL"),
            new TokenSpec(Pattern.compile("^'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'"), "CHAR_LITERAL"),
            new TokenSpec(Pattern.compile("^\\d+(\\.\\d+)?([eE][+-]?\\d+)?"), "NUMBER"),
            new TokenSpec(Pattern.compile("^[a-zA-Z_$][a-zA-Z0-9_$]*"), "IDENTIFIER"),
            new TokenSpec(Pattern.compile("^(\\+\\+|--|==|!=|<=|>=|&&|\\|\\||<<|>>)"), "OPERATOR"),
            new TokenSpec(Pattern.compile("^[+\\-*/%=<>&|^~!?:()\\[\\].,]"), "OPERATOR"),
            new TokenSpec(Pattern.compile("^\\{"), "BLOCK_START"),
            new TokenSpec(Pattern.compile("^."), "UNKNOWN")
        ));
        
        // Other modes...
        TOKEN_SPECS.put(ParseMode.JAVA_BLOCK_COMMENT, List.of(
            new TokenSpec(Pattern.compile("^\\*/"), "BLOCK_COMMENT_END"),
            new TokenSpec(Pattern.compile("^[^*]+"), "COMMENT_TEXT"),
            new TokenSpec(Pattern.compile("^\\*"), "STAR"),
            new TokenSpec(Pattern.compile("^."), "COMMENT_CHAR")
        ));
        
        TOKEN_SPECS.put(ParseMode.JAVA_TEXT_BLOCK, List.of(
            new TokenSpec(Pattern.compile("^\"\"\""), "TEXT_BLOCK_END"),
            new TokenSpec(Pattern.compile("^[^\"]+"), "TEXT_BLOCK_CONTENT"),
            new TokenSpec(Pattern.compile("^\""), "QUOTE"),
            new TokenSpec(Pattern.compile("^."), "TEXT_BLOCK_CHAR")
        ));
        
        TOKEN_SPECS.put(ParseMode.HTML_COMMENT, List.of(
            new TokenSpec(Pattern.compile("^-->"), "HTML_COMMENT_END"),
            new TokenSpec(Pattern.compile("^[^-]+"), "COMMENT_TEXT"),
            new TokenSpec(Pattern.compile("^-"), "DASH"),
            new TokenSpec(Pattern.compile("^."), "COMMENT_CHAR")
        ));
    }
    
    public TemplateParser(String source) {
        this.source = source;
        this.lines = source.split("\n", -1);
        this.currentLine = 0;
        this.currentPosition = 0;
        this.modeStack = new Stack<>();
        this.modeStack.push(ParseMode.JAVA_CODE);
        this.currentToken = null;
    }
    
    // Main parser entry point
    public ContainerNode parse() {
        List<ASTNode> nodes = new ArrayList<>();
        
        while (currentLine < lines.length) {
            ASTNode lineNode = parseLine();
            if (lineNode != null) {
                nodes.add(lineNode);
            }
            nextLine();
        }
        
        return new ContainerNode(NodeType.TEMPLATE_METHOD, 1, nodes);
    }
    
    // Parse a single line based on current mode
    private ASTNode parseLine() {
        String line = getCurrentLine();
        if (line.trim().isEmpty()) {
            return new TerminalNode(NodeType.JAVA_LINE, currentLine + 1, line);
        }
        
        ParseMode mode = getCurrentMode();
        currentPosition = 0;
        
        switch (mode) {
            case JAVA_CODE -> {
                return parseJavaLine(line);
            }
            case HTML_CONTENT -> {
                return parseHtmlLine(line);
            }
            case JAVA_EXPRESSION -> {
                return parseExpressionLine(line);
            }
            case JAVA_CONTROL -> {
                return parseControlLine(line);
            }
            case JAVA_BLOCK_COMMENT -> {
                return parseBlockCommentLine(line);
            }
            case JAVA_TEXT_BLOCK -> {
                return parseTextBlockLine(line);
            }
            case HTML_COMMENT -> {
                return parseHtmlCommentLine(line);
            }
        }
        
        return null;
    }
    
    // Parse Java code line
    private ASTNode parseJavaLine(String line) {
        List<Token> tokens = tokenizeLine(line, ParseMode.JAVA_CODE);
        
        // Check for template start
        for (Token token : tokens) {
            if ("TEMPLATE_START".equals(token.type())) {
                enterMode(ParseMode.HTML_CONTENT);
                break;
            }
            if ("BLOCK_COMMENT_START".equals(token.type())) {
                enterMode(ParseMode.JAVA_BLOCK_COMMENT);
                break;
            }
            if ("TEXT_BLOCK_START".equals(token.type())) {
                enterMode(ParseMode.JAVA_TEXT_BLOCK);
                break;
            }
        }
        
        return new TerminalNode(NodeType.JAVA_LINE, currentLine + 1, line);
    }
    
    // Parse HTML content line
    private ASTNode parseHtmlLine(String line) {
        List<Token> tokens = tokenizeLine(line, ParseMode.HTML_CONTENT);
        
        // Check for control flow or template end
        for (Token token : tokens) {
            switch (token.type()) {
                case "IF_START" -> {
                    enterMode(ParseMode.JAVA_CONTROL);
                    // TODO: Parse if condition
                }
                case "FOR_START" -> {
                    enterMode(ParseMode.JAVA_CONTROL);
                    // TODO: Parse for condition
                }
                case "BLOCK_END" -> {
                    exitMode(); // Back to JAVA_CODE
                }
                case "HTML_COMMENT_START" -> {
                    enterMode(ParseMode.HTML_COMMENT);
                }
                case "EXPR_START" -> {
                    enterMode(ParseMode.JAVA_EXPRESSION);
                }
            }
        }
        
        return parseHtmlLineWithExpressions(line, tokens);
    }
    
    // Parse HTML line splitting on expressions
    private ASTNode parseHtmlLineWithExpressions(String line, List<Token> tokens) {
        // TODO: Split line into HTML_EXPRESSION and JAVA_EXPRESSION parts
        // For now, return simple HTML line
        return new TerminalNode(NodeType.HTML_LINE, currentLine + 1, line);
    }
    
    // Parse expression line
    private ASTNode parseExpressionLine(String line) {
        List<Token> tokens = tokenizeLine(line, ParseMode.JAVA_EXPRESSION);
        
        // Check for expression end
        for (Token token : tokens) {
            if ("EXPR_END".equals(token.type())) {
                exitMode(); // Back to HTML_CONTENT
                break;
            }
        }
        
        // TODO: Handle expression content
        return new TerminalNode(NodeType.JAVA_EXPRESSION, currentLine + 1, line);
    }
    
    // Parse control flow line
    private ASTNode parseControlLine(String line) {
        List<Token> tokens = tokenizeLine(line, ParseMode.JAVA_CONTROL);
        
        // Check for block start
        for (Token token : tokens) {
            if ("BLOCK_START".equals(token.type())) {
                exitMode(); // Back to HTML_CONTENT
                enterMode(ParseMode.HTML_CONTENT);
                break;
            }
        }
        
        // TODO: Handle control flow condition
        return new TerminalNode(NodeType.JAVA_CONDITION, currentLine + 1, line);
    }
    
    // Parse block comment line
    private ASTNode parseBlockCommentLine(String line) {
        List<Token> tokens = tokenizeLine(line, ParseMode.JAVA_BLOCK_COMMENT);
        
        // Check for comment end
        for (Token token : tokens) {
            if ("BLOCK_COMMENT_END".equals(token.type())) {
                exitMode(); // Back to previous mode
                break;
            }
        }
        
        return new TerminalNode(NodeType.JAVA_LINE, currentLine + 1, line);
    }
    
    // Parse text block line
    private ASTNode parseTextBlockLine(String line) {
        List<Token> tokens = tokenizeLine(line, ParseMode.JAVA_TEXT_BLOCK);
        
        // Check for text block end
        for (Token token : tokens) {
            if ("TEXT_BLOCK_END".equals(token.type())) {
                exitMode(); // Back to JAVA_CODE
                break;
            }
        }
        
        return new TerminalNode(NodeType.JAVA_LINE, currentLine + 1, line);
    }
    
    // Parse HTML comment line
    private ASTNode parseHtmlCommentLine(String line) {
        List<Token> tokens = tokenizeLine(line, ParseMode.HTML_COMMENT);
        
        // Check for comment end
        for (Token token : tokens) {
            if ("HTML_COMMENT_END".equals(token.type())) {
                exitMode(); // Back to HTML_CONTENT
                break;
            }
        }
        
        return new TerminalNode(NodeType.HTML_LINE, currentLine + 1, line);
    }
    
    // Tokenize a line using the current mode's token specification
    private List<Token> tokenizeLine(String line, ParseMode mode) {
        List<Token> tokens = new ArrayList<>();
        List<TokenSpec> specs = TOKEN_SPECS.get(mode);
        
        int position = 0;
        while (position < line.length()) {
            String remaining = line.substring(position);
            boolean matched = false;
            
            for (TokenSpec spec : specs) {
                Matcher matcher = spec.pattern().matcher(remaining);
                if (matcher.find()) {
                    String value = matcher.group();
                    if (spec.type() != null) { // Skip null types (whitespace, comments)
                        tokens.add(new Token(spec.type(), value, currentLine + 1, position + 1));
                    }
                    position += value.length();
                    matched = true;
                    break;
                }
            }
            
            if (!matched) {
                // Unexpected character - advance one position
                position++;
            }
        }
        
        return tokens;
    }
    
    // Mode stack operations
    private void enterMode(ParseMode mode) {
        modeStack.push(mode);
    }
    
    private void exitMode() {
        if (modeStack.size() > 1) {
            modeStack.pop();
        }
    }
    
    private ParseMode getCurrentMode() {
        return modeStack.peek();
    }
    
    // Line navigation
    private String getCurrentLine() {
        return currentLine < lines.length ? lines[currentLine] : "";
    }
    
    private void nextLine() {
        currentLine++;
        currentPosition = 0;
    }
    
    // Error handling
    private void error(String message) {
        throw new ParseException(String.format("Line %d:%d - %s", 
            currentLine + 1, currentPosition + 1, message));
    }
    
    public static class ParseException extends RuntimeException {
        public ParseException(String message) {
            super(message);
        }
    }
}