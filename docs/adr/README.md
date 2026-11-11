# Architecture decision records

Decisions that shape Okygraph, in [MADR](https://adr.github.io/madr/) form. The numbers are stable identifiers; not every number is published, so there are gaps.

| ADR | Decision |
|---|---|
| [0008](0008-templates-are-transpiled-to-java-validation-is-handled-by-th.md) | Templates are transpiled to Java; validation is handled by the build |
| [0038](0038-template-syntax-java-control-structures-in-backticks-express.md) | Template syntax: Java control structures in backticks, expressions in {…} |
| [0039](0039-no-ast-transpilation-directly-on-the-token-stream.md) | No AST: transpilation directly on the token stream |
| [0040](0040-okygraph-token-based-transpilation-before-compilation-via-ma.md) | Okygraph: token-based transpilation before compilation via Maven plugin |
| [0047](0047-no-write-object-type-checking-by-the-java-compiler.md) | No write(Object); type checking by the Java compiler |
| [0057](0057-unicode-escape-preprocessing-via-regex-inlined-into-the-toke.md) | Unicode escape preprocessing via regex, inlined into the tokenizer |
| [0059](0059-tokenizer-as-a-context-based-state-machine-without-a-stack.md) | Tokenizer as a context-based state machine without a stack |
| [0060](0060-streaming-transpiler-instead-of-a-token-list.md) | Streaming transpiler instead of a token list |
| [0062](0062-html-output-text-blocks-for-whole-lines-method-chaining-for.md) | HTML output: text blocks for whole lines, method chaining for fragments |
| [0072](0072-tokenpattern-enum-with-match-method-based-on-regions-and-loo.md) | TokenPattern enum with match() method based on regions and lookingAt() |
| [0086](0086-framework-integration-via-maven-plugin-parameter-instead-of.md) | Framework integration via Maven plugin parameter instead of Spring Boot starter |
| [0087](0087-writer-stack-with-response-writer-as-root.md) | Writer stack with response writer as root |
| [0089](0089-only-raw-x-as-the-syntax-for-unescaped-output.md) | Only {raw(x)} as the syntax for unescaped output |
| [0091](0091-i18n-helper-in-defaultlayout-as-a-documented-template.md) | i18n helper in DefaultLayout as a documented template |
| [0092](0092-layout-as-a-method-with-content-lambdas-instead-of-overridin.md) | Layout as a method with Content lambdas instead of overriding render() |
| [0102](0102-indent-handling-via-saveindent-writeindent-and-central-flush.md) | Indent handling via saveIndent/writeIndent and central flush() |
| [0104](0104-okytemplate-is-provided-via-code-generation-instead-of-as-a.md) | OkyTemplate is provided via code generation instead of as a runtime dependency |
| [0108](0108-vs-code-plugin-phase-1-pure-grammar-extension-lsp-later.md) | VS Code plugin phase 1: pure grammar extension, LSP later |
| [0110](0110-custom-html-patterns-instead-of-including-text-html-basic.md) | Custom HTML patterns instead of including text.html.basic |
| [0111](0111-crlf-line-endings-are-normalized-in-the-transpiler.md) | CRLF line endings are normalized in the transpiler |
| [0112](0112-always-transpile-all-oky-files-completely.md) | Always transpile all .oky files completely |
| [0113](0113-oky-files-in-src-main-java-generated-sources-registered-by-the-mojo.md) | .oky files in src/main/java; generated sources in target/generated-sources, registered by the mojo |
| [0115](0115-tokenizer-and-transpiler-in-one-class-catch-declaration-ends-at-brace.md) | Tokenizer and transpiler in one class; the catch declaration ends at EXPRESSION_START in the CATCH context |
| [0117](0117-base-class-holds-framework-integration-only-app-helpers-live-in-the-app-layout.md) | The base class holds framework integration only; app helpers such as CSRF live in the app's layout class |
| [0118](0118-html-escapes-only-backtick-and-brace-other-backslashes-are-literal.md) | HTML escapes: only \` and \{ are escapes, every other backslash is literal |
| [0119](0119-template-regions-enter-java-scopes-via-a-grammar-injection.md) | Template regions enter Java scopes via a grammar injection |
| [0120](0120-text-blocks-are-opened-and-closed-at-the-shallowest-line-indent.md) | Text blocks are opened and closed at the shallowest line indent |
| [0121](0121-escaping-text-by-default-attr-for-quoted-attribute-values.md) | Escaping: text by default, attr() for quoted attribute values |
| [0122](0122-standalone-base-class-renders-into-the-writer-it-is-given.md) | STANDALONE base class: renders into the Writer it is given, no framework types |
