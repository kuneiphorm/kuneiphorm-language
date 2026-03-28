package org.kuneiphorm.language;

import java.util.List;
import java.util.Objects;

/**
 * Thrown when a {@link Language} fails validation.
 *
 * <p>Contains a list of human-readable error messages describing all detected issues, including
 * both grammar-level and language-level validation errors.
 *
 * @author Florent Guille
 * @since 0.1.0
 */
public class InvalidLanguageException extends Exception {

  /** The list of validation error messages. */
  private final List<String> errors;

  /**
   * Creates a new invalid language exception.
   *
   * @param errors the list of validation error messages
   */
  public InvalidLanguageException(List<String> errors) {
    super(String.join("; ", errors));
    Objects.requireNonNull(errors, "errors");
    this.errors = List.copyOf(errors);
  }

  /**
   * Returns the list of validation error messages.
   *
   * @return an unmodifiable list of error messages
   */
  public List<String> getErrors() {
    return errors;
  }
}
