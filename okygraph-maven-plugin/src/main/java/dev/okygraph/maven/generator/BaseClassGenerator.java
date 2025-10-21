package dev.okygraph.maven.generator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Generates framework-specific base view classes.
 * Loads templates from resources and fills in package/class names.
 */
public class BaseClassGenerator {

    /**
     * Generates base class source code based on framework.
     *
     * @param framework The target framework (spring, quarkus, micronaut, standalone)
     * @param packageName The package for the generated class
     * @param className The name of the generated class
     * @param basePackage The package containing OkygraphView
     * @return The generated Java source code
     * @throws IOException if template loading fails
     */
    public String generateBaseClass(String framework, String packageName, String className, String basePackage) throws IOException {
        // Load template from resources
        String templateName = getTemplateName(framework);
        String template = loadTemplate(templateName);

        // Replace placeholders
        return template
            .replace("${packageName}", packageName)
            .replace("${className}", className)
            .replace("${basePackage}", basePackage);
    }

    private String getTemplateName(String framework) {
        return switch (framework.toLowerCase()) {
            case "spring" -> "/base-classes/SpringOkygraphView.java.template";
            case "quarkus" -> "/base-classes/QuarkusOkygraphView.java.template";
            case "micronaut" -> "/base-classes/MicronautOkygraphView.java.template";
            case "standalone" -> "/base-classes/StandaloneOkygraphView.java.template";
            default -> throw new IllegalArgumentException("Unknown framework: " + framework);
        };
    }

    private String loadTemplate(String resourcePath) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Template not found: " + resourcePath);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
