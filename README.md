[![CI](https://img.shields.io/github/actions/workflow/status/kuneiphorm/kuneiphorm-language/ci.yml?branch=master&label=CI)](https://github.com/kuneiphorm/kuneiphorm-language/actions)
[![kuneiphorm-language](https://img.shields.io/github/v/release/kuneiphorm/kuneiphorm-language?include_prereleases&label=kuneiphorm-language)](https://github.com/kuneiphorm/kuneiphorm-language/releases)
![License](https://img.shields.io/badge/License-Apache_2.0-blue)
![Java](https://img.shields.io/badge/Java-21-blue)

# kuneiphorm-language

Language definition model, parser, renderer, and validation for the kuneiphorm toolkit.

This module defines the data structures for complete language specifications (grammar rules + token definitions), provides a recursive-descent parser for `.kph` language files, and a renderer that produces valid `.kph` output. It ties together the grammar (production rules over terminals and variables) and the lexical specification (regex-based token definitions with optional tags like `@skip`).

## Package overview

```
org.kuneiphorm.language
├── Language                  Complete language definition (grammar + tokens)
├── LanguageParser            Recursive-descent .kph parser
├── LanguageRenderer          Renders definitions to .kph format (Writer-based)
├── ParseResult               Sealed result: LanguageResult | LexerResult | ParserResult
├── InvalidLanguageException  Validation errors for Language
├── grammar/
│   ├── Grammar               Start symbol + ordered rule list + validation
│   ├── Rule                  Production rule with optional semantic name
│   ├── Symbol                Sealed interface: Terminal | Variable
│   ├── Terminal              Token label reference in a production
│   ├── Variable              Non-terminal reference in a production
│   └── InvalidGrammarException
└── token/
    ├── TokenDefinition       Label + regex + tags
    └── TokenTag              Enum: SKIP
```

## File format

The `.kph` format supports three kinds of definitions:

```
language ExprLang;

tokens {
    @skip 'WS' <- "[ \t\r\n]+";
    'NUM'  <- "[0-9]+";
    'ID'   <- "[a-zA-Z_][a-zA-Z0-9_]*";
    'PLUS' <- "\+";
    'STAR' <- "\*";
    'LP'   <- "\(";
    'RP'   <- "\)";
}

rules {
    [add] Expr => Expr 'PLUS' Term;
    [term] Expr => Term;
    [mul] Term => Term 'STAR' Factor;
    [factor] Term => Factor;
    [group] Factor => 'LP' Expr 'RP';
    [num] Factor => 'NUM';
    [id] Factor => 'ID';
}
```

Standalone lexer (`lexer <name>; tokens { ... }`) and parser (`parser <name>; rules { ... }`) definitions are also supported.

## Usage

### Parsing a language file

```java
// Parse with string labels (common case)
ParseResult<String> result = LanguageParser.parse(input);

// Parse with custom label type
ParseResult<MyEnum> result = LanguageParser.parse(input, MyEnum::valueOf);

// Access the result
if (result.isLanguageResult()) {
    Language<String> lang = result.getAsLanguageResult().language();
    lang.validate(); // structural validation
}
```

### Building programmatically

```java
Grammar<String> grammar = new Grammar<>(
    new Variable<>("Expr"),
    List.of(
        new Rule<>("add", new Variable<>("Expr"),
            Expression.sequence(ref("Expr"), term("PLUS"), ref("Term"))),
        new Rule<>("num", new Variable<>("Expr"), ref("Term")),
        new Rule<>("lit", new Variable<>("Term"), term("NUM"))));

Language<String> lang = new Language<>("ExprLang", grammar, List.of(
    new TokenDefinition<>("WS", "[ \\t]+", Set.of(TokenTag.SKIP)),
    new TokenDefinition<>("NUM", "[0-9]+", Set.of()),
    new TokenDefinition<>("PLUS", "\\+", Set.of())));

lang.validate();
```

### Rendering

```java
// To string
String output = LanguageRenderer.render(lang);

// To writer (streaming)
LanguageRenderer.render(lang, Function.identity(), writer);

// Render any ParseResult
String output = LanguageRenderer.renderResult(result);
```

## Validation

`Language.validate()` performs comprehensive structural checks:

- All grammar validations (start symbol defined, no undefined variables, no unreachable variables)
- Every terminal referenced in the grammar has a corresponding token definition
- Every non-`@skip` token definition is referenced by at least one grammar rule
- No duplicate token definitions

`Grammar.validate()` checks:

- Start variable has at least one defining rule
- All referenced variables are defined
- All defined variables are reachable from the start symbol

## Key design decisions

- **`Symbol<L>` is sealed.** `Terminal<L>` and `Variable<L>` are the two variants. Pattern matching in Java 21 ensures exhaustive dispatch.
- **`Expression<Symbol<L>>` for production bodies.** Reuses the expression algebra from `kuneiphorm-daedalus`, with `Symbol<L>` as the unit label type. Supports alternation, sequence, quantifiers, and grouping.
- **`Variable<L>` has a phantom type parameter.** Ensures type consistency with `Terminal<L>` in the same `Symbol<L>` hierarchy without carrying a label.
- **`@skip` is the authoritative spec.** The language definition determines which tokens are filtered during parsing. Runtime filtering via predicates remains available as an escape hatch.
- **`LanguageRenderer` writes to `Writer`.** Supports streaming output to files, network sockets, etc. String convenience overloads delegate to `StringWriter`.
- **`LanguageParser` accepts a label mapper.** `Function<String, L>` converts string literals from the file format to typed labels, enabling generic label types.
- **`ParseResult` is sealed with convenience accessors.** `isLanguageResult()` / `getAsLanguageResult()` etc. provide Gson-style access alongside pattern matching.
- **First rule determines the start symbol.** No explicit start declaration needed; the source of the first rule is the start variable.
- **Error accumulation.** Both `Grammar.validate()` and `Language.validate()` collect all errors before throwing, rather than fail-fast on the first issue.

## Dependencies

- `kuneiphorm-daedalus` -- expression algebra (`Expression`, `ExpressionUnit`, etc.)
- `kuneiphorm-runtime` -- `CharFlow`, `CharFlowUtils`, `SyntaxException`

## Requirements

- Java 21+
