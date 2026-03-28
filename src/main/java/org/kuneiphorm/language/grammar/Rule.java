package org.kuneiphorm.language.grammar;

import java.util.Objects;
import org.kuneiphorm.daedalus.core.Expression;

/**
 * A single grammar rule mapping a source non-terminal to a production pattern.
 *
 * <p>Rules may have an optional semantic name (e.g. {@code [inst]}) used by downstream modules to
 * label reductions. The production body is an {@link Expression}{@code <}{@link Symbol}{@code <L>>}
 * tree supporting alternation, sequence, and quantifiers.
 *
 * @param <L> the terminal label type
 * @param name the semantic name of the rule, or {@code null} if unnamed
 * @param source the non-terminal on the left-hand side
 * @param pattern the production body
 * @author Florent Guille
 * @since 0.1.0
 */
public record Rule<L>(String name, Variable<L> source, Expression<Symbol<L>> pattern) {

  /**
   * Creates a new grammar rule.
   *
   * @param name the semantic name, or {@code null}
   * @param source the left-hand side non-terminal
   * @param pattern the production body
   */
  public Rule {
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(pattern, "pattern");
  }
}
