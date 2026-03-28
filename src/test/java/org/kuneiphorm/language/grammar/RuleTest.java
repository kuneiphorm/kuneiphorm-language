package org.kuneiphorm.language.grammar;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.kuneiphorm.daedalus.core.Expression;

class RuleTest {

  @Test
  void fields_returnValues() {
    Variable<String> src = new Variable<>("Expr");
    Expression<Symbol<String>> pattern = Expression.unit(new Terminal<>("ID"));
    Rule<String> rule = new Rule<>("expr", src, pattern);
    assertEquals("expr", rule.name());
    assertEquals(src, rule.source());
    assertEquals(pattern, rule.pattern());
  }

  @Test
  void name_canBeNull() {
    Variable<String> src = new Variable<>("Expr");
    Expression<Symbol<String>> pattern = Expression.unit(new Terminal<>("ID"));
    Rule<String> rule = new Rule<>(null, src, pattern);
    assertNull(rule.name());
  }

  @Test
  void constructor_nullSource_throwsNpe() {
    Expression<Symbol<String>> pattern = Expression.unit(new Terminal<>("ID"));
    assertThrows(NullPointerException.class, () -> new Rule<>("name", null, pattern));
  }

  @Test
  void constructor_nullPattern_throwsNpe() {
    Variable<String> src = new Variable<>("Expr");
    assertThrows(NullPointerException.class, () -> new Rule<>("name", src, null));
  }
}
