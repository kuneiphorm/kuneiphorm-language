package org.kuneiphorm.language.token;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

class TokenDefinitionTest {

  @Test
  void fields_returnValues() {
    TokenDefinition<String> td = new TokenDefinition<>("ID", "[a-z]+", Set.of(TokenTag.SKIP));
    assertEquals("ID", td.label());
    assertEquals("[a-z]+", td.regex());
    assertEquals(Set.of(TokenTag.SKIP), td.tags());
  }

  @Test
  void constructor_nullLabel_throwsNpe() {
    assertThrows(NullPointerException.class, () -> new TokenDefinition<>(null, "[a-z]+", Set.of()));
  }

  @Test
  void constructor_nullRegex_throwsNpe() {
    assertThrows(NullPointerException.class, () -> new TokenDefinition<>("ID", null, Set.of()));
  }

  @Test
  void constructor_nullTags_throwsNpe() {
    assertThrows(NullPointerException.class, () -> new TokenDefinition<>("ID", "[a-z]+", null));
  }

  @Test
  void tags_returnsImmutableCopy() {
    TokenDefinition<String> td = new TokenDefinition<>("ID", "[a-z]+", Set.of());
    assertThrows(UnsupportedOperationException.class, () -> td.tags().add(TokenTag.SKIP));
  }

  @Test
  void hasTag_present_returnsTrue() {
    TokenDefinition<String> td = new TokenDefinition<>("WS", "[ ]+", Set.of(TokenTag.SKIP));
    assertTrue(td.hasTag(TokenTag.SKIP));
  }

  @Test
  void hasTag_absent_returnsFalse() {
    TokenDefinition<String> td = new TokenDefinition<>("ID", "[a-z]+", Set.of());
    assertFalse(td.hasTag(TokenTag.SKIP));
  }

  @Test
  void hasTag_nullTag_throwsNpe() {
    TokenDefinition<String> td = new TokenDefinition<>("ID", "[a-z]+", Set.of());
    assertThrows(NullPointerException.class, () -> td.hasTag(null));
  }
}
