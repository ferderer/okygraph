package dev.okygraph.maven;

/**
 * AST node types and classes for template parsing
 */
public sealed interface ASTNode permits ContainerNode, TerminalNode {
}
