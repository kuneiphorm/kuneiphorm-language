package org.kuneiphorm.language;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.kuneiphorm.daedalus.core.Expression;
import org.kuneiphorm.daedalus.core.ExpressionChoice;
import org.kuneiphorm.daedalus.core.ExpressionQuantifier;
import org.kuneiphorm.daedalus.core.ExpressionSequence;
import org.kuneiphorm.daedalus.core.ExpressionUnit;
import org.kuneiphorm.language.grammar.Rule;
import org.kuneiphorm.language.grammar.Symbol;
import org.kuneiphorm.language.grammar.Terminal;
import org.kuneiphorm.language.grammar.Variable;
import org.kuneiphorm.language.token.TokenDefinition;
import org.kuneiphorm.language.token.TokenTag;

/**
 * Renders language definitions to the kuneiphorm format ({@code .kph}).
 *
 * <p>Supports rendering {@link Language}, {@link ParseResult}, and individual blocks (tokens,
 * rules). All rendering methods write to a {@link Writer} for streaming output, with convenience
 * overloads that return a {@link String}.
 *
 * @author Florent Guille
 * @since 0.1.0
 */
public final class LanguageRenderer {

  private LanguageRenderer() {}

  // -------------------------------------------------------------------------
  // String convenience overloads
  // -------------------------------------------------------------------------

  /**
   * Renders a {@code Language<String>} to a string.
   *
   * @param language the language to render
   * @return the formatted language string
   */
  public static String render(Language<String> language) {
    return render(language, Function.identity());
  }

