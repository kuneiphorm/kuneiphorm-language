package org.kuneiphorm.language;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class InvalidLanguageExceptionTest {

  @Test
  void getErrors_returnsAllErrors() {
    InvalidLanguageException ex = new InvalidLanguageException(List.of("err1", "err2"));
    assertEquals(List.of("err1", "err2"), ex.getErrors());
  }

  @Test
  void message_joinsSemicolon() {
    InvalidLanguageException ex = new InvalidLanguageException(List.of("err1", "err2"));
    assertEquals("err1; err2", ex.getMessage());
  }

  @Test
  void getErrors_returnsImmutable() {
    InvalidLanguageException ex = new InvalidLanguageException(List.of("err1"));
    assertThrows(UnsupportedOperationException.class, () -> ex.getErrors().add("x"));
  }

  @Test
  void constructor_nullErrors_throwsNpe() {
    assertThrows(NullPointerException.class, () -> new InvalidLanguageException(null));
  }
}
