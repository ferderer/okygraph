package dev.okygraph.maven;

/**
 * Terminal node with content
 */
public record TerminalNode(
    NodeType type,
    int lineNumber,
    String content
) implements ASTNode {}
