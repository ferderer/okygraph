package dev.okygraph.maven;

import java.util.*;

/**
 * Recursive descent parser for template files.
 * Recognizes Java tokens and parses template method bodies.
 */
public class TemplateParser {
    
    private final String source;
    private int position = 0;
    private int line = 1;
    private int column = 1;
    
    // Current state
    private final StringBuilder currentJavaCode = new StringBuilder();
    private final List<ASTNode> nodes = new ArrayList<>();
    
    // Token types for recognition
    private enum TokenType {
        IDENTIFIER, KEYWORD, STRING_LITERAL, CHAR_LITERAL, NUMBER,
        OPERATOR, DELIMITER, LINE_COMMENT, BLOCK_COMMENT,
        TEMPLATE_START, TEMPLATE_END, WHITESPACE, NEWLINE, UNKNOWN
    }
    
    // Java keywords for recognition
    private static final Set<String> JAVA_KEYWORDS = Set.of(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
        "class", "const", "continue", "default", "do", "double", "else", "enum",
        "extends", "final", "finally", "float", "for", "goto", "if", "implements",
        "import", "instanceof", "int", "interface", "long", "native", "new", "null",
        "package", "private", "protected", "public", "return", "short", "static",
        "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
        "transient", "try", "void", "volatile", "while", "true", "false"
    );
    
    public TemplateParser(String source) {
        this.source = source;
    }
    
    // Main entry point
    public ContainerNode parse() {
        scanForTemplateStart();
        flushJavaCode();
        return new ContainerNode(NodeType.TEMPLATE_CLASS, 1, nodes);
    }
    
    // Main scanning logic
    private void scanForTemplateStart() {
        while (!isAtEnd()) {
            if (lookAhead("{`")) {
                // Add the opening brace to Java code before flushing
                currentJavaCode.append("{");
                flushJavaCode();
                parseTemplateMethod();
            } else {
                TokenType token = recognizeToken();
                skipToken(token);
            }
        }
    }
    
    // Parse template method body between {` and `}
    private void parseTemplateMethod() {
        advance(2); // Skip {`
        int templateStartLine = line;
        List<ASTNode> templateNodes = parseTemplateContent();
        
        if (!lookAhead("`}")) {
            error("Expected `} to close template method");
        }
        advance(2); // Skip `}
        
        // Add the closing brace to Java code
        currentJavaCode.append("}");
        
        // Add template content as a single template method node
        nodes.add(new ContainerNode(NodeType.TEMPLATE_METHOD, templateStartLine, templateNodes));
    }
    
    // Parse template content (HTML blocks, control flow, expressions)
    private List<ASTNode> parseTemplateContent() {
        List<ASTNode> templateNodes = new ArrayList<>();
        StringBuilder htmlBuffer = new StringBuilder();
        int htmlStartLine = line;
        
        while (!isAtEnd() && !lookAhead("`}")) {
            if (lookAhead("@if")) {
                flushHtmlBuffer(htmlBuffer, templateNodes, htmlStartLine);
                templateNodes.add(parseIfStatement());
                htmlStartLine = line;
            } else if (lookAhead("@for")) {
                flushHtmlBuffer(htmlBuffer, templateNodes, htmlStartLine);
                templateNodes.add(parseForLoop());
                htmlStartLine = line;
            } else if (lookAhead("@each")) {
                flushHtmlBuffer(htmlBuffer, templateNodes, htmlStartLine);
                templateNodes.add(parseEachLoop());
                htmlStartLine = line;
            } else if (lookAhead("@end")) {
                flushHtmlBuffer(htmlBuffer, templateNodes, htmlStartLine);
                break; // End of control block
            } else if (lookAhead("@else")) {
                flushHtmlBuffer(htmlBuffer, templateNodes, htmlStartLine);
                templateNodes.add(new TerminalNode(NodeType.ELSE, line, null));
                advance(5); // Skip @else
                skipWhitespace();
                htmlStartLine = line;
            } else {
                // Regular content - accumulate in HTML buffer
                char c = currentChar();
                htmlBuffer.append(c);
                advance();
            }
        }
        
        flushHtmlBuffer(htmlBuffer, templateNodes, htmlStartLine);
        return templateNodes;
    }
    
