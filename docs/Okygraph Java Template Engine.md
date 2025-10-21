# Okygraph Java Template Engine

## Overview

Java Method Templates (JMT) is a compile-time template engine that transpiles template method bodies to pure Java code. Unlike traditional template engines that generate entire classes, JMT focuses solely on method body transformation, giving developers complete control over class structure while providing high-performance template rendering.

## Core Principles

- **Method-Body Only**: Transpiles only template method bodies, not entire classes
- **Zero Runtime Dependencies**: Generates pure Java `write()` method calls at compile time
- **Developer Control**: Developers write normal Java classes with full IDE support
- **Type Safety**: Full compile-time validation via standard Java tools
- **Performance**: No interpretation overhead, just compiled Java method calls
- **Universal Output**: Works with any output destination via pluggable writers

## File Structure

### Input: .jmt Files
JMT files are standard Java source files with the `.jmt` extension containing template method bodies marked with backticks.

**Example: UserProfileView.jmt**
```java
package com.example.domain.users;

import de.ferderer.okygraph.core.OkygraphTemplate;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;

@Component
@Scope("request")
public class UserProfileView extends OkygraphTemplate {
    
    private User user;
    private List<Notification> notifications;
    
    // Standard Java getters/setters
    public void setUser(User user) {
        this.user = user;
    }

    public void setNotifications(List<Notification> notifications) { 
        this.notifications = notifications; 
    }
    
    // Template method - will be transpiled
    public void renderContent() {`
        <html>
        <head><title>User Profile - {user.name}</title></head>
        <body>
            <h1>Welcome {user.name}!</h1>
            
            `if (user.isActive()) {`
                <span class="badge-active">Online</span>
            `} else {`
                <span class="badge-inactive">Offline</span>
            `}`
            
            <div class="notifications">
                `for (notification : notifications) {`
                    <div class="alert">{notification.message}</div>
                `}`
            </div>
        </body>
        </html>
    `}
    
    // Regular Java methods remain unchanged
    private void logAccess() {
        System.out.println("User accessed profile: " + user.getId());
    }
}
```

### Output: Generated Java Files
The transpiler generates standard Java files in `target/generated-sources/` with the same package structure.

**Generated: target/generated-sources/com/example/domain/users/UserProfileView.java**
```java
package com.example.views;

import dev.okygraph.core.BaseView;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;

@Component
@Scope("request")
public class UserProfileView extends BaseView {
    
    private User user;
    private List<Notification> notifications;
    
    // Standard Java getters/setters (unchanged)
    public void setUser(User user) { this.user = user; }
    public void setNotifications(List<Notification> notifications) { 
        this.notifications = notifications; 
    }
    
    // Transpiled template method
    public void renderContent() {
        write("<html>");
        write("<head><title>User Profile - ").write(escape(user.name, EscapeMode.HTML)).write("</title></head>");
        write("<body>");
        write("<h1>Welcome ");
        writeEsc(user.name);
        write("!</h1>");
        if (user.isActive()) {
            write("<span class=\"badge-active\">Online</span>");
        } else {
            write("<span class=\"badge-inactive\">Offline</span>");
        }
        write("<div class=\"notifications\">");
        for (notification : notifications) {
            write("<div class=\"alert\">");
            writeEsc(notification.message);
            write("</div>");
        }
        write("""
            </div>
          </body>
          </html>
        """);
    }
    
    // Regular Java methods (unchanged)
    private void logAccess() {
        System.out.println("User accessed profile: " + user.getId());
    }
}
```

## Template Method Syntax

There only syntax concepts:
 - Backtick switches between HTML and Java
 - Braces capture and print Java expressions

### Content Types

#### Static Content
Plain text/HTML content is converted to `write()` calls: `<h1>Hello World</h1>` generates `write("<h1>Hello World</h1>");`

#### Consecuitive Static Line
Consecuitive Static Line are converted to text block syntax

```java
write("""
    </div>
  </body>
  </html>
""");
```

