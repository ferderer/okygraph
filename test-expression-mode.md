# Test: JAVA_EXPRESSION Mode

## Simple Expression
```java
`<div>{user.name}</div>`
```

**Tokens:**
1. BACKTICK: `
2. HTML_TEXT: <div>
3. EXPRESSION_START: { → PUSH JAVA_EXPRESSION mode
4. IDENTIFIER: user
5. SEPARATOR: .
6. IDENTIFIER: name
7. EXPRESSION_END: } → POP back to TEMPLATE mode
8. HTML_TEXT: </div>
9. BACKTICK: ` → POP back to JAVA mode

## Expression with Lambda
```java
`<div>{items.stream().filter(x -> x.isActive()).count()}</div>`
```

**Tokens in JAVA_EXPRESSION mode:**
- IDENTIFIER: items
- SEPARATOR: .
- IDENTIFIER: stream
- SEPARATOR: (
- SEPARATOR: )
- SEPARATOR: .
- IDENTIFIER: filter
- SEPARATOR: (
- IDENTIFIER: x
- OPERATOR: ->
- IDENTIFIER: x
- SEPARATOR: .
- IDENTIFIER: isActive
- SEPARATOR: (
- SEPARATOR: )
- SEPARATOR: )
- SEPARATOR: .
- IDENTIFIER: count
- SEPARATOR: (
- SEPARATOR: )
- EXPRESSION_END: } → POP

✅ No nested braces needed for lambdas!

## Expression with Array Initializer
```java
`<div>{new int[] {1, 2, 3}.length}</div>`
```

**Tokens in JAVA_EXPRESSION mode:**
- KEYWORD: new
- IDENTIFIER: int
- SEPARATOR: [
- SEPARATOR: ]
- SEPARATOR: { → Just a separator (doesn't push new mode!)
- NUMBER: 1
- SEPARATOR: ,
- NUMBER: 2
- SEPARATOR: ,
- NUMBER: 3
- SEPARATOR: } → Just a separator (doesn't pop!)
- SEPARATOR: .
- IDENTIFIER: length
- EXPRESSION_END: } → POP

✅ Nested braces work correctly!

## Expression with Ternary
```java
`<div>{isActive ? "Yes" : "No"}</div>`
```

**Tokens in JAVA_EXPRESSION mode:**
- IDENTIFIER: isActive
- OPERATOR: ?
- STRING_LITERAL: "Yes"
- OPERATOR: :
- STRING_LITERAL: "No"
- EXPRESSION_END: }

✅ Strings inside expressions work!

## Mode Stack Example

```
Initial: [JAVA]

`              → PUSH TEMPLATE  → [JAVA, TEMPLATE]
<div>          → HTML_TEXT
{              → PUSH JAVA_EXPRESSION → [JAVA, TEMPLATE, JAVA_EXPRESSION]
user.name      → Java tokens
}              → POP → [JAVA, TEMPLATE]
</div>         → HTML_TEXT
`              → POP → [JAVA]
```

Perfect! 🎯