    // Parse @if statement
    private ContainerNode parseIfStatement() {
        advance(3); // Skip @if
        skipWhitespace();
        
        if (currentChar() != '(') {
            error("Expected '(' after @if");
        }
        
        String condition = parseParenthesizedExpression();
        skipWhitespace();
        
        List<ASTNode> children = new ArrayList<>();
        children.add(new TerminalNode(NodeType.JAVA_CONDITION, line, condition));
        
        // Parse if body
        List<ASTNode> ifBody = parseTemplateContent();
        if (!ifBody.isEmpty()) {
            children.add(new ContainerNode(NodeType.HTML_BLOCK, line, ifBody));
        }
        
        // Check for @else
        if (lookAhead("@else")) {
            advance(5); // Skip @else
            skipWhitespace();
            children.add(new TerminalNode(NodeType.ELSE, line, null));
            
            List<ASTNode> elseBody = parseTemplateContent();
            if (!elseBody.isEmpty()) {
                children.add(new ContainerNode(NodeType.HTML_BLOCK, line, elseBody));
            }
        }
        
        if (!lookAhead("@end")) {
            error("Expected @end to close @if statement");
        }
        advance(4); // Skip @end
        
        return new ContainerNode(NodeType.IF_STATEMENT, line, children);
    }
    
    // Parse @for loop
    private ContainerNode parseForLoop() {
        advance(4); // Skip @for
        skipWhitespace();
        
        if (currentChar() != '(') {
            error("Expected '(' after @for");
        }
        
        String forExpression = parseParenthesizedExpression();
        skipWhitespace();
        
        List<ASTNode> children = new ArrayList<>();
        children.add(new TerminalNode(NodeType.JAVA_CONDITION, line, forExpression));
        
        List<ASTNode> body = parseTemplateContent();
        if (!body.isEmpty()) {
            children.add(new ContainerNode(NodeType.HTML_BLOCK, line, body));
        }
        
        if (!lookAhead("@end")) {
            error("Expected @end to close @for loop");
        }
        advance(4); // Skip @end
        
        return new ContainerNode(NodeType.FOR_LOOP, line, children);
    }
    
    // Parse @each loop
    private ContainerNode parseEachLoop() {
        advance(5); // Skip @each
        skipWhitespace();
        
        if (currentChar() != '(') {
            error("Expected '(' after @each");
        }
        
        String eachExpression = parseParenthesizedExpression();
        skipWhitespace();
        
        List<ASTNode> children = new ArrayList<>();
        children.add(new TerminalNode(NodeType.JAVA_CONDITION, line, eachExpression));
        
        List<ASTNode> body = parseTemplateContent();
        if (!body.isEmpty()) {
            children.add(new ContainerNode(NodeType.HTML_BLOCK, line, body));
        }
        
        if (!lookAhead("@end")) {
            error("Expected @end to close @each loop");
        }
        advance(4); // Skip @end
        
        return new ContainerNode(NodeType.EACH_LOOP, line, children);
    }
    
    // Parse expression in parentheses, handling nested parens
    private String parseParenthesizedExpression() {
        StringBuilder expr = new StringBuilder();
        advance(); // Skip opening (
        
        int parenDepth = 1;
        while (!isAtEnd() && parenDepth > 0) {
            char c = currentChar();
            if (c == '(') {
                parenDepth++;
            } else if (c == ')') {
                parenDepth--;
            }
            
            if (parenDepth > 0) {
                expr.append(c);
            }
            advance();
        }
        
        return expr.toString().trim();
    }
    
    // Flush accumulated HTML content to nodes
    private void flushHtmlBuffer(StringBuilder htmlBuffer, List<ASTNode> nodes, int startLine) {
        if (htmlBuffer.length() > 0) {
            String content = htmlBuffer.toString();
            List<ASTNode> htmlNodes = parseHtmlContent(content, startLine);
            if (!htmlNodes.isEmpty()) {
                nodes.add(new ContainerNode(NodeType.HTML_BLOCK, startLine, htmlNodes));
            }
            htmlBuffer.setLength(0);
        }
    }
    
