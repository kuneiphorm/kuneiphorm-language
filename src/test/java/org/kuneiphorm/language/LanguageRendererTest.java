package org.kuneiphorm.language;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.kuneiphorm.daedalus.core.Expression;
import org.kuneiphorm.language.grammar.Grammar;
import org.kuneiphorm.language.grammar.Rule;
import org.kuneiphorm.language.grammar.Symbol;
import org.kuneiphorm.language.grammar.Terminal;
import org.kuneiphorm.language.grammar.Variable;
import org.kuneiphorm.language.token.TokenDefinition;
import org.kuneiphorm.language.token.TokenTag;

class LanguageRendererTest {

  // --- Helpers ---

  private static Variable<String> var(String name) {
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

  // --- Header ---

  @Test
  void render_header_containsLanguageName() {
    Language<String> lang =
        new Language<>(
            "MyLang",
            new Grammar<>(var("S"), List.of(rule("S", term("a")))),
            List.of(token("a", "a")));
    String result = LanguageRenderer.render(lang);
    assertTrue(result.startsWith("language MyLang;\n"));
  }

  @Test
  void render_fullStructure_containsBlockBraces() {
    Language<String> lang =
        new Language<>(
            "Test",
            new Grammar<>(var("S"), List.of(rule("S", term("a")))),
            List.of(token("a", "a")));
    String result = LanguageRenderer.render(lang);
    // Tokens block has opening and closing braces
    assertTrue(result.contains("tokens {\n"));
    assertTrue(result.contains("}\n"));
    // Rules block has opening brace and closing brace at end
    assertTrue(result.contains("rules {\n"));
    assertTrue(result.endsWith("}"));
  }

  @Test
  void render_lexerResult_containsTokensBlock() {
    ParseResult.LexerResult<String> lr =
        new ParseResult.LexerResult<>("Lex", List.of(token("a", "a")));
    String rendered = LanguageRenderer.renderResult(lr);
    assertTrue(rendered.contains("tokens {\n"));
    assertTrue(rendered.contains("}\n"));
  }

  @Test
  void render_parserResult_endsWithClosingBrace() {
    Grammar<String> g = new Grammar<>(var("S"), List.of(rule("S", term("a"))));
    ParseResult.ParserResult<String> pr = new ParseResult.ParserResult<>("Par", g);
    String rendered = LanguageRenderer.renderResult(pr);
    assertTrue(rendered.contains("rules {\n"));
    assertTrue(rendered.endsWith("}"));
  }

  // --- Tokens block ---

  @Test
  void render_token_containsLabelAndRegex() {
    Language<String> lang =
        new Language<>(
            "Test",
            new Grammar<>(var("S"), List.of(rule("S", term("ID")))),
            List.of(token("ID", "[a-z]+")));
    String result = LanguageRenderer.render(lang);
    assertTrue(result.contains("'ID' <- \"[a-z]+\";"));
  }

  @Test
  void render_skipToken_containsAtSkipTag() {
    Language<String> lang =
        new Language<>(
            "Test",
            new Grammar<>(var("S"), List.of(rule("S", term("ID")))),
            List.of(token("ID", "[a-z]+"), skipToken("WS", "[ ]+")));
    String result = LanguageRenderer.render(lang);
    assertTrue(result.contains("@skip 'WS' <- \"[ ]+\";"));
  }

  // --- Rules block ---

  @Test
  void render_rule_containsSourceAndTerminal() {
    Language<String> lang =
        new Language<>(
            "Test",
            new Grammar<>(var("S"), List.of(rule("S", term("ID")))),
            List.of(token("ID", "[a-z]+")));
    String result = LanguageRenderer.render(lang);
    assertTrue(result.contains("S => 'ID';"));
  }

  @Test
  void render_namedRule_containsBracketedName() {
    Language<String> lang =
        new Language<>(
            "Test",
            new Grammar<>(var("S"), List.of(namedRule("start", "S", term("ID")))),
            List.of(token("ID", "[a-z]+")));
    String result = LanguageRenderer.render(lang);
    assertTrue(result.contains("[start] S => 'ID';"));
  }

  @Test
  void render_variable_renderedWithoutQuotes() {
    Language<String> lang =
        new Language<>(
            "Test",
            new Grammar<>(var("S"), List.of(rule("S", ref("T")), rule("T", term("a")))),
            List.of(token("a", "a")));
    String result = LanguageRenderer.render(lang);
    assertTrue(result.contains("S => T;"));
  }

  // --- Expression rendering ---

  @Test
  void render_sequence_spacesSeparated() {
    Language<String> lang =
        new Language<>(
            "Test",
            new Grammar<>(var("S"), List.of(rule("S", Expression.sequence(term("a"), term("b"))))),
            List.of(token("a", "a"), token("b", "b")));
    String result = LanguageRenderer.render(lang);
    assertTrue(result.contains("'a' 'b'"));
  }

  @Test
  void render_choice_pipeSeparated() {
    Language<String> lang =
        new Language<>(
            "Test",
            new Grammar<>(var("S"), List.of(rule("S", Expression.choice(term("a"), term("b"))))),
            List.of(token("a", "a"), token("b", "b")));
    String result = LanguageRenderer.render(lang);
    assertTrue(result.contains("'a' | 'b'"));
  }

  @Test
  void render_quantifierStar_appendsStar() {
    Language<String> lang =
        new Language<>(
            "Test",
            new Grammar<>(var("S"), List.of(rule("S", Expression.star(term("a"))))),
            List.of(token("a", "a")));
    String result = LanguageRenderer.render(lang);
    assertTrue(result.contains("'a'*"));
  }

  @Test
  void render_quantifierPlus_appendsPlus() {
    Language<String> lang =
        new Language<>(
            "Test",
            new Grammar<>(var("S"), List.of(rule("S", Expression.plus(term("a"))))),
            List.of(token("a", "a")));
    String result = LanguageRenderer.render(lang);
    assertTrue(result.contains("'a'+"));
  }

  @Test
  void render_quantifierOptional_appendsQuestion() {
    Language<String> lang =
        new Language<>(
            "Test",
            new Grammar<>(var("S"), List.of(rule("S", Expression.optional(term("a"))))),
            List.of(token("a", "a")));
    String result = LanguageRenderer.render(lang);
    assertTrue(result.contains("'a'?"));
  }

  @Test
  void render_quantifierOnChoice_wrapsInParens() {
    Language<String> lang =
        new Language<>(
            "Test",
            new Grammar<>(
                var("S"),
                List.of(rule("S", Expression.star(Expression.choice(term("a"), term("b")))))),
            List.of(token("a", "a"), token("b", "b")));
    String result = LanguageRenderer.render(lang);
    assertTrue(result.contains("('a' | 'b')*"));
  }

  @Test
  void render_quantifierOnSequence_wrapsInParens() {
    Language<String> lang =
        new Language<>(
            "Test",
            new Grammar<>(
                var("S"),
                List.of(rule("S", Expression.plus(Expression.sequence(term("a"), term("b")))))),
            List.of(token("a", "a"), token("b", "b")));
    String result = LanguageRenderer.render(lang);
    assertTrue(result.contains("('a' 'b')+"));
  }

  @Test
  void render_quantifierOnUnit_noParens() {
    Language<String> lang =
        new Language<>(
            "Test",
            new Grammar<>(var("S"), List.of(rule("S", Expression.star(term("a"))))),
            List.of(token("a", "a")));
    String result = LanguageRenderer.render(lang);
    assertTrue(result.contains("'a'*"));
    assertFalse(result.contains("('a')*"));
  }

  @Test
  void render_quantifierOnVariable_noParens() {
    Language<String> lang =
        new Language<>(
            "Test",
            new Grammar<>(
                var("S"), List.of(rule("S", Expression.plus(ref("T"))), rule("T", term("a")))),
            List.of(token("a", "a")));
    String result = LanguageRenderer.render(lang);
    assertTrue(result.contains("T+"));
    assertFalse(result.contains("(T)+"));
  }

  // --- Null checks ---

  @Test
  void render_nullLanguage_throwsNpe() {
    assertThrows(NullPointerException.class, () -> LanguageRenderer.render(null));
  }

  @Test
  void render_nullLabelRenderer_throwsNpe() {
    Language<String> lang =
        new Language<>(
            "Test",
            new Grammar<>(var("S"), List.of(rule("S", term("a")))),
            List.of(token("a", "a")));
    assertThrows(NullPointerException.class, () -> LanguageRenderer.render(lang, null));
  }

  // --- Custom label renderer ---

  // --- ParseResult rendering ---

  @Test
  void render_lexerResult_containsLexerKeyword() {
    ParseResult.LexerResult<String> result =
        new ParseResult.LexerResult<>(
            "MyLexer", List.of(token("ID", "[a-z]+"), skipToken("WS", "[ ]+")));
    String rendered = LanguageRenderer.renderResult(result);
    assertTrue(rendered.startsWith("lexer MyLexer;\n"));
    assertTrue(rendered.contains("'ID' <- \"[a-z]+\";"));
    assertTrue(rendered.contains("@skip 'WS'"));
    assertFalse(rendered.contains("rules"));
  }

  @Test
  void render_parserResult_containsParserKeyword() {
    Grammar<String> g =
        new Grammar<>(
            var("S"),
            List.of(rule("S", Expression.sequence(term("a"), ref("T"))), rule("T", term("b"))));
    ParseResult.ParserResult<String> result = new ParseResult.ParserResult<>("MyParser", g);
    String rendered = LanguageRenderer.renderResult(result);
    assertTrue(rendered.startsWith("parser MyParser;\n"));
    assertTrue(rendered.contains("S => 'a' T;"));
    assertFalse(rendered.contains("tokens"));
  }

  @Test
  void render_languageResult_delegatesToLanguageRender() {
    Language<String> lang =
        new Language<>(
            "Test",
            new Grammar<>(var("S"), List.of(rule("S", term("a")))),
            List.of(token("a", "a")));
    ParseResult.LanguageResult<String> result = new ParseResult.LanguageResult<>(lang);
    String rendered = LanguageRenderer.renderResult(result);
    assertTrue(rendered.startsWith("language Test;\n"));
    assertTrue(rendered.contains("tokens"));
    assertTrue(rendered.contains("rules"));
  }

  @Test
  void render_parseResult_nullResult_throwsNpe() {
    assertThrows(
        NullPointerException.class, () -> LanguageRenderer.renderResult(null, Function.identity()));
  }

  // --- Custom label renderer ---

  @Test
  void render_customLabelRenderer_usesRenderer() {
    Language<Integer> lang =
        new Language<>(
            "NumLang",
            new Grammar<>(
                new Variable<>("S"),
                List.of(
                    new Rule<>(null, new Variable<>("S"), Expression.unit(new Terminal<>(42))))),
            List.of(new TokenDefinition<>(42, "[0-9]+", Set.of())));
    String result = LanguageRenderer.render(lang, i -> "T" + i);
    assertTrue(result.contains("'T42'"));
  }
}
