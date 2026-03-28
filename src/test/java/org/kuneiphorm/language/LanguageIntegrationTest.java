package org.kuneiphorm.language;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.kuneiphorm.daedalus.core.Expression;
import org.kuneiphorm.language.grammar.Grammar;
import org.kuneiphorm.language.grammar.Rule;
import org.kuneiphorm.language.grammar.Symbol;
import org.kuneiphorm.language.grammar.Terminal;
import org.kuneiphorm.language.grammar.Variable;
import org.kuneiphorm.language.token.TokenDefinition;
import org.kuneiphorm.language.token.TokenTag;

/**
 * Integration tests with realistic language definitions.
 *
 * <p>These tests demonstrate how to define complete languages using the kuneiphorm-language API,
 * serving both as validation and as reference examples.
 */
class LanguageIntegrationTest {

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private static <L> Variable<L> var(String name) {
    return new Variable<>(name);
  }

  private static Expression<Symbol<String>> term(String label) {
    return Expression.unit(new Terminal<>(label));
  }

  private static Expression<Symbol<String>> ref(String name) {
    return Expression.unit(new Variable<>(name));
  }

  private static Rule<String> rule(String source, Expression<Symbol<String>> pattern) {
    return new Rule<>(null, var(source), pattern);
  }

  private static Rule<String> namedRule(
      String name, String source, Expression<Symbol<String>> pattern) {
    return new Rule<>(name, var(source), pattern);
  }

  private static TokenDefinition<String> token(String label, String regex) {
    return new TokenDefinition<>(label, regex, Set.of());
  }

  private static TokenDefinition<String> skipToken(String label, String regex) {
    return new TokenDefinition<>(label, regex, Set.of(TokenTag.SKIP));
  }

  // -------------------------------------------------------------------------
  // Arithmetic expression language
  //
  //   language ExprLang;
  //
  //   tokens {
  //       @skip 'WS' <- "[ \t\r\n]+";
  //       'NUM'  <- "[0-9]+";
  //       'ID'   <- "[a-zA-Z_][a-zA-Z0-9_]*";
  //       'PLUS' <- "\+";
  //       'STAR' <- "\*";
  //       'LP'   <- "\(";
  //       'RP'   <- "\)";
  //   }
  //
  //   rules {
  //       [add] Expr => Expr 'PLUS' Term;
  //       [term] Expr => Term;
  //       [mul] Term => Term 'STAR' Factor;
  //       [factor] Term => Factor;
  //       [group] Factor => 'LP' Expr 'RP';
  //       [num] Factor => 'NUM';
  //       [id] Factor => 'ID';
  //   }
  // -------------------------------------------------------------------------

  private static Language<String> buildExprLang() {
    List<TokenDefinition<String>> tokens =
        List.of(
            skipToken("WS", "[ \\t\\r\\n]+"),
            token("NUM", "[0-9]+"),
            token("ID", "[a-zA-Z_][a-zA-Z0-9_]*"),
            token("PLUS", "\\+"),
            token("STAR", "\\*"),
            token("LP", "\\("),
            token("RP", "\\)"));

    List<Rule<String>> rules =
        List.of(
            namedRule("add", "Expr", Expression.sequence(ref("Expr"), term("PLUS"), ref("Term"))),
            namedRule("term", "Expr", ref("Term")),
            namedRule("mul", "Term", Expression.sequence(ref("Term"), term("STAR"), ref("Factor"))),
            namedRule("factor", "Term", ref("Factor")),
            namedRule("group", "Factor", Expression.sequence(term("LP"), ref("Expr"), term("RP"))),
            namedRule("num", "Factor", term("NUM")),
            namedRule("id", "Factor", term("ID")));

    Grammar<String> grammar = new Grammar<>(var("Expr"), rules);
    return new Language<>("ExprLang", grammar, tokens);
  }

  @Test
  void exprLang_validates() {
    Language<String> lang = buildExprLang();
    assertDoesNotThrow(lang::validate);
  }

