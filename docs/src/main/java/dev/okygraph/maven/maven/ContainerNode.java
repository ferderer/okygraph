package dev.okygraph.maven;

import java.util.List;

/**
 * Container node that holds child nodes
 */
public record ContainerNode(
    NodeType type,
    int lineNumber,
    List<ASTNode> children
) implements ASTNode {}
