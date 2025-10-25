package de.ferderer.okygraph.maven;

import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class TranspilerTest {

    @Test
    void testPlainJavaPassthrough() throws IOException {
        String input = """
            public class Test {
                int x = 42;
            }
        """;
        String output = Transpiler.process(input);
        assertEquals(input, output);
    }

    @Test
    void testSingleHTMLLine() throws IOException {
        String input = """
            void render() {`
                <p>Hello World</p>
            `}
            """;
        String expected = """
            void render() {
                html("<p>Hello World</p>");
            }
            """;
        assertEquals(expected, Transpiler.process(input));
    }

    @Test
    void testSimpleHTMLBlock() throws IOException {
        String input = """
            void render() {`
                <p>Hello World!</p>
                <p>Hello Claude!</p>
            `}
            """;
        String expected = """
            void render() {
                html(\"\"\"
                <p>Hello World!</p>
                <p>Hello Claude!</p>
                \"\"\");
            }
            """;
        assertEquals(expected, Transpiler.process(input));
    }

    private static final String COMPLEX_TEST_OKY = 
    """
    protected void render() {`
        <p>Greetings {user.name}!</p>
        `if (user.isActive()) {`
            <p>User is active</p>
        `} else {`
            <p>User is active</p>
        `}`
        <h3>Die Hobbies von {user.name}:</h3>
        <ul>
            `for (var hobby : user.getHobbies()) {`
                <li>{hobby}</li>
            `}`
        </ul>
        `try {`
            <img src="{user.getAvatar}">
        `} catch (AppException e) {`
            <img src="/img/defaultAvatar.png">
        `}`
        <p><b>All this is possible with 300 LOC of Java code! 💪</b></p>
        <p>Give it a try!</p>
    `}
    """;

    private static final String COMPLEX_TEST_JAVA = 
    """
    protected void render() {
        html("<p>Greetings ").write(user.name).html("!</p>\\n");
        if (user.isActive()) {
            html("<p>User is active</p>\\n");
        } else {
            html("<p>User is active</p>\\n");
        }
        html("<h3>Die Hobbies von ").write(user.name).html(":</h3>\\n");
        <ul>
            for (var hobby : user.getHobbies()) {
                html("<li>").write(hobby).html("</li>\\n");
            }
        </ul>
        try { pushBuffer();
            html("<img src=\\").write(user.getAvatar).html("\\">\\n");
        commitBuffer(); } catch (AppException e) { discardBuffer();
            html("<img src=\\"/img/defaultAvatar.png\\">");
        }
        html(\"\"\"
        <p><b>All this is possible with 300 LOC of Java code! 💪</b></p>
        <p>Give it a try!</p>
        \"\"\");
    `}
    """;
    @Test
    void testComplexTemplate() throws IOException {
        assertEquals(COMPLEX_TEST_JAVA, Transpiler.process(COMPLEX_TEST_OKY));
    }
}
