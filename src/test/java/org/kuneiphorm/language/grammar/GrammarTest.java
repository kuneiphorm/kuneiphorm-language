package org.kuneiphorm.language.grammar;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.kuneiphorm.daedalus.core.Expression;

class GrammarTest {

  // --- Helpers ---

  private static Variable<String> var(String name) {
    return new Variable<>(name);
  }

  private static Expression<Symbol<String>> term(String label) {
    return Expression.unit(new Terminal<>(label));
  }

  private static Expression<Symbol<String>> ref(String name) {
    return Expression.unit(new Variable<>(name));
  }

  private static Rule<String> rule(String source, Expression<Symbol<String>> pattern) {
    return new Rule<>(null, var(source), pattern);
  }

  // --- Constructor ---

  @Test
  void constructor_nullStart_throwsNpe() {
    assertThrows(
        NullPointerException.class, () -> new Grammar<>(null, List.of(rule("S", term("a")))));
  }

  @Test
  void constructor_nullRules_throwsNpe() {
    assertThrows(NullPointerException.class, () -> new Grammar<>(var("S"), null));
  }

  @Test
  void rules_returnsImmutableCopy() {
    Grammar<String> g = new Grammar<>(var("S"), List.of(rule("S", term("a"))));
    assertThrows(UnsupportedOperationException.class, () -> g.rules().add(rule("S", term("b"))));
  }

  // --- rulesFor ---

  @Test
  void rulesFor_returnsMatchingRules() {
    Rule<String> r1 = rule("S", term("a"));
    Rule<String> r2 = rule("S", term("b"));
    Rule<String> r3 = rule("T", term("c"));
    Grammar<String> g = new Grammar<>(var("S"), List.of(r1, r2, r3));

    List<Rule<String>> result = g.rulesFor(var("S"));
    assertEquals(2, result.size());
    assertEquals(r1, result.get(0));
    assertEquals(r2, result.get(1));
  }

  @Test
  void rulesFor_noMatch_returnsEmpty() {
    Grammar<String> g = new Grammar<>(var("S"), List.of(rule("S", term("a"))));
    assertTrue(g.rulesFor(var("X")).isEmpty());
  }

  @Test
  void rulesFor_nullVariable_throwsNpe() {
    Grammar<String> g = new Grammar<>(var("S"), List.of(rule("S", term("a"))));
    assertThrows(NullPointerException.class, () -> g.rulesFor(null));
  }

  // --- terminalLabels ---

  @Test
  void terminalLabels_collectsAllTerminals() {
    Grammar<String> g =
        new Grammar<>(
            var("S"),
            List.of(
                rule("S", Expression.sequence(term("ID"), term("PLUS"), ref("S"))),
                rule("S", term("NUM"))));
    assertEquals(Set.of("ID", "PLUS", "NUM"), g.terminalLabels());
  }

  @Test
  void terminalLabels_noTerminals_returnsEmpty() {
    Grammar<String> g = new Grammar<>(var("S"), List.of(rule("S", ref("S"))));
    assertTrue(g.terminalLabels().isEmpty());
  }

  // --- validate: start symbol ---

  @Test
  void validate_startDefined_doesNotThrow() {
    Grammar<String> g = new Grammar<>(var("S"), List.of(rule("S", term("a"))));
    assertDoesNotThrow(g::validate);
  }

  @Test
  void validate_startNotDefined_throwsWithMessage() {
    Grammar<String> g = new Grammar<>(var("S"), List.of(rule("T", term("a"))));
    InvalidGrammarException ex = assertThrows(InvalidGrammarException.class, g::validate);
    assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("Start variable")));
  }

  // --- validate: undefined variables ---

  @Test
  void validate_undefinedVariable_throwsWithMessage() {
    Grammar<String> g = new Grammar<>(var("S"), List.of(rule("S", ref("T"))));
    InvalidGrammarException ex = assertThrows(InvalidGrammarException.class, g::validate);
    assertTrue(
        ex.getErrors().stream().anyMatch(e -> e.contains("'T'") && e.contains("no defining")));
  }

  @Test
  void validate_allVariablesDefined_doesNotThrow() {
    Grammar<String> g = new Grammar<>(var("S"), List.of(rule("S", ref("T")), rule("T", term("a"))));
    assertDoesNotThrow(g::validate);
  }

  // --- validate: unreachable variables ---

  @Test
  void validate_unreachableVariable_throwsWithMessage() {
    Grammar<String> g =
        new Grammar<>(var("S"), List.of(rule("S", term("a")), rule("T", term("b"))));
    InvalidGrammarException ex = assertThrows(InvalidGrammarException.class, g::validate);
    assertTrue(
        ex.getErrors().stream().anyMatch(e -> e.contains("'T'") && e.contains("unreachable")));
  }

  @Test
  void validate_allReachable_doesNotThrow() {
    Grammar<String> g =
        new Grammar<>(
            var("S"),
            List.of(
                rule("S", Expression.sequence(ref("A"), ref("B"))),
                rule("A", term("a")),
                rule("B", term("b"))));
    assertDoesNotThrow(g::validate);
  }

  // --- validate: multiple errors ---

  @Test
  void validate_multipleErrors_allReported() {
    Grammar<String> g = new Grammar<>(var("S"), List.of(rule("A", ref("B")), rule("C", term("x"))));
    InvalidGrammarException ex = assertThrows(InvalidGrammarException.class, g::validate);
    // Start not defined, B undefined, A and C unreachable (if S has no rules)
    assertTrue(ex.getErrors().size() >= 2);
  }

  // --- validate: transitive reachability ---

  @Test
  void validate_transitiveReachability_doesNotThrow() {
    // S -> A, A -> B, B -> terminal
    Grammar<String> g =
        new Grammar<>(
            var("S"), List.of(rule("S", ref("A")), rule("A", ref("B")), rule("B", term("x"))));
    assertDoesNotThrow(g::validate);
  }

  // --- validate: recursive grammar ---

  @Test
  void validate_directRecursion_doesNotThrow() {
    Grammar<String> g =
        new Grammar<>(
            var("S"),
            List.of(rule("S", Expression.sequence(term("a"), ref("S"))), rule("S", term("a"))));
    assertDoesNotThrow(g::validate);
  }

  @Test
  void validate_mutualRecursion_doesNotThrow() {
    Grammar<String> g =
        new Grammar<>(
            var("S"),
            List.of(
                rule("S", ref("A")),
                rule("A", ref("B")),
                rule("B", ref("A")),
                rule("B", term("x"))));
    assertDoesNotThrow(g::validate);
  }
}
