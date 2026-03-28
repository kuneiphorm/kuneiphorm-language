package org.kuneiphorm.language;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.kuneiphorm.daedalus.core.Expression;
import org.kuneiphorm.language.grammar.Grammar;
import org.kuneiphorm.language.grammar.Rule;
import org.kuneiphorm.language.grammar.Terminal;
import org.kuneiphorm.language.grammar.Variable;
import org.kuneiphorm.language.token.TokenDefinition;

class ParseResultTest {

  private static final Language<String> LANG =
      new Language<>(
          "Test",
          new Grammar<>(
              new Variable<>("S"),
              List.of(new Rule<>(null, new Variable<>("S"), Expression.unit(new Terminal<>("a"))))),
          List.of(new TokenDefinition<>("a", "a", Set.of())));

  private static final ParseResult<String> LANGUAGE_RESULT = new ParseResult.LanguageResult<>(LANG);

  private static final ParseResult<String> LEXER_RESULT =
      new ParseResult.LexerResult<>("Lex", List.of(new TokenDefinition<>("a", "a", Set.of())));

  private static final ParseResult<String> PARSER_RESULT =
      new ParseResult.ParserResult<>("Par", LANG.grammar());

  // --- isXxxResult ---

  @Test
  void isLanguageResult_onLanguage_returnsTrue() {
    assertTrue(LANGUAGE_RESULT.isLanguageResult());
  }

  @Test
  void isLanguageResult_onLexer_returnsFalse() {
    assertFalse(LEXER_RESULT.isLanguageResult());
  }

  @Test
  void isLanguageResult_onParser_returnsFalse() {
    assertFalse(PARSER_RESULT.isLanguageResult());
  }

  @Test
  void isLexerResult_onLexer_returnsTrue() {
    assertTrue(LEXER_RESULT.isLexerResult());
  }

  @Test
  void isLexerResult_onLanguage_returnsFalse() {
    assertFalse(LANGUAGE_RESULT.isLexerResult());
  }

  @Test
  void isParserResult_onParser_returnsTrue() {
    assertTrue(PARSER_RESULT.isParserResult());
  }

  @Test
  void isParserResult_onLanguage_returnsFalse() {
    assertFalse(LANGUAGE_RESULT.isParserResult());
  }

  // --- getAsXxxResult ---

  @Test
  void getAsLanguageResult_onLanguage_returnsValue() {
    ParseResult.LanguageResult<String> lr = LANGUAGE_RESULT.getAsLanguageResult();
    assertSame(LANG, lr.language());
  }

  @Test
  void getAsLanguageResult_onLexer_throwsIse() {
    assertThrows(IllegalStateException.class, LEXER_RESULT::getAsLanguageResult);
  }

  @Test
  void getAsLanguageResult_onParser_throwsIse() {
    assertThrows(IllegalStateException.class, PARSER_RESULT::getAsLanguageResult);
  }

  @Test
  void getAsLexerResult_onLexer_returnsValue() {
    ParseResult.LexerResult<String> lr = LEXER_RESULT.getAsLexerResult();
    assertEquals("Lex", lr.name());
  }

  @Test
  void getAsLexerResult_onLanguage_throwsIse() {
    assertThrows(IllegalStateException.class, LANGUAGE_RESULT::getAsLexerResult);
  }

  @Test
  void getAsLexerResult_onParser_throwsIse() {
    assertThrows(IllegalStateException.class, PARSER_RESULT::getAsLexerResult);
  }

  @Test
  void getAsParserResult_onParser_returnsValue() {
    ParseResult.ParserResult<String> pr = PARSER_RESULT.getAsParserResult();
    assertEquals("Par", pr.name());
  }

  @Test
  void getAsParserResult_onLanguage_throwsIse() {
    assertThrows(IllegalStateException.class, LANGUAGE_RESULT::getAsParserResult);
  }

  @Test
  void getAsParserResult_onLexer_throwsIse() {
    assertThrows(IllegalStateException.class, LEXER_RESULT::getAsParserResult);
  }

  // --- LexerResult immutability ---

  @Test
  void lexerResult_tokens_areImmutable() {
    ParseResult.LexerResult<String> lr = LEXER_RESULT.getAsLexerResult();
    assertThrows(UnsupportedOperationException.class, () -> lr.tokens().add(null));
  }
}
