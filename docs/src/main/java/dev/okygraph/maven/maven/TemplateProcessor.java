package dev.okygraph.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Core template processing logic for Okygraph templates.
 * Converts template method bodies from .jmt files to Java write() method calls.
 */
public class TemplateProcessor {

    // Regex patterns for parsing
    private static final Pattern PACKAGE_PATTERN = Pattern.compile(
        "^\\s*package\\s+([\\w.]+)\\s*;",
        Pattern.MULTILINE
    );

    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile(
        "(?:public\\s+)?class\\s+(\\w+)"
    );

    private static final Pattern TEMPLATE_METHOD_PATTERN = Pattern.compile(
        "\\{`([^`]*(?:`[^}][^`]*)*)`\\}",
        Pattern.DOTALL
    );

    // Control flow patterns - made more flexible
    private static final Pattern IF_PATTERN = Pattern.compile(
        "^\\s*@if\\s*\\(([^)]+)\\)\\s*$"
    );

    private static final Pattern ELSE_PATTERN = Pattern.compile(
        "^\\s*@else\\s*$"
    );

    private static final Pattern EACH_PATTERN = Pattern.compile(
        "^\\s*@each\\s*\\(([^)]+)\\)\\s*$"
    );

    private static final Pattern FOR_PATTERN = Pattern.compile(
        "^\\s*@for\\s*\\(([^)]+)\\)\\s*$"
    );

    private static final Pattern END_PATTERN = Pattern.compile(
        "^\\s*@end\\s*$"
    );

    // Expression pattern - supports escape functions
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile(
        "\\{([^{}]+)\\}"
    );

    // Escape function patterns
    private static final Pattern ESCAPE_FUNCTION_PATTERN = Pattern.compile(
        "^(escHtml|escAttr|escJs|raw)\\s*\\((.+)\\)$"
    );

