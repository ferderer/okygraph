package dev.okygraph.maven;

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

    /** Simple name of the generated base class; every framework template defines this class. */
    private static final String BASE_CLASS_NAME = "OkyTemplate";

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
            List<Path> okyFiles = findAllOkyTemplates();
            if (okyFiles.isEmpty()) {
                getLog().info("No .oky files found");
                return;
            }
            long start = System.currentTimeMillis();
            int count = 0;
            for (Path oky : okyFiles) {
                Path out = outputDirectory.toPath()
                    .resolve(sourceDirectory.toPath().relativize(oky))
                    .resolveSibling(oky.getFileName().toString().replace(OKY_EXTENSION, ".java"));

                if (!Files.exists(out) ||
                    Files.getLastModifiedTime(oky).compareTo(Files.getLastModifiedTime(out)) > 0) {
                        Files.createDirectories(out.getParent());
                        Files.writeString(out, Transpiler.process(Files.readString(oky)));
                        count++;
                }
            }
            generateBaseClass();
            long ms = System.currentTimeMillis() - start;
            if (count > 0) {
                getLog().info("Transpiled " + count + " of " + okyFiles.size() + " .oky files in " + ms + " ms");
            } else {
                getLog().info("" + okyFiles.size() + " .oky files up to date");
            }
            project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to process template files", e);
        }
    }

    private List<Path> findAllOkyTemplates() throws IOException {
        try (Stream<Path> walk = Files.walk(sourceDirectory.toPath())) {
            return walk
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(OKY_EXTENSION))
                .toList();
        }
    }

    /**
     * Writes the framework's base class into the configured package, but only if it does not exist yet:
     * it lives in the user's source tree by default and may be adapted there. Delete it to regenerate.
     */
    private void generateBaseClass() throws IOException, MojoExecutionException {
        Path out = baseClassOutputDirectory.toPath()
            .resolve(baseClassPackage.replace('.', '/'))
            .resolve(BASE_CLASS_NAME + ".java");
        project.addCompileSourceRoot(baseClassOutputDirectory.getAbsolutePath());
        if (Files.exists(out)) {
            getLog().debug("Keeping existing base class " + out);
            return;
        }
        String template = loadTemplate("/base-classes/" + framework.name() + ".template");
        Files.createDirectories(out.getParent());
        Files.writeString(out, template.replace("${package}", baseClassPackage));
        getLog().info("Generated base class " + baseClassPackage + "." + BASE_CLASS_NAME);
    }

    private String loadTemplate(String path) throws IOException, MojoExecutionException {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null)
                throw new MojoExecutionException("No base class template for framework " + framework + " yet (" + path + ")");
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
