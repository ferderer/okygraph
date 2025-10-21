package dev.okygraph.maven.transpiler;

import dev.okygraph.maven.tokenizer.Token;
import dev.okygraph.maven.tokenizer.Tokenizer;
import dev.okygraph.maven.tokenizer.UnicodePreprocessor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TemplateTranspiler.
 */
class TemplateTranspilerTest {

    private String transpile(String template) {
        String processed = UnicodePreprocessor.process(template);

        Tokenizer tokenizer = new Tokenizer(processed);
        List<Token> tokens = tokenizer.tokenize();

        TemplateTranspiler transpiler = new TemplateTranspiler();
        return transpiler.transpile(tokens);
    }

    @Test
    void testSimpleJavaPassthrough() {
        String template = """
            package com.example;

            public class Test {
                private String name;
            }
            """;

        String result = transpile(template);

        // Java code should pass through unchanged
        assertTrue(result.contains("package com.example;"));
        assertTrue(result.contains("public class Test"));
        assertTrue(result.contains("private String name;"));
    }

    @Test
    void testSimpleTemplateMode() {
        String template = """
            protected void render() throws IOException {`
                <h1>Hello World</h1>
            `}
            """;

        String result = transpile(template);

        System.out.println("=== Simple Template Result ===");
        System.out.println(result);

        // Should generate writeRaw() call
        assertTrue(result.contains("writeRaw"));
        assertTrue(result.contains("Hello World"));
    }

    @Test
    void testExpressionInTemplate() {
        String template = """
            protected void render() throws IOException {`
                <h1>{user.name}</h1>
            `}
            """;

        String result = transpile(template);

        System.out.println("=== Expression Template Result ===");
        System.out.println(result);

        // Should have write() call for expression
        assertTrue(result.contains("write(user.name)"));
    }

    @Test
    void testBacktickToggling() {
        String template = """
            protected void render() throws IOException {`
                <div>
            `if (user.isAdmin()) {`
                <span>Admin</span>
            `}`
            `}
            """;

        String result = transpile(template);

        System.out.println("=== Backtick Toggling Result ===");
        System.out.println(result);

        // Should have Java if statement
        assertTrue(result.contains("if (user.isAdmin())"));

        // Should have writeRaw for HTML
        assertTrue(result.contains("writeRaw"));
        assertTrue(result.contains("Admin"));
    }

    @Test
    void testMixedContent() {
        String template = """
            package com.example.views;

            import lombok.Setter;

            public class UserView extends BaseView {
                @Setter
                private User user;

                @Override
                protected void render() throws IOException {`
                    <div class="profile">
                        <h1>{user.name}</h1>
                        <p>{user.email}</p>
                    </div>
                `}
            }
            """;

        String result = transpile(template);

        System.out.println("=== Mixed Content Result ===");
        System.out.println(result);

        // Java parts should be preserved
        assertTrue(result.contains("package com.example.views;"));
        assertTrue(result.contains("import lombok.Setter;"));
        assertTrue(result.contains("public class UserView extends BaseView"));
        assertTrue(result.contains("@Setter"));
        assertTrue(result.contains("private User user;"));

        // Template parts should be transpiled
        assertTrue(result.contains("write(user.name)"));
        assertTrue(result.contains("write(user.email)"));
        assertTrue(result.contains("writeRaw"));
    }
}
