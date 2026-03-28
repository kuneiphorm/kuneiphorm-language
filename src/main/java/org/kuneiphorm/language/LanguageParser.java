package org.kuneiphorm.language;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import org.kuneiphorm.daedalus.core.Expression;
import org.kuneiphorm.language.grammar.Grammar;
import org.kuneiphorm.language.grammar.Rule;
import org.kuneiphorm.language.grammar.Symbol;
import org.kuneiphorm.language.grammar.Terminal;
import org.kuneiphorm.language.grammar.Variable;
import org.kuneiphorm.language.token.TokenDefinition;
import org.kuneiphorm.language.token.TokenTag;
import org.kuneiphorm.runtime.charflow.CharFlow;
import org.kuneiphorm.runtime.charflow.CharFlowUtils;
import org.kuneiphorm.runtime.exception.SyntaxException;
import org.kuneiphorm.runtime.exception.UnexpectedCharException;
import org.kuneiphorm.runtime.exception.UnexpectedEndOfInputException;

/**
 * Recursive-descent parser for kuneiphorm language definition files ({@code .kph}).
 *
 * <p>Supports three kinds of definitions:
 *
 * <ul>
 *   <li>{@code language <name>; tokens { ... } rules { ... }} -- full language
 *   <li>{@code lexer <name>; tokens { ... }} -- standalone lexer
 *   <li>{@code parser <name>; rules { ... }} -- standalone parser
 * </ul>
 *
 * <p>The parser accepts a {@link Function}{@code <String, L>} label mapper to convert string
 * terminal literals to typed labels, enabling generic label types.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * ParseResult<String> result = LanguageParser.parse("language Foo; tokens { ... } rules { ... }");
 * ParseResult<MyEnum> result = LanguageParser.parse(charFlow, MyEnum::valueOf);
 * }</pre>
 *
 * @author Florent Guille
 * @since 0.1.0
 */
public final class LanguageParser {

  private LanguageParser() {}

  /**
   * Parses a language definition from a string, using string labels directly.
   *
   * @param input the language definition string
   * @return the parse result
   * @throws SyntaxException if the input is malformed
   */
  public static ParseResult<String> parse(String input) throws SyntaxException {
    return parse(input, Function.identity());
  }

