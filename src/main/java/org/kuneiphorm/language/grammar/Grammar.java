package org.kuneiphorm.language.grammar;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.kuneiphorm.daedalus.core.Expression;
import org.kuneiphorm.daedalus.core.ExpressionUnit;

/**
 * A complete grammar consisting of a start variable and an ordered list of rules.
 *
 * <p>The start variable is determined by the first rule in the grammar definition: the source of
 * the first rule is the start symbol.
 *
 * @param <L> the terminal label type
 * @param start the start non-terminal
 * @param rules the ordered list of grammar rules
 * @author Florent Guille
 * @since 0.1.0
 */
public record Grammar<L>(Variable<L> start, List<Rule<L>> rules) {

  /**
   * Creates a new grammar.
   *
   * @param start the start non-terminal
   * @param rules the ordered list of grammar rules
   */
  public Grammar {
    Objects.requireNonNull(start, "start");
    Objects.requireNonNull(rules, "rules");
    rules = List.copyOf(rules);
  }

  /**
   * Returns all rules whose source variable equals the given variable.
   *
   * @param variable the non-terminal to look up
   * @return an unmodifiable list of matching rules, in definition order
   */
  public List<Rule<L>> rulesFor(Variable<L> variable) {
    Objects.requireNonNull(variable, "variable");
    return rules.stream().filter(r -> r.source().equals(variable)).toList();
  }

  /**
   * Validates this grammar for structural correctness.
   *
   * <p>Checks performed:
   *
   * <ul>
   *   <li>The start variable must have at least one rule.
   *   <li>Every variable referenced in a production body must have at least one defining rule.
   *   <li>Every defined variable (other than the start symbol) must be reachable from the start
   *       symbol.
   * </ul>
   *
   * @throws InvalidGrammarException if any validation errors are found
   */
  public void validate() throws InvalidGrammarException {
    List<String> errors = new ArrayList<>();

    Set<Variable<L>> defined =
        rules.stream().map(Rule::source).collect(Collectors.toCollection(LinkedHashSet::new));

    // Start symbol must be defined.
    if (!defined.contains(start)) {
      errors.add("Start variable '" + start.name() + "' has no defining rule");
    }

    // All referenced variables must be defined.
    Set<Variable<L>> referenced = new LinkedHashSet<>();
    for (Rule<L> rule : rules) {
      collectVariables(rule.pattern(), referenced);
    }
    for (Variable<L> ref : referenced) {
      if (!defined.contains(ref)) {
        errors.add("Variable '" + ref.name() + "' is referenced but has no defining rule");
      }
    }

    // All defined variables must be reachable from the start symbol.
    Set<Variable<L>> reachable = new LinkedHashSet<>();
    collectReachable(start, defined, reachable);
    for (Variable<L> def : defined) {
      if (!reachable.contains(def)) {
        errors.add(
            "Variable '" + def.name() + "' is defined but unreachable from the start symbol");
      }
    }

    if (!errors.isEmpty()) {
      throw new InvalidGrammarException(errors);
    }
  }

  /**
   * Returns the set of all terminal labels referenced in the grammar rules.
   *
   * @return an unmodifiable set of terminal labels
   */
  public Set<L> terminalLabels() {
    Set<L> labels = new LinkedHashSet<>();
    for (Rule<L> rule : rules) {
      collectTerminals(rule.pattern(), labels);
    }
    return Set.copyOf(labels);
  }

  private void collectVariables(Expression<Symbol<L>> pattern, Set<Variable<L>> target) {
    Expression.unfoldPrefix(pattern)
        .filter(e -> e instanceof ExpressionUnit<?> u && u.label() instanceof Variable<?>)
        .map(e -> (Variable<L>) ((ExpressionUnit<Symbol<L>>) e).label())
        .forEach(target::add);
  }

  private void collectTerminals(Expression<Symbol<L>> pattern, Set<L> target) {
    Expression.unfoldPrefix(pattern)
        .filter(e -> e instanceof ExpressionUnit<?> u && u.label() instanceof Terminal<?>)
        .map(e -> ((Terminal<L>) ((ExpressionUnit<Symbol<L>>) e).label()).label())
        .forEach(target::add);
  }

  private void collectReachable(
      Variable<L> variable, Set<Variable<L>> defined, Set<Variable<L>> visited) {
    if (!visited.add(variable) || !defined.contains(variable)) {
      return;
    }
    for (Rule<L> rule : rulesFor(variable)) {
      Set<Variable<L>> vars = new LinkedHashSet<>();
      collectVariables(rule.pattern(), vars);
      vars.forEach(v -> collectReachable(v, defined, visited));
    }
  }
}
