package org.kuneiphorm.language.grammar;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TerminalTest {

  @Test
  void label_returnsValue() {
    Terminal<String> t = new Terminal<>("ID");
    assertEquals("ID", t.label());
  }

  @Test
  void constructor_nullLabel_throwsNpe() {
    assertThrows(NullPointerException.class, () -> new Terminal<>(null));
  }

  @Test
  void toString_wrapsInQuotes() {
    Terminal<String> t = new Terminal<>("PLUS");
    assertEquals("'PLUS'", t.toString());
  }

  @Test
  void equality_sameLabel_areEqual() {
    assertEquals(new Terminal<>("ID"), new Terminal<>("ID"));
  }

  @Test
  void equality_differentLabel_areNotEqual() {
    assertNotEquals(new Terminal<>("ID"), new Terminal<>("NUM"));
  }

  @Test
  void implementsSymbol() {
    Symbol<String> symbol = new Terminal<>("ID");
    assertInstanceOf(Terminal.class, symbol);
  }
}
