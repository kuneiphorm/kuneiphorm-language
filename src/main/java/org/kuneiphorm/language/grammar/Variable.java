package org.kuneiphorm.language.grammar;

import java.util.Objects;

/**
 * A non-terminal symbol in a grammar, such as {@code Expr} or {@code Stmt}.
 *
 * <p>Variables are identified by name only and do not carry a typed label. The type parameter
 * {@code L} is a phantom type that ensures type consistency with {@link Terminal}{@code <L>} in the
 * same {@link Symbol}{@code <L>} hierarchy.
 *
 * @param <L> the terminal label type (phantom -- not used by this record)
 * @param name the variable name
 * @author Florent Guille
 * @since 0.1.0
 */
public record Variable<L>(String name) implements Symbol<L> {

  /**
   * Creates a new variable symbol.
   *
   * @param name the variable name
   */
  public Variable {
    Objects.requireNonNull(name, "name");
  }

  @Override
  public String toString() {
    return name;
  }
}
