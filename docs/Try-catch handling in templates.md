## Try/Catch Handling Summary

### Core Concept
When the transpiler encounters `try` in Java mode within a template, it automatically inserts buffer management calls to handle potential exceptions.

### Implementation

**BaseView has one writer stack:**
```java
protected TemplateWriter w;
private final Stack<TemplateWriter> writerStack = new Stack<>();

protected void pushBuffer() {
    writerStack.push(w);
    w = new BufferedWriter();
}

protected void popBufferCommit() {
    String content = w.getBuffered();
    w = writerStack.pop();
    write(content);
}

protected void popBufferDiscard() {
    w = writerStack.pop();  // Discard buffer
}
```

**Transpiler injects calls:**
- After `try {` → insert `pushBuffer();`
- Before `}` (end of try) → insert `popBufferCommit();`
- After `catch (...) {` → insert `popBufferDiscard();`

### Example Transformation

**Template:**
```java
{`
    `try {`
        <p>{riskyOp()}</p>
    `} catch (Exception e) {`
        <span>Error</span>
    `}`
`}
```

**Generated:**
```java
try { pushBuffer();
    write("<p>");
    writeHtml(riskyOp());
    write("</p>");
    popBufferCommit();
} catch (Exception e) { popBufferDiscard();
    write("<span>Error</span>");
}
```

### Execution Flow
- **Success:** Buffer fills → `popBufferCommit()` flushes to output
- **Exception:** Buffer fills partially → jumps to catch → `popBufferDiscard()` discards buffer → catch writes directly

### Benefits
- Line numbers preserved (just method calls)
- Handles nested try blocks automatically (stack)
- No partial output on exceptions
- Clean generated code