  @Test
  void exprLang_terminalLabels_matchTokenLabels() {
    Language<String> lang = buildExprLang();
    Set<String> terminalLabels = lang.grammar().terminalLabels();
    assertEquals(Set.of("NUM", "ID", "PLUS", "STAR", "LP", "RP"), terminalLabels);
  }

  @Test
  void exprLang_rulesFor_returnsCorrectCount() {
    Language<String> lang = buildExprLang();
    assertEquals(2, lang.grammar().rulesFor(var("Expr")).size());
    assertEquals(2, lang.grammar().rulesFor(var("Term")).size());
    assertEquals(3, lang.grammar().rulesFor(var("Factor")).size());
  }

  @Test
  void exprLang_renders_roundTrippable() {
    Language<String> lang = buildExprLang();
    String rendered = LanguageRenderer.render(lang);

    assertTrue(rendered.contains("language ExprLang;"));
    assertTrue(rendered.contains("@skip 'WS'"));
    assertTrue(rendered.contains("'NUM' <- \"[0-9]+\";"));
    assertTrue(rendered.contains("[add] Expr => Expr 'PLUS' Term;"));
    assertTrue(rendered.contains("[num] Factor => 'NUM';"));
    assertTrue(rendered.contains("[group] Factor => 'LP' Expr 'RP';"));
  }

  // -------------------------------------------------------------------------
  // Simple assignment language (uses quantifiers)
  //
  //   language AssignLang;
  //
  //   tokens {
  //       @skip 'WS' <- "[ \t\r\n]+";
  //       'ID'    <- "[a-zA-Z_][a-zA-Z0-9_]*";
  //       'NUM'   <- "[0-9]+";
  //       'ASSIGN' <- ":=";
  //       'SEMI'  <- ";";
  //       'COMMA' <- ",";
  //   }
  //
  //   rules {
  //       [prog] Prog => Stmt+;
  //       [assign] Stmt => 'ID' 'ASSIGN' Expr 'SEMI';
  //       [list] Expr => 'NUM' ('COMMA' 'NUM')*;
  //   }
  // -------------------------------------------------------------------------

  private static Language<String> buildAssignLang() {
    List<TokenDefinition<String>> tokens =
        List.of(
            skipToken("WS", "[ \\t\\r\\n]+"),
            token("ID", "[a-zA-Z_][a-zA-Z0-9_]*"),
            token("NUM", "[0-9]+"),
            token("ASSIGN", ":="),
            token("SEMI", ";"),
            token("COMMA", ","));

    List<Rule<String>> rules =
        List.of(
            namedRule("prog", "Prog", Expression.plus(ref("Stmt"))),
            namedRule(
                "assign",
                "Stmt",
                Expression.sequence(term("ID"), term("ASSIGN"), ref("Expr"), term("SEMI"))),
            namedRule(
                "list",
                "Expr",
                Expression.sequence(
                    term("NUM"),
                    Expression.star(Expression.sequence(term("COMMA"), term("NUM"))))));

    Grammar<String> grammar = new Grammar<>(var("Prog"), rules);
    return new Language<>("AssignLang", grammar, tokens);
  }

  @Test
  void assignLang_validates() {
    Language<String> lang = buildAssignLang();
    assertDoesNotThrow(lang::validate);
  }

  @Test
  void assignLang_renders_quantifiers() {
    Language<String> lang = buildAssignLang();
    String rendered = LanguageRenderer.render(lang);

    assertTrue(rendered.contains("[prog] Prog => Stmt+;"));
    assertTrue(rendered.contains("('COMMA' 'NUM')*"));
  }

  // -------------------------------------------------------------------------
  // JSON-like language (optional fields, choices)
  //
  //   language JsonLike;
  //
  //   tokens {
  //       @skip 'WS'   <- "[ \t\r\n]+";
  //       'LBRACE'     <- "\{";
  //       'RBRACE'     <- "\}";
  //       'LBRACKET'   <- "\[";
  //       'RBRACKET'   <- "\]";
  //       'COLON'      <- ":";
  //       'COMMA'      <- ",";
  //       'STRING'     <- "\"[^\"]*\"";
  //       'NUMBER'     <- "-?[0-9]+";
  //       'TRUE'       <- "true";
  //       'FALSE'      <- "false";
  //       'NULL'       <- "null";
  //   }
  //
  //   rules {
  //       Value => Object | Array | 'STRING' | 'NUMBER' | 'TRUE' | 'FALSE' | 'NULL';
  //       Object => 'LBRACE' (Pair ('COMMA' Pair)*)? 'RBRACE';
  //       Pair => 'STRING' 'COLON' Value;
  //       Array => 'LBRACKET' (Value ('COMMA' Value)*)? 'RBRACKET';
  //   }
  // -------------------------------------------------------------------------

