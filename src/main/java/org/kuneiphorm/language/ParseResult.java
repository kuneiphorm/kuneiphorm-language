package org.kuneiphorm.language;

import java.util.List;
import org.kuneiphorm.language.grammar.Grammar;
import org.kuneiphorm.language.token.TokenDefinition;

/**
 * The result of parsing a {@code .kph} language definition file.
 *
 * <p>Three variants correspond to the three kinds of definitions:
 *
 * <ul>
 *   <li>{@link LanguageResult} -- a full language definition ({@code language <name>}), containing
 *       both tokens and grammar rules.
 *   <li>{@link LexerResult} -- a standalone lexer definition ({@code lexer <name>}), containing
 *       only token definitions.
 *   <li>{@link ParserResult} -- a standalone parser definition ({@code parser <name>}), containing
 *       only grammar rules.
 * </ul>
 *
 * <p>Every result carries a format {@link #version()} number parsed from the {@code @version N;}
 * header.
 *
 * <p>Use pattern matching or the convenience methods {@link #getAsLanguageResult()}, {@link
 * #getAsLexerResult()}, and {@link #getAsParserResult()} to access the specific variant.
 *
 * @param <L> the terminal label type
 * @author Florent Guille
 * @since 0.1.0
 */
public sealed interface ParseResult<L>
    permits ParseResult.LanguageResult, ParseResult.LexerResult, ParseResult.ParserResult {

  /**
   * Returns the format version declared in the file header ({@code @version N;}).
   *
   * @return the format version number
   */
  int version();

  /**
   * Returns {@code true} if this result is a {@link LanguageResult}.
   *
   * @return whether this is a language result
   */
  default boolean isLanguageResult() {
    return this instanceof LanguageResult;
  }

  /**
   * Returns {@code true} if this result is a {@link LexerResult}.
   *
   * @return whether this is a lexer result
   */
  default boolean isLexerResult() {
    return this instanceof LexerResult;
  }

  /**
   * Returns {@code true} if this result is a {@link ParserResult}.
   *
   * @return whether this is a parser result
   */
  default boolean isParserResult() {
    return this instanceof ParserResult;
  }

  /**
   * Returns this result as a {@link LanguageResult}.
   *
   * @return the language result
   * @throws IllegalStateException if this is not a language result
   */
  default LanguageResult<L> getAsLanguageResult() {
    if (this instanceof LanguageResult<L> lr) {
      return lr;
    }
    throw new IllegalStateException("Not a language result");
  }

  /**
   * Returns this result as a {@link LexerResult}.
   *
   * @return the lexer result
   * @throws IllegalStateException if this is not a lexer result
   */
  default LexerResult<L> getAsLexerResult() {
    if (this instanceof LexerResult<L> lr) {
      return lr;
    }
    throw new IllegalStateException("Not a lexer result");
  }

  /**
   * Returns this result as a {@link ParserResult}.
   *
   * @return the parser result
   * @throws IllegalStateException if this is not a parser result
   */
  default ParserResult<L> getAsParserResult() {
    if (this instanceof ParserResult<L> pr) {
      return pr;
    }
    throw new IllegalStateException("Not a parser result");
  }

  /**
   * A full language definition with tokens and grammar rules.
   *
   * @param <L> the terminal label type
   * @param version the format version
   * @param language the parsed language
   */
  record LanguageResult<L>(int version, Language<L> language) implements ParseResult<L> {}

  /**
   * A standalone lexer definition with token definitions only.
   *
   * @param <L> the terminal label type
   * @param version the format version
   * @param name the lexer name
   * @param tokens the token definitions
   */
  record LexerResult<L>(int version, String name, List<TokenDefinition<L>> tokens)
      implements ParseResult<L> {

    /** Creates a new lexer result. */
    public LexerResult {
      tokens = List.copyOf(tokens);
    }
  }

  /**
   * A standalone parser definition with grammar rules only.
   *
   * @param <L> the terminal label type
   * @param version the format version
   * @param name the parser name
   * @param grammar the parsed grammar
   */
  record ParserResult<L>(int version, String name, Grammar<L> grammar) implements ParseResult<L> {}
}
