package dev.okygraph.maven;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OkygraphMojo.
 * Note: Full integration tests require maven-plugin-testing-harness.
 * These are basic unit tests for now.
 */
class OkygraphMojoTest {

    @Test
    void testDirectorySetup(@TempDir Path tempDir) throws Exception {
        // Create source directory
        Path sourceDir = tempDir.resolve("src/main/java");
        Path packageDir = sourceDir.resolve("com/example");
        Files.createDirectories(packageDir);

        // Create a simple .oky file
        String okyContent = """
            package com.example;

            public class HelloView extends BaseView {
                @Override
                protected void render() throws java.io.IOException {`
                    <h1>Hello World!</h1>
                `}
            }
            """;

        Files.writeString(packageDir.resolve("HelloView.oky"), okyContent);

        // Create output directory
        Path outputDir = tempDir.resolve("target/generated-sources/okygraph");
        Files.createDirectories(outputDir);

        // Verify directories and file exist
        assertTrue(Files.exists(sourceDir));
        assertTrue(Files.exists(outputDir));
        assertTrue(Files.exists(packageDir.resolve("HelloView.oky")));
    }
}