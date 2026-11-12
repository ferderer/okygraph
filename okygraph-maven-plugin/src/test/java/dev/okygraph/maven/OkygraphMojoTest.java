package dev.okygraph.maven;

import dev.okygraph.maven.OkygraphMojo.Framework;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class OkygraphMojoTest {

    @TempDir
    Path dir;

    private OkygraphMojo mojo(Framework framework) throws Exception {
        OkygraphMojo mojo = new OkygraphMojo();
        set(mojo, "project", new MavenProject());
        set(mojo, "sourceDirectory", dir.resolve("src").toFile());
        set(mojo, "outputDirectory", dir.resolve("gen").toFile());
        set(mojo, "framework", framework);
        set(mojo, "baseClassPackage", "com.example.views");
        set(mojo, "baseClassOutputDirectory", dir.resolve("src").toFile());
        return mojo;
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field f = OkygraphMojo.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private void writeOky(String relativePath, String content) throws Exception {
        Path p = dir.resolve("src").resolve(relativePath);
        Files.createDirectories(p.getParent());
        Files.writeString(p, content);
    }

    private Path baseClass() {
        return dir.resolve("src/com/example/views/OkyTemplate.java");
    }

    @Test
    void generatesSpringBaseClassIntoPackage() throws Exception {
        writeOky("com/example/HomeView.oky", "class HomeView {}\n");
        mojo(Framework.SPRING).execute();

        String source = Files.readString(baseClass());
        assertTrue(source.startsWith("package com.example.views;"), source.lines().findFirst().orElse(""));
        assertTrue(source.contains("public abstract class OkyTemplate implements View"));
        assertFalse(source.contains("${package}"));
        assertTrue(source.contains("protected OkyTemplate attr(String s)"), "attribute escaping helper (ADR-0121, ADR-0124)");
        assertFalse(source.contains("record Raw"), "raw() and attr() write directly, there is no Raw type (ADR-0124)");
        assertFalse(source.contains("new PrintWriter(res.getWriter()"), "the root writer is the response writer, not wrapped (ADR-0001, ADR-0125)");
        assertFalse(source.contains("org.springframework.security"), "base class must only need Spring MVC (ADR-0117)");
        assertFalse(source.contains("LocaleContextHolder"), "no formatters in the base class (ADR-0123)");
    }

    @Test
    void generatesStandaloneBaseClassWithoutAnyFrameworkType() throws Exception {
        writeOky("com/example/HomeView.oky", "class HomeView {}\n");
        mojo(Framework.STANDALONE).execute();

        String source = Files.readString(baseClass());
        assertTrue(source.startsWith("package com.example.views;"), source.lines().findFirst().orElse(""));
        assertTrue(source.contains("public abstract class OkyTemplate {"));
        assertTrue(source.contains("public void render(Writer writer)"));
        assertTrue(source.contains("protected OkyTemplate attr(String s)"));
        assertFalse(source.contains("record Raw"));
        assertFalse(source.contains("${package}"));
        assertFalse(source.matches("(?s).*import (org|jakarta|javax)\\..*"), "only java.* imports");
    }

    @Test
    void keepsExistingBaseClass() throws Exception {
        writeOky("com/example/HomeView.oky", "class HomeView {}\n");
        Files.createDirectories(baseClass().getParent());
        Files.writeString(baseClass(), "// adapted by the developer\n");

        mojo(Framework.SPRING).execute();

        assertEquals("// adapted by the developer\n", Files.readString(baseClass()));
    }

    @Test
    void failsClearlyForFrameworkWithoutTemplate() throws Exception {
        writeOky("com/example/HomeView.oky", "class HomeView {}\n");
        MojoExecutionException e = assertThrows(MojoExecutionException.class, () -> mojo(Framework.QUARKUS).execute());
        assertTrue(e.getMessage().contains("QUARKUS"), e.getMessage());
    }

    @Test
    void retranspilesEvenWhenOutputIsNewer() throws Exception {
        writeOky("com/example/HomeView.oky", "class HomeView {}\n");
        Path out = dir.resolve("gen/com/example/HomeView.java");
        Files.createDirectories(out.getParent());
        Files.writeString(out, "stale");
        Files.setLastModifiedTime(out, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis() + 60_000));

        mojo(Framework.SPRING).execute();

        assertEquals("class HomeView {}\n", Files.readString(out));
    }

    @Test
    void registersOutputDirectoriesAsSourceRoots() throws Exception {
        writeOky("com/example/HomeView.oky", "class HomeView {}\n");
        OkygraphMojo mojo = mojo(Framework.SPRING);
        mojo.execute();

        Field f = OkygraphMojo.class.getDeclaredField("project");
        f.setAccessible(true);
        MavenProject project = (MavenProject) f.get(mojo);
        assertTrue(project.getCompileSourceRoots().contains(new File(dir.resolve("gen").toString()).getAbsolutePath()));
        assertTrue(project.getCompileSourceRoots().contains(new File(dir.resolve("src").toString()).getAbsolutePath()));
        assertTrue(Files.exists(dir.resolve("gen/com/example/HomeView.java")));
    }
}