#### Expressions
Dynamic content wrapped in `{expression}`:
```java
{`<h1>Welcome {user.name}!</h1>`}
```
**Generates:**
```java
{
    write("<h1>Welcome ").write(escape(user.name, EscapeMode.HTML)).write("!</h1>");
}
```

## Control Structures and Exception Handling

Works as expected because it's just HTML.

## Expression Processing

### Automatic Escaping
Expressions are automatically escaped using `write(escape(expr, EscapeMode.XXX))` for security:

## Content Composition

### Lambda-Based Content Passing
Templates support composable content through lambda expressions:

```java
// Layout method
public void layout(String title, Content body) {`
    <!DOCTYPE html>
    <html>
    <head><title>{escHtml(title)}</title></head>
    <body>
        <header><nav>Site Navigation</nav></header>
        <main>
        {body}
        </main>
        <footer>&copy; 2025</footer>
    </body>
    </html>
`}

// Usage
public void renderPage() {
    layout("Home Page", () -> {`
        <div class="welcome">
            <h1>Welcome {user.name}!</h1>
            `if (user.hasNotifications()) {`
                <div class="notifications">You have messages!</div>
            `}`
        </div>
    `});
}
```

Overloaded write Method takes care of rendering the Content instances.

### Maven Plugin Configuration
```xml
<plugin>
    <groupId>dev.okygraph</groupId>
    <artifactId>okygraph-maven-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
        <sourceDirectory>src/main/templates</sourceDirectory>
        <outputDirectory>target/generated-sources/templates</outputDirectory>
        <templateExtension>jmt</templateExtension>
        <verbose>false</verbose>
        <failOnError>true</failOnError>
    </configuration>
    <executions>
        <execution>
            <phase>process-sources</phase>
            <goals>
                <goal>process</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Processing Flow
1. **Discovery**: Plugin scans `src/main/java/**/*.jmt`
2. **Preprocessing**: convert unicode sequences to normal characters.
3. **Tokenization**: Convert Java file to token list/stream.
4. **Transpilation**: Convert tokens into java code.
5. **Compilation**: Generated files are automatically included in compilation


## Performance Characteristics

### Zero Runtime Overhead
- Templates compile to direct method calls
- No reflection or interpretation
- String literals stored in string pool
- Primitive values written directly

### Optimizations
- **Write calls**: Single-parameter methods for JIT optimization
- **String escaping**: Efficient character-by-character processing
- **Primitive handling**: No boxing with TypeSafeWriter

## Framework Compatibility

### Spring Boot
```java
@Controller
public class UserController {
    @Inject private UserProfileView userProfileView; // or directly new UserProfileView() if no bean injection required.
    
    @GetMapping("/profile")
    public void profile(HttpServletResponse response) throws Exception {
        userProfileView.setUser(getCurrentUser());
        userProfileView.renderToHttp(response);
    }
}
```

### JEE/Servlet
```java
@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {
    @Inject private UserProfileView userProfileView;
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        userProfileView.renderToHttp(resp);
    }
}
```

### Standalone Applications

TODO: need to provide a writer
```java
public class EmailService {
    public String generateWelcomeEmail(User user) throws Exception {
        WelcomeEmailTemplate template = new WelcomeEmailTemplate();
        template.setUser(user);
        return template.render();
    }
}
```

## Use Cases

### Web Applications
- **HTML pages**: Dynamic server-side rendering
- **API responses**: JSON/XML generation with templates
- **Error pages**: Templated error responses

### Email Systems
- **Welcome emails**: Personalized HTML/text emails
- **Notifications**: Dynamic email content
- **Reports**: Formatted email reports

### Code Generation
- **SQL queries**: Dynamic query building
- **Configuration files**: Environment-specific configs
- **Documentation**: Generated API docs

### Report Generation
- **CSV exports**: Templated data exports
- **Text reports**: Formatted console output
- **Log formatting**: Structured log entries
