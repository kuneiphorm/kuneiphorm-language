package org.kuneiphorm.language.token;

import java.util.Objects;
import java.util.Set;

/**
 * A token definition mapping a label to a regex pattern, with optional tags.
 *
 * <p>Token definitions appear in the {@code tokens} block of a language file:
 *
 * <pre>
 *   tokens {
 *       {@literal @}skip 'WS' {@literal <-} "[ \t\r\n]+";
 *       'ID' {@literal <-} "[_a-zA-Z][_a-zA-Z0-9]*";
 *   }
 * </pre>
 *
 * <p>The label type {@code L} matches the terminal label type used in the grammar. The regex is
 * stored as a raw string; compilation to automata is the responsibility of downstream modules.
 *
 * @param <L> the terminal label type
 * @param label the token label
 * @param regex the regex pattern defining this token
 * @param tags the set of tags applied to this token definition
 * @author Florent Guille
 * @since 0.1.0
 */
public record TokenDefinition<L>(L label, String regex, Set<TokenTag> tags) {

  /**
   * Creates a new token definition.
   *
   * @param label the token label
   * @param regex the regex pattern
   * @param tags the set of tags
   */
  public TokenDefinition {
    Objects.requireNonNull(label, "label");
    Objects.requireNonNull(regex, "regex");
    Objects.requireNonNull(tags, "tags");
    tags = Set.copyOf(tags);
  }

  /**
   * Returns whether this token definition has the given tag.
   *
   * @param tag the tag to check
   * @return {@code true} if this definition has the tag
   */
  public boolean hasTag(TokenTag tag) {
    Objects.requireNonNull(tag, "tag");
    return tags.contains(tag);
  }
}
