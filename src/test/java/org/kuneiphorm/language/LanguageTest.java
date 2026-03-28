package org.kuneiphorm.language;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.kuneiphorm.daedalus.core.Expression;
import org.kuneiphorm.language.grammar.Grammar;
import org.kuneiphorm.language.grammar.Rule;
import org.kuneiphorm.language.grammar.Symbol;
import org.kuneiphorm.language.grammar.Terminal;
import org.kuneiphorm.language.grammar.Variable;
import org.kuneiphorm.language.token.TokenDefinition;
import org.kuneiphorm.language.token.TokenTag;

class LanguageTest {

  // --- Helpers ---

  private static Variable<String> var(String name) {
    return new Variable<>(name);
  }

  private static Expression<Symbol<String>> term(String label) {
    return Expression.unit(new Terminal<>(label));
  }

  private static Rule<String> rule(String source, Expression<Symbol<String>> pattern) {
    return new Rule<>(null, var(source), pattern);
  }

  private static TokenDefinition<String> token(String label, String regex) {
    return new TokenDefinition<>(label, regex, Set.of());
  }

  private static TokenDefinition<String> skipToken(String label, String regex) {
    return new TokenDefinition<>(label, regex, Set.of(TokenTag.SKIP));
  }

  // --- Constructor ---

  @Test
  void constructor_nullName_throwsNpe() {
    Grammar<String> g = new Grammar<>(var("S"), List.of(rule("S", term("a"))));
    assertThrows(
        NullPointerException.class, () -> new Language<>(null, g, List.of(token("a", "a"))));
  }

  @Test
  void constructor_nullGrammar_throwsNpe() {
    assertThrows(
        NullPointerException.class, () -> new Language<>("Test", null, List.of(token("a", "a"))));
  }

  @Test
  void constructor_nullTokens_throwsNpe() {
    Grammar<String> g = new Grammar<>(var("S"), List.of(rule("S", term("a"))));
    assertThrows(NullPointerException.class, () -> new Language<>("Test", g, null));
  }

  @Test
  void tokens_returnsImmutableCopy() {
    Grammar<String> g = new Grammar<>(var("S"), List.of(rule("S", term("a"))));
    Language<String> lang = new Language<>("Test", g, List.of(token("a", "a")));
    assertThrows(UnsupportedOperationException.class, () -> lang.tokens().add(token("b", "b")));
  }

  // --- validate: valid language ---

  @Test
  void validate_validLanguage_doesNotThrow() {
    Grammar<String> g =
        new Grammar<>(var("S"), List.of(rule("S", Expression.sequence(term("ID"), term("NUM")))));
    Language<String> lang =
        new Language<>("Test", g, List.of(token("ID", "[a-z]+"), token("NUM", "[0-9]+")));
    assertDoesNotThrow(lang::validate);
  }

  @Test
  void validate_validWithSkipToken_doesNotThrow() {
    Grammar<String> g = new Grammar<>(var("S"), List.of(rule("S", term("ID"))));
    Language<String> lang =
        new Language<>("Test", g, List.of(token("ID", "[a-z]+"), skipToken("WS", "[ ]+")));
    assertDoesNotThrow(lang::validate);
  }

  // --- validate: duplicate tokens ---

  @Test
  void validate_duplicateToken_throwsWithMessage() {
    Grammar<String> g = new Grammar<>(var("S"), List.of(rule("S", term("ID"))));
    Language<String> lang =
        new Language<>("Test", g, List.of(token("ID", "[a-z]+"), token("ID", "[A-Z]+")));
    InvalidLanguageException ex = assertThrows(InvalidLanguageException.class, lang::validate);
    assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("Duplicate") && e.contains("ID")));
  }

  // --- validate: terminal without token ---

  @Test
  void validate_terminalWithoutToken_throwsWithMessage() {
    Grammar<String> g = new Grammar<>(var("S"), List.of(rule("S", term("MISSING"))));
    Language<String> lang = new Language<>("Test", g, List.of());
    InvalidLanguageException ex = assertThrows(InvalidLanguageException.class, lang::validate);
    assertTrue(
        ex.getErrors().stream()
            .anyMatch(e -> e.contains("MISSING") && e.contains("no token definition")));
  }

  // --- validate: unreferenced non-skip token ---

  @Test
  void validate_unreferencedNonSkipToken_throwsWithMessage() {
    Grammar<String> g = new Grammar<>(var("S"), List.of(rule("S", term("ID"))));
    Language<String> lang =
        new Language<>("Test", g, List.of(token("ID", "[a-z]+"), token("UNUSED", "x")));
    InvalidLanguageException ex = assertThrows(InvalidLanguageException.class, lang::validate);
    assertTrue(
        ex.getErrors().stream()
            .anyMatch(e -> e.contains("UNUSED") && e.contains("not referenced")));
  }

  @Test
  void validate_unreferencedSkipToken_doesNotThrow() {
    Grammar<String> g = new Grammar<>(var("S"), List.of(rule("S", term("ID"))));
    Language<String> lang =
        new Language<>("Test", g, List.of(token("ID", "[a-z]+"), skipToken("WS", "[ ]+")));
    assertDoesNotThrow(lang::validate);
  }

  // --- validate: grammar errors propagate ---

  @Test
  void validate_grammarErrors_propagated() {
    Grammar<String> g = new Grammar<>(var("S"), List.of(rule("T", term("a"))));
    Language<String> lang = new Language<>("Test", g, List.of(token("a", "a")));
    InvalidLanguageException ex = assertThrows(InvalidLanguageException.class, lang::validate);
    assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("Start variable")));
  }

  // --- validate: multiple errors accumulated ---

  @Test
  void validate_multipleErrors_allReported() {
    Grammar<String> g =
        new Grammar<>(var("S"), List.of(rule("S", Expression.sequence(term("A"), term("B")))));
    // A has no token, B has no token, EXTRA is unreferenced
    Language<String> lang = new Language<>("Test", g, List.of(token("EXTRA", "x")));
    InvalidLanguageException ex = assertThrows(InvalidLanguageException.class, lang::validate);
    assertTrue(ex.getErrors().size() >= 3);
  }
}
