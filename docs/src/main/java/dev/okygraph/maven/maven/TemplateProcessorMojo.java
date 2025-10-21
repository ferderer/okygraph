package dev.okygraph.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

/**
 * Maven Mojo for processing template method bodies in Java files.
 * Transpiles template syntax to Java w() method calls.
 */
@Mojo(
    name = "process",
    defaultPhase = LifecyclePhase.PROCESS_SOURCES,
    requiresDependencyResolution = ResolutionScope.COMPILE
)
public class TemplateProcessorMojo extends AbstractMojo {

    /**
     * The Maven project
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    /**
     * Source directory to scan for Java files with template methods
     */
    @Parameter(
        property = "template.sourceDirectory",
        defaultValue = "${project.build.sourceDirectory}"
    )
    File sourceDirectory;

    /**
     * Additional source directories to process (useful for test sources)
     */
    @Parameter(property = "template.additionalSourceDirectories")
    List<File> additionalSourceDirectories;

    /**
     * File extension for template files (if processing separate template files)
     */
    @Parameter(property = "template.extension", defaultValue = "jte")
    String templateExtension;

    /**
     * Whether to process template method bodies in Java files
     */
    @Parameter(property = "template.processMethodBodies", defaultValue = "true")
    boolean processMethodBodies;

    /**
     * Whether to process separate template files
     */
    @Parameter(property = "template.processTemplateFiles", defaultValue = "false")
    boolean processTemplateFiles;

    /**
     * Output directory for processed files (if different from source)
     */
    @Parameter(property = "template.outputDirectory")
    File outputDirectory;

    /**
     * Whether to backup original files before processing
     */
    @Parameter(property = "template.backupOriginals", defaultValue = "true")
    boolean backupOriginals;

    /**
     * File patterns to include (glob patterns)
     */
    @Parameter(property = "template.includes")
    List<String> includes;

    /**
     * File patterns to exclude (glob patterns)
     */
    @Parameter(property = "template.excludes")
    List<String> excludes;

    /**
     * Whether to fail build on template processing errors
     */
    @Parameter(property = "template.failOnError", defaultValue = "true")
    boolean failOnError;

    /**
     * Verbose logging
     */
    @Parameter(property = "template.verbose", defaultValue = "false")
    boolean verbose;

    // Regex pattern for finding template method bodies
    private static final Pattern TEMPLATE_METHOD_PATTERN = Pattern.compile(
        "\\{`([^`]*(?:`[^}][^`]*)*)`\\}",
        Pattern.DOTALL
    );

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Starting template processing...");
        logConfiguration();

        validateConfiguration();

        int processedFiles = 0;
        int processedMethods = 0;

