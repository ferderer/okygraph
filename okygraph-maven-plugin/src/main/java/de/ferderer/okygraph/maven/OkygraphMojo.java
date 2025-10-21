package de.ferderer.okygraph.maven;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/** Maven plugin for transpiling Okygraph template files (.oky) to Java. */
@Mojo(name = "transpile", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class OkygraphMojo extends AbstractMojo {

    /** For which framework a base class should be generated. */
    public enum Framework { SPRING, QUARKUS, MICRONAUT, STANDALONE }

    /** Extension for template classes. */
    private static final String OKY_EXTENSION = ".oky";
    
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /** Source directory containing .oky template files. */
    @Parameter(defaultValue = "${project.basedir}/src/main/java", property = "okygraph.sourceDirectory")
    private File sourceDirectory;

    /** Output directory for generated .java files. */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources", property = "okygraph.outputDirectory")
    private File outputDirectory;

    /** Framework to generate views for (spring, quarkus, micronaut, standalone). */
    @Parameter(required = true, property = "okygraph.framework")
    private Framework framework;

    /** Package name for the generated base class. */
    @Parameter(required = true, property = "okygraph.baseClassPackage")
    private String baseClassPackage;

    /** Where the generated base class should live. */
    @Parameter(required = true, defaultValue = "${project.basedir}/src/main/java", property = "okygraph.baseClassOutputDirectory")
    private File baseClassOutputDirectory;

    @Override
    public void execute() throws MojoExecutionException {
        if (!sourceDirectory.isDirectory()) return;

        try {
            List<Path> okyFiles = findOkyFiles();
            if (okyFiles.isEmpty()) {
                getLog().info("No .oky files found");
                return;
            }
            for (Path oky : okyFiles) {
               transpileTemplateFile(oky);
            }
            generateBaseClass();
            project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to process template files", e);
        }
    }

    /** Find all templates. */
    private List<Path> findOkyFiles() throws IOException {
        try (Stream<Path> walk = Files.walk(sourceDirectory.toPath())) {
            return walk
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(OKY_EXTENSION))
                .toList();
        }
    }

    private void transpileTemplateFile(Path template) throws IOException {
        String source = Files.readString(template);
        String java = Transpiler.process(Tokenizer.process(source));
        
        Path out = getTemplateOutputPath(template);
        Files.createDirectories(out.getParent());
        Files.writeString(out, java);
    }

    private Path getTemplateOutputPath(Path template) {
        return outputDirectory.toPath()
            .resolve(sourceDirectory.toPath().relativize(template))
            .resolveSibling(template.getFileName().toString().replace(OKY_EXTENSION, ".java"));
    }

    private void generateBaseClass() throws IOException {
        String template = loadTemplate("/base-classes/" + framework.name() + ".template");
        String source = template.replace("${package}", baseClassPackage);

        Path out = baseClassOutputDirectory.toPath()
            .resolve(baseClassPackage.replace('.', '/'))
            .resolve(framework.name() + "OkygraphView.java");

        Files.createDirectories(out.getParent());
        Files.writeString(out, source);
    }

    private String loadTemplate(String path) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null)
                throw new IOException("Base class template not found: " + path);

            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
