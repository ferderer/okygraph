package dev.okygraph.maven;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/** Transpiles a template, compiles and runs the result, and compares the HTML it prints. */
class EscapeRoundTripTest {

    @TempDir
    Path dir;

    private String render(String body) throws Exception {
        String java = Transpiler.process("""
            public class T {
                StringBuilder out = new StringBuilder();
                T html(String s) { out.append(s); return this; }
                T write(String s) { return html(s); }
                public String run() { render(); return out.toString(); }
                void render() {`
            """ + body + """
                `}
            }
            """);
        Path file = dir.resolve("T.java");
        Files.writeString(file, java);
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, "-Xlint:text-blocks", "-Werror", "-d", dir.toString(), file.toString()),
            "generated code does not compile:\n" + java);
        try (URLClassLoader loader = new URLClassLoader(new URL[] {dir.toUri().toURL()})) {
            Object t = loader.loadClass("T").getDeclaredConstructor().newInstance();
            return (String) t.getClass().getMethod("run").invoke(t);
        }
    }

    @Test
    void backslashesInHtmlBlockStayLiteral() throws Exception {
        assertEquals("<input pattern=\"\\d+\">\n<p>C:\\dir\\temp \\\\</p>\n", render("""
                    <input pattern="\\d+">
                    <p>C:\\dir\\temp \\\\</p>
            """));
    }

    @Test
    void escapedBacktickAndBraceInSingleLine() throws Exception {
        assertEquals("<p>a`b {c} \\d</p>\n", render("""
                    <p>a\\`b \\{c} \\d</p>
            """));
    }

    @Test
    void escapedBacktickAndBraceInHtmlBlock() throws Exception {
        assertEquals("<p>a`b</p>\n<p>{c} \\\\`</p>\n", render("""
                    <p>a\\`b</p>
                    <p>\\{c} \\\\\\`</p>
            """));
    }

    @Test
    void escapedBacktickInFragment() throws Exception {
        assertEquals("<p>a`b x \\d</p>\n", render("""
                    <p>a\\`b {"x"} \\d</p>
            """));
    }

    @Test
    void aSpaceBetweenTwoExpressionsIsKept() throws Exception {
        assertEquals("<p>x y</p>\n<p>a  b</p>\n", render("""
                    <p>{"x"} {"y"}</p>
                    <p>{"a"}  {"b"}</p>
            """));
    }

    @Test
    void whitespaceAfterTheLastExpressionOfALineIsNotOutput() throws Exception {
        assertEquals("<p>x\n", render("            <p>{\"x\"}   \n"));
    }

    @Test
    void trailingBackslashIsLiteral() throws Exception {
        assertEquals("<p>x\\\n<p>y\\\n", render("""
                    <p>x\\
                    <p>y\\
            """));
    }

    @Test
    void dedentedHtmlBlockCompilesWithoutWarnings() throws Exception {
        assertEquals("  <link>\n</head>\n<body>\n", render("""
                    <link>
                  </head>
                  <body>
            """));
    }

    @Test
    void tripleQuotesInHtmlBlockStayLiteral() throws Exception {
        assertEquals("<p>say \"\"\"hi\"\"\"</p>\n<p>x</p>\n", render("""
                    <p>say \"\"\"hi\"\"\"</p>
                    <p>x</p>
            """));
    }
}