        try {
            // Process main source directory
            if (sourceDirectory != null && sourceDirectory.exists()) {
                ProcessingResult result = processDirectory(sourceDirectory);
                processedFiles += result.filesProcessed;
                processedMethods += result.methodsProcessed;
            }

            // Process additional source directories
            if (additionalSourceDirectories != null) {
                for (File additionalDir : additionalSourceDirectories) {
                    if (additionalDir != null && additionalDir.exists()) {
                        ProcessingResult result = processDirectory(additionalDir);
                        processedFiles += result.filesProcessed;
                        processedMethods += result.methodsProcessed;
                    }
                }
            }

            getLog().info(String.format(
                "Template processing completed successfully. Processed %d methods in %d files.",
                processedMethods, processedFiles
            ));

        } catch (IOException e) {
            String message = "Error processing template files: " + e.getMessage();
            if (failOnError) {
                throw new MojoExecutionException(message, e);
            } else {
                getLog().error(message, e);
            }
        }
    }

    private void logConfiguration() {
        if (verbose) {
            getLog().info("Configuration:");
            getLog().info("  Source Directory: " + sourceDirectory);
            getLog().info("  Template Extension: " + templateExtension);
            getLog().info("  Process Method Bodies: " + processMethodBodies);
            getLog().info("  Process Template Files: " + processTemplateFiles);
            getLog().info("  Output Directory: " + outputDirectory);
            getLog().info("  Backup Originals: " + backupOriginals);
            getLog().info("  Fail On Error: " + failOnError);
        }
    }

    private void validateConfiguration() throws MojoExecutionException {
        if (sourceDirectory == null || !sourceDirectory.exists()) {
            throw new MojoExecutionException(
                "Source directory does not exist: " + sourceDirectory
            );
        }

        if (!sourceDirectory.isDirectory()) {
            throw new MojoExecutionException(
                "Source directory is not a directory: " + sourceDirectory
            );
        }

        if (outputDirectory != null && !outputDirectory.exists()) {
            if (!outputDirectory.mkdirs()) {
                throw new MojoExecutionException(
                    "Failed to create output directory: " + outputDirectory
                );
            }
        }
    }

    private ProcessingResult processDirectory(File directory) throws IOException {
        getLog().info("Processing directory: " + directory.getAbsolutePath());

        ProcessingResult result = new ProcessingResult();
        Path directoryPath = directory.toPath();

        try (Stream<Path> paths = Files.walk(directoryPath)) {
            List<Path> filesToProcess = paths
                .filter(this::shouldProcessFile)
                .toList();

            for (Path filePath : filesToProcess) {
                try {
                    int methodsProcessed = processFile(filePath);
                    if (methodsProcessed > 0) {
                        result.filesProcessed++;
                        result.methodsProcessed += methodsProcessed;
                        
                        if (verbose) {
                            getLog().info(String.format(
                                "Processed %d template methods in: %s",
                                methodsProcessed, filePath
                            ));
                        }
                    }
                } catch (Exception e) {
                    String message = "Error processing file: " + filePath + " - " + e.getMessage();
                    if (failOnError) {
                        throw new IOException(message, e);
                    } else {
                        getLog().error(message, e);
                    }
                }
            }
        }

        return result;
    }

    private boolean shouldProcessFile(Path path) {
        if (!Files.isRegularFile(path)) {
            return false;
        }

        String fileName = path.getFileName().toString();

        // Check file extension
        if (processMethodBodies && fileName.endsWith(".java")) {
            return matchesIncludeExcludePatterns(path);
        }

        if (processTemplateFiles && fileName.endsWith("." + templateExtension)) {
            return matchesIncludeExcludePatterns(path);
        }

        return false;
    }

    private boolean matchesIncludeExcludePatterns(Path path) {
        String pathString = path.toString();

        // Check exclude patterns first
        if (excludes != null) {
            for (String excludePattern : excludes) {
                if (pathString.matches(excludePattern.replace("*", ".*"))) {
                    return false;
                }
            }
        }

        // Check include patterns
        if (includes != null && !includes.isEmpty()) {
            for (String includePattern : includes) {
                if (pathString.matches(includePattern.replace("*", ".*"))) {
                    return true;
                }
            }
            return false; // No include pattern matched
        }

        return true; // No include patterns specified, so include by default
    }

    private int processFile(Path filePath) throws IOException {
        getLog().debug("Processing template file: " + filePath);
        
        try {
            // Use TemplateProcessor to handle the entire file processing
            TemplateProcessor processor = new TemplateProcessor();
            
            // Determine output base directory
            Path outputBaseDir = getOutputBaseDirectory();
            
            // Backup original file if requested
            if (backupOriginals) {
                Path backupPath = filePath.resolveSibling(filePath.getFileName() + ".backup");
                Files.copy(filePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
                
                if (verbose) {
                    getLog().debug("Created backup: " + backupPath);
                }
            }
            
            // Process the file and get the output path
            Path outputPath = processor.processFile(filePath, outputBaseDir);
            
            if (verbose) {
                getLog().info("Generated: " + outputPath);
            }
            
            // Return 1 to indicate one file was processed successfully
            return 1;
            
        } catch (Exception e) {
            String message = "Error processing template file " + filePath + ": " + e.getMessage();
            getLog().error(message, e);
            
            if (failOnError) {
                throw new IOException(message, e);
            }
            
            // Return 0 if processing failed and failOnError is false
            return 0;
        }
    }

    private Path getOutputBaseDirectory() {
        if (outputDirectory != null) {
            return outputDirectory.toPath();
        } else {
            // Default to target/generated-sources/templates
            return project.getBasedir().toPath()
                .resolve("target")
                .resolve("generated-sources")
                .resolve("templates");
        }
    }

    /**
     * Result holder for processing statistics
     */
    private static class ProcessingResult {
        int filesProcessed = 0;
        int methodsProcessed = 0;
    }
}