    // Parse HTML content with embedded expressions
    private List<ASTNode> parseHtmlContent(String content, int startLine) {
        List<ASTNode> nodes = new ArrayList<>();
        String[] lines = content.split("\n", -1);
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (i == lines.length - 1 && line.isEmpty()) {
                break; // Skip final empty line
            }
            
            List<ASTNode> lineNodes = parseHtmlLine(line, startLine + i);
            nodes.addAll(lineNodes);
        }
        
        return nodes;
    }
    
    // Parse a single HTML line, splitting on {expressions}
    private List<ASTNode> parseHtmlLine(String line, int lineNumber) {
        List<ASTNode> nodes = new ArrayList<>();
        
        int pos = 0;
        while (pos < line.length()) {
            int exprStart = line.indexOf('{', pos);
            if (exprStart == -1) {
                // No more expressions - rest is HTML
                if (pos < line.length()) {
                    nodes.add(new TerminalNode(NodeType.HTML_EXPRESSION, lineNumber, line.substring(pos)));
                }
                break;
            }
            
            // HTML content before expression
            if (exprStart > pos) {
                nodes.add(new TerminalNode(NodeType.HTML_EXPRESSION, lineNumber, line.substring(pos, exprStart)));
            }
            
            // Find matching closing brace
            int exprEnd = findMatchingBrace(line, exprStart);
            if (exprEnd == -1) {
                error("Unclosed expression starting at position " + exprStart);
            }
            
            // Extract expression content (without braces)
            String expression = line.substring(exprStart + 1, exprEnd);
            nodes.add(new TerminalNode(NodeType.JAVA_EXPRESSION, lineNumber, expression));
            
            pos = exprEnd + 1;
        }
        
        return nodes;
    }
    
    // Find matching closing brace, handling nested braces
    private int findMatchingBrace(String line, int start) {
        int depth = 1;
        for (int i = start + 1; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1; // No matching brace found
    }
    
    // Token recognition
    private TokenType recognizeToken() {
        char c = currentChar();
        
        if (Character.isWhitespace(c)) {
            if (c == '\n') return TokenType.NEWLINE;
            return TokenType.WHITESPACE;
        }
        
        if (c == '"') return TokenType.STRING_LITERAL;
        if (c == '\'') return TokenType.CHAR_LITERAL;
        if (c == '/' && peekChar() == '/') return TokenType.LINE_COMMENT;
        if (c == '/' && peekChar() == '*') return TokenType.BLOCK_COMMENT;
        if (Character.isDigit(c)) return TokenType.NUMBER;
        if (Character.isJavaIdentifierStart(c)) {
            String identifier = peekIdentifier();
            return JAVA_KEYWORDS.contains(identifier) ? TokenType.KEYWORD : TokenType.IDENTIFIER;
        }
        
        // Operators and delimiters
        if ("+-*/%=!<>&|^~?:".indexOf(c) >= 0) return TokenType.OPERATOR;
        if ("(){}[];,.".indexOf(c) >= 0) return TokenType.DELIMITER;
        
        return TokenType.UNKNOWN;
    }
    
    // Skip token and add to Java code buffer
    private void skipToken(TokenType token) {
        String tokenText = consumeToken(token);
        currentJavaCode.append(tokenText);
    }
    
    // Consume token and return its text
    private String consumeToken(TokenType token) {
        int start = position;
        
        switch (token) {
            case WHITESPACE -> skipWhitespace();
            case NEWLINE -> { advance(); }
            case STRING_LITERAL -> consumeStringLiteral();
            case CHAR_LITERAL -> consumeCharLiteral();
            case LINE_COMMENT -> consumeLineComment();
            case BLOCK_COMMENT -> consumeBlockComment();
            case NUMBER -> consumeNumber();
            case IDENTIFIER, KEYWORD -> consumeIdentifier();
            case OPERATOR -> consumeOperator();
            case DELIMITER -> advance();
            default -> advance();
        }
        
        return source.substring(start, position);
    }
    
    // Flush accumulated Java code to nodes
    private void flushJavaCode() {
        if (currentJavaCode.length() > 0) {
            String code = currentJavaCode.toString();
            String[] lines = code.split("\n", -1);
            
            // Calculate starting line number for this batch of Java code
            int startLine = line - (lines.length - 1);
            
            for (int i = 0; i < lines.length - 1; i++) {
                // Preserve all lines, including empty ones
                nodes.add(new TerminalNode(NodeType.JAVA_LINE, startLine + i, lines[i]));
            }
            
            // Handle partial line (no trailing newline)
            String lastLine = lines[lines.length - 1];
            if (!lastLine.isEmpty()) {
                nodes.add(new TerminalNode(NodeType.JAVA_PARTIAL, startLine + lines.length - 1, lastLine));
            }
            currentJavaCode.setLength(0);
        }
    }
    
    // Utility methods
    private char currentChar() {
        return isAtEnd() ? '\0' : source.charAt(position);
    }
    
    private char peekChar() {
        return (position + 1 >= source.length()) ? '\0' : source.charAt(position + 1);
    }
    
    private void advance() {
        if (!isAtEnd()) {
            if (currentChar() == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
            position++;
        }
    }
    
    private void advance(int count) {
        for (int i = 0; i < count; i++) {
            advance();
        }
    }
    
    private boolean isAtEnd() {
        return position >= source.length();
    }
    
    private boolean lookAhead(String text) {
        if (position + text.length() > source.length()) {
            return false;
        }
        return source.substring(position, position + text.length()).equals(text);
    }
    
    private void skipWhitespace() {
        while (!isAtEnd() && Character.isWhitespace(currentChar()) && currentChar() != '\n') {
            advance();
        }
    }
    
    private String peekIdentifier() {
        int start = position;
        int pos = position;
        
        if (pos < source.length() && Character.isJavaIdentifierStart(source.charAt(pos))) {
            pos++;
            while (pos < source.length() && Character.isJavaIdentifierPart(source.charAt(pos))) {
                pos++;
            }
        }
        
        return source.substring(start, pos);
    }
    
    private void consumeIdentifier() {
        if (Character.isJavaIdentifierStart(currentChar())) {
            advance();
            while (!isAtEnd() && Character.isJavaIdentifierPart(currentChar())) {
                advance();
            }
        }
    }
    
    private void consumeStringLiteral() {
        advance(); // Skip opening quote
        while (!isAtEnd() && currentChar() != '"') {
            if (currentChar() == '\\') {
                advance(); // Skip escape character
                if (!isAtEnd()) advance(); // Skip escaped character
            } else {
                advance();
            }
        }
        if (!isAtEnd()) advance(); // Skip closing quote
    }
    
    private void consumeCharLiteral() {
        advance(); // Skip opening quote
        while (!isAtEnd() && currentChar() != '\'') {
            if (currentChar() == '\\') {
                advance(); // Skip escape character
                if (!isAtEnd()) advance(); // Skip escaped character
            } else {
                advance();
            }
        }
        if (!isAtEnd()) advance(); // Skip closing quote
    }
    
    private void consumeLineComment() {
        advance(2); // Skip //
        while (!isAtEnd() && currentChar() != '\n') {
            advance();
        }
    }
    
    private void consumeBlockComment() {
        advance(2); // Skip /*
        while (!isAtEnd()) {
            if (currentChar() == '*' && peekChar() == '/') {
                advance(2); // Skip */
                break;
            }
            advance();
        }
    }
    
    private void consumeNumber() {
        while (!isAtEnd() && (Character.isDigit(currentChar()) || currentChar() == '.')) {
            advance();
        }
        // Handle scientific notation
        if (!isAtEnd() && (currentChar() == 'e' || currentChar() == 'E')) {
            advance();
            if (!isAtEnd() && (currentChar() == '+' || currentChar() == '-')) {
                advance();
            }
            while (!isAtEnd() && Character.isDigit(currentChar())) {
                advance();
            }
        }
    }
    
    private void consumeOperator() {
        char c = currentChar();
        advance();
        
        // Handle multi-character operators
        if (!isAtEnd()) {
            char next = currentChar();
            String twoChar = "" + c + next;
            if (Set.of("==", "!=", "<=", ">=", "&&", "||", "++", "--", "+=", "-=", "*=", "/=", "%=", "<<", ">>", ">>>").contains(twoChar)) {
                advance();
            }
        }
    }
    
    private void error(String message) {
        throw new ParseException(String.format("Line %d:%d - %s", line, column, message));
    }
    
    // Exception class
    public static class ParseException extends RuntimeException {
        public ParseException(String message) {
            super(message);
        }
    }
}
