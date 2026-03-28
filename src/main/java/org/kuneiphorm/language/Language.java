package org.kuneiphorm.language;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.kuneiphorm.language.grammar.Grammar;
import org.kuneiphorm.language.grammar.InvalidGrammarException;
import org.kuneiphorm.language.token.TokenDefinition;
import org.kuneiphorm.language.token.TokenTag;

/**
 * A complete language definition consisting of a name, a grammar, and a list of token definitions.
 *
 * <p>A language combines the syntactic structure (grammar rules) with the lexical specification
 * (token definitions) into a single unit that fully describes how to lex and parse input.
 *
 * @param <L> the terminal label type
 * @param name the language name
 * @param grammar the grammar rules
 * @param tokens the token definitions
 * @author Florent Guille
 * @since 0.1.0
 */
public record Language<L>(String name, Grammar<L> grammar, List<TokenDefinition<L>> tokens) {

  /**
   * Creates a new language.
   *
   * @param name the language name
   * @param grammar the grammar rules
   * @param tokens the token definitions
   */
  public Language {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(grammar, "grammar");
    Objects.requireNonNull(tokens, "tokens");
    tokens = List.copyOf(tokens);
  }

  /**
   * Validates this language for structural correctness.
   *
   * <p>Checks performed:
   *
   * <ul>
   *   <li>All grammar validations (start symbol defined, no undefined variables, no unreachable
   *       variables).
   *   <li>Every terminal referenced in the grammar must have a corresponding token definition.
   *   <li>Every non-{@link TokenTag#SKIP} token definition must be referenced by at least one
   *       grammar rule.
   *   <li>No duplicate token definitions (each label must appear exactly once).
   * </ul>
   *
   * @throws InvalidLanguageException if any validation errors are found
   */
  public void validate() throws InvalidLanguageException {
    List<String> errors = new ArrayList<>();

    // Grammar validation.
    try {
      grammar.validate();
    } catch (InvalidGrammarException e) {
      errors.addAll(e.getErrors());
    }

    // Collect defined token labels.
    Set<L> definedLabels = new LinkedHashSet<>();
    for (TokenDefinition<L> token : tokens) {
      if (!definedLabels.add(token.label())) {
        errors.add("Duplicate token definition for '" + token.label() + "'");
      }
    }

    // Every terminal in the grammar must have a token definition.
    Set<L> referencedTerminals = grammar.terminalLabels();
    for (L terminal : referencedTerminals) {
      if (!definedLabels.contains(terminal)) {
        errors.add("Terminal '" + terminal + "' is used in grammar but has no token definition");
      }
    }

    // Every non-skip token must be referenced in the grammar.
    Set<L> nonSkipLabels =
        tokens.stream()
            .filter(t -> !t.hasTag(TokenTag.SKIP))
            .map(TokenDefinition::label)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    for (L label : nonSkipLabels) {
      if (!referencedTerminals.contains(label)) {
        errors.add("Token '" + label + "' is defined but not referenced in any grammar rule");
      }
    }

    if (!errors.isEmpty()) {
      throw new InvalidLanguageException(errors);
    }
  }
}
