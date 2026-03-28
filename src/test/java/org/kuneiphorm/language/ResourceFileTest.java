package org.kuneiphorm.language;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.kuneiphorm.runtime.exception.SyntaxException;

/**
 * Tests that parse {@code .kph} resource files and verify their structure.
 *
 * <p>Resource files live in {@code src/test/resources/} and serve as reference examples for the
 * kuneiphorm language format.
 */
class ResourceFileTest {

  private static String loadResource(String name) throws IOException {
    InputStream is = ResourceFileTest.class.getClassLoader().getResourceAsStream(name);
    assertNotNull(is, "Resource not found: " + name);
    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
  }

  // -------------------------------------------------------------------------
  // expr-lang.kph
  // -------------------------------------------------------------------------

  @Test
  void exprLang_parsesAndValidates() throws IOException, SyntaxException, InvalidLanguageException {
    String input = loadResource("expr-lang.kph");
    ParseResult<String> result = LanguageParser.parse(input);
    assertTrue(result.isLanguageResult());

    Language<String> lang = result.getAsLanguageResult().language();
    assertEquals("ExprLang", lang.name());
    lang.validate();
  }

  @Test
  void exprLang_hasCorrectStructure() throws IOException, SyntaxException {
    Language<String> lang =
        LanguageParser.parse(loadResource("expr-lang.kph")).getAsLanguageResult().language();
    assertEquals(7, lang.tokens().size());
    assertEquals(7, lang.grammar().rules().size());
    assertEquals("Expr", lang.grammar().start().name());
    assertEquals(Set.of("NUM", "ID", "PLUS", "STAR", "LP", "RP"), lang.grammar().terminalLabels());
  }

  @Test
  void exprLang_roundTrips() throws IOException, SyntaxException {
    String input = loadResource("expr-lang.kph");
    Language<String> lang1 = LanguageParser.parse(input).getAsLanguageResult().language();
    String rendered = LanguageRenderer.render(lang1);
    Language<String> lang2 = LanguageParser.parse(rendered).getAsLanguageResult().language();

    assertEquals(lang1.name(), lang2.name());
    assertEquals(lang1.tokens().size(), lang2.tokens().size());
    assertEquals(lang1.grammar().rules().size(), lang2.grammar().rules().size());
  }

  // -------------------------------------------------------------------------
  // assign-lang.kph
  // -------------------------------------------------------------------------

  @Test
  void assignLang_parsesAndValidates()
      throws IOException, SyntaxException, InvalidLanguageException {
    Language<String> lang =
        LanguageParser.parse(loadResource("assign-lang.kph")).getAsLanguageResult().language();
    assertEquals("AssignLang", lang.name());
    lang.validate();
  }

  @Test
  void assignLang_hasQuantifiers() throws IOException, SyntaxException {
    Language<String> lang =
        LanguageParser.parse(loadResource("assign-lang.kph")).getAsLanguageResult().language();
    String rendered = LanguageRenderer.render(lang);
    assertTrue(rendered.contains("Stmt+"));
    assertTrue(rendered.contains("('COMMA' 'NUM')*"));
  }

  // -------------------------------------------------------------------------
  // json-like.kph
  // -------------------------------------------------------------------------

  @Test
  void jsonLike_parsesAndValidates() throws IOException, SyntaxException, InvalidLanguageException {
    Language<String> lang =
        LanguageParser.parse(loadResource("json-like.kph")).getAsLanguageResult().language();
    assertEquals("JsonLike", lang.name());
    lang.validate();
  }

  @Test
  void jsonLike_hasCorrectTokenCount() throws IOException, SyntaxException {
    Language<String> lang =
        LanguageParser.parse(loadResource("json-like.kph")).getAsLanguageResult().language();
    assertEquals(12, lang.tokens().size());
    assertEquals(4, lang.grammar().rules().size());
  }

  // -------------------------------------------------------------------------
  // standalone-lexer.kph
  // -------------------------------------------------------------------------

  @Test
  void standaloneLexer_parsesAsLexerResult() throws IOException, SyntaxException {
    ParseResult<String> result = LanguageParser.parse(loadResource("standalone-lexer.kph"));
    assertTrue(result.isLexerResult());

    ParseResult.LexerResult<String> lexer = result.getAsLexerResult();
    assertEquals("TokenLexer", lexer.name());
    assertEquals(10, lexer.tokens().size());
  }

  @Test
  void standaloneLexer_rendersAsLexer() throws IOException, SyntaxException {
    ParseResult<String> result = LanguageParser.parse(loadResource("standalone-lexer.kph"));
    String rendered = LanguageRenderer.renderResult(result);
    assertTrue(rendered.contains("lexer TokenLexer;\n"));
    assertFalse(rendered.contains("rules"));
  }

  // -------------------------------------------------------------------------
  // standalone-parser.kph
  // -------------------------------------------------------------------------

  @Test
  void standaloneParser_parsesAsParserResult() throws IOException, SyntaxException {
    ParseResult<String> result = LanguageParser.parse(loadResource("standalone-parser.kph"));
    assertTrue(result.isParserResult());

    ParseResult.ParserResult<String> parser = result.getAsParserResult();
    assertEquals("ExprParser", parser.name());
    assertEquals(9, parser.grammar().rules().size());
    assertEquals("Expr", parser.grammar().start().name());
  }

  @Test
  void standaloneParser_rendersAsParser() throws IOException, SyntaxException {
    ParseResult<String> result = LanguageParser.parse(loadResource("standalone-parser.kph"));
    String rendered = LanguageRenderer.renderResult(result);
    assertTrue(rendered.contains("parser ExprParser;\n"));
    assertFalse(rendered.contains("tokens"));
  }
}
