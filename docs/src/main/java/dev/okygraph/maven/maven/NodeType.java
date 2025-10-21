package dev.okygraph.maven;

/**
 * Node types for AST classification
 */
public enum NodeType {
    // Container types
    TEMPLATE_CLASS,     // The entire template class
    TEMPLATE_METHOD,    // Root template method
    HTML_BLOCK,         // Block of HTML content with expressions
    IF_STATEMENT,       // @if conditional
    FOR_LOOP,           // @for loop
    EACH_LOOP,          // @each loop
    
    // Terminal types
    JAVA_LINE,          // Single line of Java code
    JAVA_PARTIAL,       // Partial line of Java code following by the template code
    JAVA_EXPRESSION,    // {expression} content
    JAVA_CONDITION,     // Condition/parameters for control flow
    HTML_EXPRESSION,    // Static HTML fragment
    ELSE                // @else marker
}
