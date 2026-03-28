package org.kuneiphorm.language.grammar;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class InvalidGrammarExceptionTest {

  @Test
  void getErrors_returnsAllErrors() {
    InvalidGrammarException ex = new InvalidGrammarException(List.of("err1", "err2"));
    assertEquals(List.of("err1", "err2"), ex.getErrors());
  }

  @Test
  void message_joinsSemicolon() {
    InvalidGrammarException ex = new InvalidGrammarException(List.of("err1", "err2"));
    assertEquals("err1; err2", ex.getMessage());
  }

  @Test
  void getErrors_returnsImmutable() {
    InvalidGrammarException ex = new InvalidGrammarException(List.of("err1"));
    assertThrows(UnsupportedOperationException.class, () -> ex.getErrors().add("x"));
  }

  @Test
  void constructor_nullErrors_throwsNpe() {
    assertThrows(NullPointerException.class, () -> new InvalidGrammarException(null));
  }
}
