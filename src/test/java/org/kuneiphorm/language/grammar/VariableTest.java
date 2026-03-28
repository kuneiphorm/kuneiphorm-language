package org.kuneiphorm.language.grammar;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class VariableTest {

  @Test
  void name_returnsValue() {
    Variable<String> v = new Variable<>("Expr");
    assertEquals("Expr", v.name());
  }

  @Test
  void constructor_nullName_throwsNpe() {
    assertThrows(NullPointerException.class, () -> new Variable<>(null));
  }

  @Test
  void toString_returnsName() {
    Variable<String> v = new Variable<>("Stmt");
    assertEquals("Stmt", v.toString());
  }

  @Test
  void equality_sameName_areEqual() {
    assertEquals(new Variable<>("Expr"), new Variable<>("Expr"));
  }

  @Test
  void equality_differentName_areNotEqual() {
    assertNotEquals(new Variable<>("Expr"), new Variable<>("Stmt"));
  }

  @Test
  void implementsSymbol() {
    Symbol<String> symbol = new Variable<>("Expr");
    assertInstanceOf(Variable.class, symbol);
  }
}
