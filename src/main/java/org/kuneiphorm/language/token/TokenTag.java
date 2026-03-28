package org.kuneiphorm.language.token;

/**
 * Tags that can be applied to token definitions to control their behavior.
 *
 * <p>Tags are annotations on token definitions in the language file format (e.g. {@code @skip}).
 * Downstream modules interpret tags to configure lexer behavior.
 *
 * @author Florent Guille
 * @since 0.1.0
 */
public enum TokenTag {

  /** Marks a token to be skipped (filtered out) during parsing. */
  SKIP
}