    /**
     * Process a .jmt template file and generate Java code
     */
    public Path processFile(Path sourcePath, Path outputBaseDir) throws IOException {
        String sourceContent = Files.readString(sourcePath);

        // Extract package and class information
        String packageName = extractPackage(sourceContent);
        String className = extractClassName(sourceContent);

        if (className == null) {
            throw new IOException("Could not extract class name from: " + sourcePath);
        }

        // Determine output path based on package structure
        Path outputPath = buildOutputPath(outputBaseDir, packageName, className);

        // Create output directories if needed
        Files.createDirectories(outputPath.getParent());

        // Process template content
        String processedContent = processTemplateContent(sourceContent, sourcePath);

        // Write generated Java file
        Files.writeString(outputPath, processedContent,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        return outputPath;
    }

    /**
     * Extract package declaration from Java source
     */
    private String extractPackage(String sourceContent) {
        Matcher matcher = PACKAGE_PATTERN.matcher(sourceContent);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return ""; // Default package
    }

    /**
     * Extract class name from Java source
     */
    private String extractClassName(String sourceContent) {
        Matcher matcher = CLASS_NAME_PATTERN.matcher(sourceContent);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Build output path based on package structure
     */
    private Path buildOutputPath(Path outputBaseDir, String packageName, String className) {
        Path packagePath = outputBaseDir;

        if (!packageName.isEmpty()) {
            // Convert package to directory structure
            String[] packageParts = packageName.split("\\.");
            for (String part : packageParts) {
                packagePath = packagePath.resolve(part);
            }
        }

        return packagePath.resolve(className + ".java");
    }

    /**
     * Process template content - replace template method bodies with Java code
     */
    private String processTemplateContent(String sourceContent, Path sourcePath) throws IOException {
        Matcher matcher = TEMPLATE_METHOD_PATTERN.matcher(sourceContent);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String templateBody = matcher.group(1);
            String processedBody = processTemplateMethodBody(templateBody, sourcePath);

            // Replace {` template content `} with { processed Java code }
            matcher.appendReplacement(result,
                Matcher.quoteReplacement("{\n" + processedBody + "\n    }"));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Process a single template method body
     */
    private String processTemplateMethodBody(String templateBody, Path sourcePath) throws IOException {
        StringBuilder result = new StringBuilder();

        // Split into lines for processing
        String[] lines = templateBody.split("\\n");

        // Track current state for control flow
        ProcessingState state = new ProcessingState();

        for (int i = 0; i < lines.length; i++) {
            int lineNumber = i + 1; // Line numbers start from 1
            String line = lines[i];

            processLine(line, lineNumber, state, result);
        }

        // Close any remaining open blocks
        while (state.blockDepth > 0) {
            result.append("\n        }");
            state.blockDepth--;
        }

        return result.toString();
    }

    /**
     * Process a single line of template content
     */
    private void processLine(String line, int lineNumber, ProcessingState state, StringBuilder result) {
        String trimmedLine = line.trim();

        // Handle control flow directives
        if (handleControlFlow(trimmedLine, lineNumber, state, result)) {
            return;
        }

        // Handle regular content with expressions
        if (!trimmedLine.isEmpty()) {
            processContentLine(line, lineNumber, result);
        }
    }

    /**
     * Handle control flow directives (@if, @else, @each, @for, @end)
     */
    private boolean handleControlFlow(String line, int lineNumber, ProcessingState state, StringBuilder result) {
        // @if directive
        Matcher ifMatcher = IF_PATTERN.matcher(line);
        if (ifMatcher.find()) {
            String condition = ifMatcher.group(1).trim();
            result.append(String.format("\n        setLine(%d); if (%s) {", lineNumber, condition));
            state.blockDepth++;
            return true;
        }

        // @else directive
        if (ELSE_PATTERN.matcher(line).find()) {
            result.append(String.format("\n        setLine(%d); } else {", lineNumber));
            return true;
        }

        // @each directive (enhanced for loop)
        Matcher eachMatcher = EACH_PATTERN.matcher(line);
        if (eachMatcher.find()) {
            String forStatement = eachMatcher.group(1).trim();
            result.append(String.format("\n        setLine(%d); for (%s) {", lineNumber, forStatement));
            state.blockDepth++;
            return true;
        }

        // @for directive (traditional for loop)
        Matcher forMatcher = FOR_PATTERN.matcher(line);
        if (forMatcher.find()) {
            String forStatement = forMatcher.group(1).trim();
            result.append(String.format("\n        setLine(%d); for (%s) {", lineNumber, forStatement));
            state.blockDepth++;
            return true;
        }

        // @end directive
        if (END_PATTERN.matcher(line).find()) {
            if (state.blockDepth > 0) {
                result.append(String.format("\n        setLine(%d); }", lineNumber));
                state.blockDepth--;
            }
            return true;
        }

        return false;
    }

    /**
     * Process a content line with potential expressions
     */
    private void processContentLine(String line, int lineNumber, StringBuilder result) {
        // Find expressions in the line
        Matcher matcher = EXPRESSION_PATTERN.matcher(line);

        if (!matcher.find()) {
            // No expressions - simple static content
            String escapedLine = escapeJavaString(line);
            result.append(String.format("\n        setLine(%d); write(\"%s\");", lineNumber, escapedLine));
            return;
        }

        // Line contains expressions - need to split and process
        matcher.reset();
        int lastEnd = 0;

        while (matcher.find()) {
            // Add static content before expression
            if (matcher.start() > lastEnd) {
                String staticPart = line.substring(lastEnd, matcher.start());
                if (!staticPart.isEmpty()) {
                    String escapedPart = escapeJavaString(staticPart);
                    result.append(String.format("\n        setLine(%d); write(\"%s\");", lineNumber, escapedPart));
                }
            }

            // Process expression
            String expression = matcher.group(1).trim();
            processExpression(expression, lineNumber, result);

            lastEnd = matcher.end();
        }

        // Add remaining static content
        if (lastEnd < line.length()) {
            String remainingPart = line.substring(lastEnd);
            if (!remainingPart.isEmpty()) {
                String escapedPart = escapeJavaString(remainingPart);
                result.append(String.format("\n        setLine(%d); write(\"%s\");", lineNumber, escapedPart));
            }
        }
    }

    /**
     * Process a single expression - handle escape functions or auto-escape
     */
    private void processExpression(String expression, int lineNumber, StringBuilder result) {
        Matcher escapeMatcher = ESCAPE_FUNCTION_PATTERN.matcher(expression);
        
        if (escapeMatcher.find()) {
            // Expression uses explicit escape function
            String escapeFunction = escapeMatcher.group(1);
            String innerExpression = escapeMatcher.group(2);
            
            if ("raw".equals(escapeFunction)) {
                // Raw output - no escaping
                result.append(String.format("\n        setLine(%d); write(%s);", lineNumber, innerExpression));
            } else {
                // Use the escape function
                result.append(String.format("\n        setLine(%d); write(%s(%s));", 
                    lineNumber, escapeFunction, innerExpression));
            }
        } else {
            // Auto-escape with writeEsc
            result.append(String.format("\n        setLine(%d); writeEsc(%s);", lineNumber, expression));
        }
    }

    /**
     * Escape string for Java string literals
     */
    private String escapeJavaString(String input) {
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    /**
     * State tracking for template processing
     */
    private static class ProcessingState {
        int blockDepth = 0;
    }
}
