package dev.okygraph.maven;

import dev.okygraph.maven.generator.BaseClassGenerator;
import dev.okygraph.maven.tokenizer.Token;
import dev.okygraph.maven.tokenizer.Tokenizer;
import dev.okygraph.maven.tokenizer.UnicodePreprocessor;
import dev.okygraph.maven.transpiler.TemplateTranspiler;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Maven plugin for transpiling Okygraph template files (.oky) to Java.
 *
 * This plugin scans for .oky files in the source directory and transpiles
 * them to .java files in the generated sources directory.
 *
 * Configuration example:
 * <pre>
 * &lt;plugin&gt;
 *   &lt;groupId&gt;dev.okygraph&lt;/groupId&gt;
 *   &lt;artifactId&gt;okygraph-maven-plugin&lt;/artifactId&gt;
 *   &lt;configuration&gt;
 *     &lt;framework&gt;spring&lt;/framework&gt;
 *   &lt;/configuration&gt;
 * &lt;/plugin&gt;
 * </pre>
 *
 * @author Okygraph Team
 */
@Mojo(name = "transpile", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class OkygraphMojo extends AbstractMojo {

    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Source directory containing .oky template files.
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/java", property = "okygraph.sourceDirectory")
    private File sourceDirectory;

    /**
     * Output directory for generated .java files.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/okygraph", property = "okygraph.outputDirectory")
    private File outputDirectory;

    /**
     * Framework to generate views for (spring, quarkus, micronaut, standalone).
     * Determines which base class to generate.
     * Default: standalone
     */
    @Parameter(defaultValue = "standalone", property = "okygraph.framework")
    private String framework;

    /**
     * Package name for the generated base class.
     * Default: views
     */
    @Parameter(defaultValue = "views", property = "okygraph.baseClassPackage")
    private String baseClassPackage;

    /**
     * Name of the generated base class.
     * Default: OkygraphView
     */
    @Parameter(defaultValue = "OkygraphView", property = "okygraph.baseClassName")
    private String baseClassName;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Starting Okygraph template transpilation...");
        getLog().info("Source directory: " + sourceDirectory.getAbsolutePath());
        getLog().info("Output directory: " + outputDirectory.getAbsolutePath());
        getLog().info("Framework: " + framework);

        // Validate source directory
        if (!sourceDirectory.exists() || !sourceDirectory.isDirectory()) {
            getLog().warn("Source directory does not exist: " + sourceDirectory.getAbsolutePath());
            return;
        }

        // Create output directory if it doesn't exist
        if (!outputDirectory.exists()) {
            if (!outputDirectory.mkdirs()) {
                throw new MojoExecutionException("Failed to create output directory: " + outputDirectory.getAbsolutePath());
            }
        }

        try {
            // Find all .oky files
            List<Path> okyFiles = findOkyFiles(sourceDirectory.toPath());
            getLog().info("Found " + okyFiles.size() + " .oky file(s)");

            if (okyFiles.isEmpty()) {
                getLog().info("No .oky files found. Nothing to transpile.");
                return;
            }

            // Generate base class first
            generateBaseClass();

            // Transpile each file
            int successCount = 0;
            int errorCount = 0;

            for (Path okyFile : okyFiles) {
                try {
                    getLog().info("Processing: " + sourceDirectory.toPath().relativize(okyFile));
                    transpileFile(okyFile);
                    successCount++;
                } catch (Exception e) {
                    errorCount++;
                    getLog().error("Failed to transpile " + okyFile + ": " + e.getMessage(), e);
                }
            }

            // Add generated sources to Maven project
            project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
            getLog().info("Added generated sources to compile path");

            getLog().info("Okygraph transpilation completed: " + successCount + " succeeded, " + errorCount + " failed");

            if (errorCount > 0) {
                throw new MojoFailureException("Failed to transpile " + errorCount + " file(s)");
            }

        } catch (IOException e) {
            throw new MojoExecutionException("Failed to process template files", e);
        }
    }

    /**
     * Finds all .oky files recursively in the given directory.
     */
    private List<Path> findOkyFiles(Path directory) throws IOException {
        try (Stream<Path> walk = Files.walk(directory)) {
            return walk
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".oky"))
                .toList();
        }
    }

    /**
     * Transpiles a single .oky file to .java.
     */
    private void transpileFile(Path okyFile) throws IOException {
        // Read source file
        String source = Files.readString(okyFile, StandardCharsets.UTF_8);

        // Preprocess Unicode escapes
        String processed = UnicodePreprocessor.process(source);

        // Tokenize
        Tokenizer tokenizer = new Tokenizer(processed);
        List<Token> tokens = tokenizer.tokenize();

        // Transpile
        TemplateTranspiler transpiler = new TemplateTranspiler();
        String javaCode = transpiler.transpile(tokens);

        // Calculate output path (preserve package structure)
        Path relativePath = sourceDirectory.toPath().relativize(okyFile);
        String outputFileName = okyFile.getFileName().toString().replace(".oky", ".java");
        Path outputPath = outputDirectory.toPath()
            .resolve(relativePath.getParent() != null ? relativePath.getParent() : Path.of(""))
            .resolve(outputFileName);

        // Create parent directories
        Files.createDirectories(outputPath.getParent());

        // Write output file
        Files.writeString(outputPath, javaCode, StandardCharsets.UTF_8);

        getLog().debug("Generated: " + outputPath);
    }

    /**
     * Generates the base class for the selected framework.
     */
    private void generateBaseClass() throws IOException {
        getLog().info("Generating base class: " + baseClassPackage + "." + baseClassName);
        getLog().info("Framework: " + framework);

        // Base package is where OkygraphView lives
        String basePackage = "dev.okygraph.maven.runtime";

        // Generate base class source code
        BaseClassGenerator generator = new BaseClassGenerator();
        String sourceCode = generator.generateBaseClass(framework, baseClassPackage, baseClassName, basePackage);

        // Calculate output path
        String packagePath = baseClassPackage.replace('.', '/');
        Path baseClassPath = outputDirectory.toPath()
            .resolve(packagePath)
            .resolve(baseClassName + ".java");

        // Create directories
        Files.createDirectories(baseClassPath.getParent());

        // Write file
        Files.writeString(baseClassPath, sourceCode, StandardCharsets.UTF_8);

        getLog().info("Generated: " + baseClassPath);
    }
}
