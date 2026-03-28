package org.kuneiphorm.language.grammar;

import java.util.Objects;

/**
 * A terminal symbol in a grammar, representing a token label such as {@code 'ID'} or {@code '+'}.
 *
 * <p>Each terminal carries a typed label of type {@code L}, produced by the label mapper during
 * parsing. The label is opaque to the grammar module; its meaning is defined by downstream modules
 * (lexer, parser generation).
 *
 * @param <L> the terminal label type
 * @param label the typed terminal label
 * @author Florent Guille
 * @since 0.1.0
 */
public record Terminal<L>(L label) implements Symbol<L> {

  /**
   * Creates a new terminal symbol.
   *
   * @param label the typed terminal label
   */
  public Terminal {
    Objects.requireNonNull(label, "label");
  }

  @Override
  public String toString() {
    return "'" + label + "'";
  }
}