  private static Language<String> buildJsonLike() {
    List<TokenDefinition<String>> tokens =
        List.of(
            skipToken("WS", "[ \\t\\r\\n]+"),
            token("LBRACE", "\\{"),
            token("RBRACE", "\\}"),
            token("LBRACKET", "\\["),
            token("RBRACKET", "\\]"),
            token("COLON", ":"),
            token("COMMA", ","),
            token("STRING", "\"[^\"]*\""),
            token("NUMBER", "-?[0-9]+"),
            token("TRUE", "true"),
            token("FALSE", "false"),
            token("NULL", "null"));

    // Value => Object | Array | 'STRING' | 'NUMBER' | 'TRUE' | 'FALSE' | 'NULL'
    Expression<Symbol<String>> valuePattern =
        Expression.choice(
            ref("Object"),
            ref("Array"),
            term("STRING"),
            term("NUMBER"),
            term("TRUE"),
            term("FALSE"),
            term("NULL"));

    // Object => 'LBRACE' (Pair ('COMMA' Pair)*)? 'RBRACE'
    Expression<Symbol<String>> objectPattern =
        Expression.sequence(
            term("LBRACE"),
            Expression.optional(
                Expression.sequence(
                    ref("Pair"), Expression.star(Expression.sequence(term("COMMA"), ref("Pair"))))),
            term("RBRACE"));

    // Pair => 'STRING' 'COLON' Value
    Expression<Symbol<String>> pairPattern =
        Expression.sequence(term("STRING"), term("COLON"), ref("Value"));

    // Array => 'LBRACKET' (Value ('COMMA' Value)*)? 'RBRACKET'
    Expression<Symbol<String>> arrayPattern =
        Expression.sequence(
            term("LBRACKET"),
            Expression.optional(
                Expression.sequence(
                    ref("Value"),
                    Expression.star(Expression.sequence(term("COMMA"), ref("Value"))))),
            term("RBRACKET"));

    List<Rule<String>> rules =
        List.of(
            rule("Value", valuePattern),
            rule("Object", objectPattern),
            rule("Pair", pairPattern),
            rule("Array", arrayPattern));

    Grammar<String> grammar = new Grammar<>(var("Value"), rules);
    return new Language<>("JsonLike", grammar, tokens);
  }

  @Test
  void jsonLike_validates() {
    Language<String> lang = buildJsonLike();
    assertDoesNotThrow(lang::validate);
  }

  @Test
  void jsonLike_renders_complexExpressions() {
    Language<String> lang = buildJsonLike();
    String rendered = LanguageRenderer.render(lang);

    assertTrue(rendered.contains("language JsonLike;"));
    // Value has a 7-way choice
    assertTrue(
        rendered.contains("Object | Array | 'STRING' | 'NUMBER' | 'TRUE' | 'FALSE' | 'NULL'"));
    // Object uses optional + star
    assertTrue(rendered.contains("(Pair ('COMMA' Pair)*)?"));
  }

  @Test
  void jsonLike_terminalLabels_coversAllTokens() {
    Language<String> lang = buildJsonLike();
    Set<String> terminals = lang.grammar().terminalLabels();
    // All non-skip tokens should be referenced
    Set<String> expected =
        Set.of(
            "LBRACE",
            "RBRACE",
            "LBRACKET",
            "RBRACKET",
            "COLON",
            "COMMA",
            "STRING",
            "NUMBER",
            "TRUE",
            "FALSE",
            "NULL");
    assertEquals(expected, terminals);
  }
}