  /**
   * Parses a language definition from a string with a label mapper.
   *
   * @param <L> the terminal label type
   * @param input the language definition string
   * @param labelMapper converts terminal string literals to typed labels
   * @return the parse result
   * @throws SyntaxException if the input is malformed
   */
  public static <L> ParseResult<L> parse(String input, Function<String, L> labelMapper)
      throws SyntaxException {
    Objects.requireNonNull(input, "input");
    try {
      CharFlow flow = new CharFlow(new StringReader(input));
      return parse(flow, labelMapper);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Parses a language definition from a {@link CharFlow} with a label mapper.
   *
   * @param <L> the terminal label type
   * @param flow the character flow to read from
   * @param labelMapper converts terminal string literals to typed labels
   * @return the parse result
   * @throws IOException if an I/O error occurs
   * @throws SyntaxException if the input is malformed
   */
  public static <L> ParseResult<L> parse(CharFlow flow, Function<String, L> labelMapper)
      throws IOException, SyntaxException {
    Objects.requireNonNull(flow, "flow");
    Objects.requireNonNull(labelMapper, "labelMapper");

    CharFlowUtils.skipTrivia(flow);
    String keyword = readIdentifier(flow);

    CharFlowUtils.skipTrivia(flow);
    String name = readIdentifier(flow);

    CharFlowUtils.skipTrivia(flow);
    flow.expect(';');

    return switch (keyword) {
      case "language" -> parseLanguage(flow, name, labelMapper);
      case "lexer" -> parseLexer(flow, name, labelMapper);
      case "parser" -> parseParser(flow, name, labelMapper);
      default ->
          throw new UnexpectedCharException(
              flow.getName(), flow.getLine(), flow.getColumn(), 'l', keyword.charAt(0));
    };
  }

  // ---------------------------------------------------------------------------
  // Top-level definitions
  // ---------------------------------------------------------------------------

  private static <L> ParseResult<L> parseLanguage(
      CharFlow flow, String name, Function<String, L> labelMapper)
      throws IOException, SyntaxException {
    CharFlowUtils.skipTrivia(flow);
    expectKeyword(flow, "tokens");
    List<TokenDefinition<L>> tokens = parseTokensBlock(flow, labelMapper);

    CharFlowUtils.skipTrivia(flow);
    expectKeyword(flow, "rules");
    List<Rule<L>> rules = parseRulesBlock(flow, labelMapper);

    Variable<L> start = rules.get(0).source();
    Grammar<L> grammar = new Grammar<>(start, rules);
    Language<L> language = new Language<>(name, grammar, tokens);
    return new ParseResult.LanguageResult<>(language);
  }

  private static <L> ParseResult<L> parseLexer(
      CharFlow flow, String name, Function<String, L> labelMapper)
      throws IOException, SyntaxException {
    CharFlowUtils.skipTrivia(flow);
    expectKeyword(flow, "tokens");
    List<TokenDefinition<L>> tokens = parseTokensBlock(flow, labelMapper);
    return new ParseResult.LexerResult<>(name, tokens);
  }

  private static <L> ParseResult<L> parseParser(
      CharFlow flow, String name, Function<String, L> labelMapper)
      throws IOException, SyntaxException {
    CharFlowUtils.skipTrivia(flow);
    expectKeyword(flow, "rules");
    List<Rule<L>> rules = parseRulesBlock(flow, labelMapper);
    Variable<L> start = rules.get(0).source();
    Grammar<L> grammar = new Grammar<>(start, rules);
    return new ParseResult.ParserResult<>(name, grammar);
  }

  // ---------------------------------------------------------------------------
  // Tokens block: tokens { ... }
  // ---------------------------------------------------------------------------

  private static <L> List<TokenDefinition<L>> parseTokensBlock(
      CharFlow flow, Function<String, L> labelMapper) throws IOException, SyntaxException {
    CharFlowUtils.skipTrivia(flow);
    flow.expect('{');

    List<TokenDefinition<L>> tokens = new ArrayList<>();
    CharFlowUtils.skipTrivia(flow);
    while (flow.hasMore() && flow.peek() != '}') {
      tokens.add(parseTokenDefinition(flow, labelMapper));
      CharFlowUtils.skipTrivia(flow);
    }

    flow.expect('}');
    return tokens;
  }

  private static <L> TokenDefinition<L> parseTokenDefinition(
      CharFlow flow, Function<String, L> labelMapper) throws IOException, SyntaxException {
    // Optional tags: @skip
    Set<TokenTag> tags = new LinkedHashSet<>();
    while (flow.peek() == '@') {
      flow.next();
      String tagName = readIdentifier(flow);
      TokenTag tag = parseTokenTag(tagName, flow);
      tags.add(tag);
      CharFlowUtils.skipTrivia(flow);
    }

    // Label: 'NAME'
    flow.expect('\'');
    String labelText = readUntil(flow, '\'');
    flow.expect('\'');
    L label = labelMapper.apply(labelText);

    // Arrow: <-
    CharFlowUtils.skipTrivia(flow);
    flow.expect('<');
    flow.expect('-');

    // Regex: "pattern"
    CharFlowUtils.skipTrivia(flow);
    flow.expect('"');
    String regex = readUntil(flow, '"');
    flow.expect('"');

    // Semicolon
    CharFlowUtils.skipTrivia(flow);
    flow.expect(';');

    return new TokenDefinition<>(label, regex, tags);
  }

  private static TokenTag parseTokenTag(String name, CharFlow flow) throws SyntaxException {
    return switch (name) {
      case "skip" -> TokenTag.SKIP;
      default ->
          throw new UnexpectedCharException(
              flow.getName(), flow.getLine(), flow.getColumn(), '@', name.charAt(0));
    };
  }

  // ---------------------------------------------------------------------------
  // Rules block: rules { ... }
  // ---------------------------------------------------------------------------

  private static <L> List<Rule<L>> parseRulesBlock(CharFlow flow, Function<String, L> labelMapper)
      throws IOException, SyntaxException {
    CharFlowUtils.skipTrivia(flow);
    flow.expect('{');

    List<Rule<L>> rules = new ArrayList<>();
    CharFlowUtils.skipTrivia(flow);
    while (flow.hasMore() && flow.peek() != '}') {
      rules.add(parseRule(flow, labelMapper));
      CharFlowUtils.skipTrivia(flow);
    }

    flow.expect('}');
    return rules;
  }

  private static <L> Rule<L> parseRule(CharFlow flow, Function<String, L> labelMapper)
      throws IOException, SyntaxException {
    // Optional semantic name: [name]
    String name = null;
    if (flow.accept('[')) {
      name = readUntil(flow, ']');
      flow.expect(']');
      CharFlowUtils.skipTrivia(flow);
    }

    // Source variable
    String sourceName = readIdentifier(flow);
    Variable<L> source = new Variable<>(sourceName);

    // Arrow: =>
    CharFlowUtils.skipTrivia(flow);
    flow.expect('=');
    flow.expect('>');

    // Pattern expression
    CharFlowUtils.skipTrivia(flow);
    Expression<Symbol<L>> pattern = parseExpression(flow, labelMapper);

    // Semicolon
    CharFlowUtils.skipTrivia(flow);
    flow.expect(';');

    return new Rule<>(name, source, pattern);
  }

  // ---------------------------------------------------------------------------
  // Expression parsing: E => T ('|' T)*
  // ---------------------------------------------------------------------------

  private static <L> Expression<Symbol<L>> parseExpression(
      CharFlow flow, Function<String, L> labelMapper) throws IOException, SyntaxException {
    List<Expression<Symbol<L>>> alternatives = new ArrayList<>();
    alternatives.add(parseTerm(flow, labelMapper));

    while (flow.hasMore()) {
      CharFlowUtils.skipTrivia(flow);
      if (!flow.accept('|')) {
        break;
      }
      CharFlowUtils.skipTrivia(flow);
      alternatives.add(parseTerm(flow, labelMapper));
    }

    if (alternatives.size() == 1) {
      return alternatives.get(0);
    }
    return Expression.choice(alternatives);
  }

  // ---------------------------------------------------------------------------
  // Term: F+
  // ---------------------------------------------------------------------------

  private static <L> Expression<Symbol<L>> parseTerm(CharFlow flow, Function<String, L> labelMapper)
      throws IOException, SyntaxException {
    List<Expression<Symbol<L>>> elements = new ArrayList<>();

    while (flow.hasMore()) {
      int ch = flow.peek();
      if (ch == '|' || ch == ')' || ch == ';') {
        break;
      }
      elements.add(parseFactor(flow, labelMapper));
      CharFlowUtils.skipTrivia(flow);
    }

    if (elements.isEmpty()) {
      return Expression.sequence();
    }
    if (elements.size() == 1) {
      return elements.get(0);
    }
    return Expression.sequence(elements);
  }

  // ---------------------------------------------------------------------------
  // Factor: B ('?' | '+' | '*')?
  // ---------------------------------------------------------------------------

  private static <L> Expression<Symbol<L>> parseFactor(
      CharFlow flow, Function<String, L> labelMapper) throws IOException, SyntaxException {
    Expression<Symbol<L>> base = parseBase(flow, labelMapper);

    CharFlowUtils.skipTrivia(flow);
    if (flow.hasMore()) {
      if (flow.accept('?')) {
        return Expression.optional(base);
      }
      if (flow.accept('+')) {
        return Expression.plus(base);
      }
      if (flow.accept('*')) {
        return Expression.star(base);
      }
    }

    return base;
  }

  // ---------------------------------------------------------------------------
  // Base: '(' E ')' | 'label' | Variable
  // ---------------------------------------------------------------------------

  private static <L> Expression<Symbol<L>> parseBase(CharFlow flow, Function<String, L> labelMapper)
      throws IOException, SyntaxException {
    if (flow.accept('(')) {
      CharFlowUtils.skipTrivia(flow);
      Expression<Symbol<L>> expr = parseExpression(flow, labelMapper);
      CharFlowUtils.skipTrivia(flow);
      flow.expect(')');
      return expr;
    }

    if (flow.peek() == '\'') {
      flow.next();
      String labelText = readUntil(flow, '\'');
      flow.expect('\'');
      L label = labelMapper.apply(labelText);
      return Expression.unit(new Terminal<>(label));
    }

    // Variable reference
    String varName = readIdentifier(flow);
    return Expression.unit(new Variable<>(varName));
  }

  // ---------------------------------------------------------------------------
  // Lexical helpers
  // ---------------------------------------------------------------------------

  private static String readIdentifier(CharFlow flow) throws IOException, SyntaxException {
    if (!flow.hasMore() || !isIdentStart(flow.peek())) {
      throw new UnexpectedCharException(
          flow.getName(),
          flow.getLine(),
          flow.getColumn(),
          'a',
          flow.hasMore() ? (char) flow.peek() : '?');
    }
    StringBuilder sb = new StringBuilder();
    while (flow.hasMore() && isIdentPart(flow.peek())) {
      sb.append((char) flow.next());
    }
    return sb.toString();
  }

  private static String readUntil(CharFlow flow, int delimiter)
      throws IOException, SyntaxException {
    StringBuilder sb = new StringBuilder();
    while (flow.hasMore() && flow.peek() != delimiter) {
      int ch = flow.next();
      if (ch == '\\' && flow.hasMore()) {
        sb.append((char) ch);
        sb.append((char) flow.next());
      } else {
        sb.append((char) ch);
      }
    }
    if (!flow.hasMore()) {
      throw new UnexpectedEndOfInputException(flow.getName(), flow.getLine(), flow.getColumn());
    }
    return sb.toString();
  }

  private static void expectKeyword(CharFlow flow, String keyword)
      throws IOException, SyntaxException {
    String actual = readIdentifier(flow);
    if (!actual.equals(keyword)) {
      throw new UnexpectedCharException(
          flow.getName(), flow.getLine(), flow.getColumn(), keyword.charAt(0), actual.charAt(0));
    }
  }

  private static boolean isIdentStart(int ch) {
    return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == '_';
  }

  private static boolean isIdentPart(int ch) {
    return isIdentStart(ch) || (ch >= '0' && ch <= '9');
  }
}