  /**
   * Renders a language to a string using a custom label renderer.
   *
   * @param <L> the terminal label type
   * @param language the language to render
   * @param labelRenderer converts a terminal label to its text representation
   * @return the formatted language string
   */
  public static <L> String render(Language<L> language, Function<L, String> labelRenderer) {
    StringWriter sw = new StringWriter();
    try {
      render(language, labelRenderer, sw);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return sw.toString();
  }

  /**
   * Renders a {@code ParseResult<String>} to a string.
   *
   * @param result the parse result to render
   * @return the formatted definition string
   */
  public static String renderResult(ParseResult<String> result) {
    return renderResult(result, Function.identity());
  }

  /**
   * Renders a parse result to a string using a custom label renderer.
   *
   * @param <L> the terminal label type
   * @param result the parse result to render
   * @param labelRenderer converts a terminal label to its text representation
   * @return the formatted definition string
   */
  public static <L> String renderResult(ParseResult<L> result, Function<L, String> labelRenderer) {
    StringWriter sw = new StringWriter();
    try {
      renderResult(result, labelRenderer, sw);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return sw.toString();
  }

  // -------------------------------------------------------------------------
  // Writer-based rendering
  // -------------------------------------------------------------------------

  /**
   * Renders a language to the given writer.
   *
   * @param <L> the terminal label type
   * @param language the language to render
   * @param labelRenderer converts a terminal label to its text representation
   * @param writer the output writer
   * @throws IOException if an I/O error occurs
   */
  public static <L> void render(
      Language<L> language, Function<L, String> labelRenderer, Writer writer) throws IOException {
    Objects.requireNonNull(language, "language");
    Objects.requireNonNull(labelRenderer, "labelRenderer");
    Objects.requireNonNull(writer, "writer");

    writer.write("language ");
    writer.write(language.name());
    writer.write(";\n");

    renderTokensBlock(language.tokens(), labelRenderer, writer);
    renderRulesBlock(language.grammar().rules(), labelRenderer, writer);
  }

  /**
   * Renders a parse result to the given writer.
   *
   * @param <L> the terminal label type
   * @param result the parse result to render
   * @param labelRenderer converts a terminal label to its text representation
   * @param writer the output writer
   * @throws IOException if an I/O error occurs
   */
  public static <L> void renderResult(
      ParseResult<L> result, Function<L, String> labelRenderer, Writer writer) throws IOException {
    Objects.requireNonNull(result, "result");
    Objects.requireNonNull(labelRenderer, "labelRenderer");
    Objects.requireNonNull(writer, "writer");

    switch (result) {
      case ParseResult.LanguageResult<L> lr -> render(lr.language(), labelRenderer, writer);
      case ParseResult.LexerResult<L> lr -> {
        writer.write("lexer ");
        writer.write(lr.name());
        writer.write(";\n");
        renderTokensBlock(lr.tokens(), labelRenderer, writer);
      }
      case ParseResult.ParserResult<L> pr -> {
        writer.write("parser ");
        writer.write(pr.name());
        writer.write(";\n");
        renderRulesBlock(pr.grammar().rules(), labelRenderer, writer);
      }
    }
  }

  // -------------------------------------------------------------------------
  // Block rendering
  // -------------------------------------------------------------------------

  private static <L> void renderTokensBlock(
      List<TokenDefinition<L>> tokens, Function<L, String> labelRenderer, Writer writer)
      throws IOException {
    writer.write("\ntokens {\n");
    for (TokenDefinition<L> token : tokens) {
      writer.write("    ");
      renderTokenDefinition(token, writer, labelRenderer);
      writer.write('\n');
    }
    writer.write("}\n");
  }

  private static <L> void renderRulesBlock(
      List<Rule<L>> rules, Function<L, String> labelRenderer, Writer writer) throws IOException {
    writer.write("\nrules {\n");
    for (Rule<L> rule : rules) {
      writer.write("    ");
      renderRule(rule, writer, labelRenderer);
      writer.write('\n');
    }
    writer.write("}");
  }

  // -------------------------------------------------------------------------
  // Token and rule rendering
  // -------------------------------------------------------------------------

  private static <L> void renderTokenDefinition(
      TokenDefinition<L> token, Writer writer, Function<L, String> labelRenderer)
      throws IOException {
    for (TokenTag tag : token.tags()) {
      writer.write('@');
      writer.write(tag.name().toLowerCase());
      writer.write(' ');
    }
    writer.write("'");
    writer.write(labelRenderer.apply(token.label()));
    writer.write("' <- \"");
    writer.write(token.regex());
    writer.write("\";");
  }

  private static <L> void renderRule(Rule<L> rule, Writer writer, Function<L, String> labelRenderer)
      throws IOException {
    if (rule.name() != null) {
      writer.write('[');
      writer.write(rule.name());
      writer.write("] ");
    }
    writer.write(rule.source().name());
    writer.write(" => ");
    renderExpression(rule.pattern(), writer, false, labelRenderer);
    writer.write(';');
  }

  // -------------------------------------------------------------------------
  // Expression rendering
  // -------------------------------------------------------------------------

  private static <L> void renderExpression(
      Expression<Symbol<L>> expr, Writer writer, boolean wrap, Function<L, String> labelRenderer)
      throws IOException {
    switch (expr) {
      case ExpressionChoice<Symbol<L>> choice -> {
        if (wrap) writer.write('(');
        for (int i = 0; i < choice.alternatives().size(); i++) {
          if (i > 0) writer.write(" | ");
          renderExpression(choice.alternatives().get(i), writer, false, labelRenderer);
        }
        if (wrap) writer.write(')');
      }
      case ExpressionSequence<Symbol<L>> seq -> {
        if (wrap) writer.write('(');
        for (int i = 0; i < seq.elements().size(); i++) {
          if (i > 0) writer.write(' ');
          renderExpression(seq.elements().get(i), writer, false, labelRenderer);
        }
        if (wrap) writer.write(')');
      }
      case ExpressionQuantifier<Symbol<L>> q -> {
        boolean needsWrap = needsParens(q.body());
        renderExpression(q.body(), writer, needsWrap, labelRenderer);
        writer.write(
            switch (q.kind()) {
              case OPTIONAL -> '?';
              case STAR -> '*';
              case PLUS -> '+';
            });
      }
      case ExpressionUnit<Symbol<L>> unit -> {
        switch (unit.label()) {
          case Terminal<L> t -> {
            writer.write("'");
            writer.write(labelRenderer.apply(t.label()));
            writer.write("'");
          }
          case Variable<L> v -> writer.write(v.name());
        }
      }
    }
  }

  private static boolean needsParens(Expression<?> expr) {
    return expr instanceof ExpressionChoice || expr instanceof ExpressionSequence;
  }
}
