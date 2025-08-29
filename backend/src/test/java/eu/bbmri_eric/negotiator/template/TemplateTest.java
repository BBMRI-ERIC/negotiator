package eu.bbmri_eric.negotiator.template;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class TemplateTest {

  @Test
  void builder_CreatesValidEntity() {
    LocalDateTime now = LocalDateTime.now();

    Template entity =
        Template.builder()
            .id(1L)
            .name("test-template")
            .content("<html><body>Test template</body></html>")
            .updatedAt(now)
            .build();

    assertEquals(1L, entity.getId());
    assertEquals("test-template", entity.getName());
    assertEquals("<html><body>Test template</body></html>", entity.getContent());
    assertTrue(java.time.Duration.between(now, entity.getUpdatedAt()).abs().getSeconds() <= 5);
  }

  @Test
  void noArgsConstructor_CreatesEmptyEntity() {
    Template entity = new Template();

    assertNull(entity.getId());
    assertNull(entity.getName());
    assertNull(entity.getContent());
  }

  @Test
  void allArgsConstructor_CreatesValidEntity() {
    LocalDateTime now = LocalDateTime.now();

    Template entity = new Template(1L, "test-template", "<html><body>Test template</body></html>");

    assertEquals(1L, entity.getId());
    assertEquals("test-template", entity.getName());
    assertEquals("<html><body>Test template</body></html>", entity.getContent());
    assertTrue(java.time.Duration.between(now, entity.getUpdatedAt()).abs().getSeconds() <= 5);
  }

  @Test
  void settersAndGetters_WorkCorrectly() {
    Template entity = new Template();
    LocalDateTime now = LocalDateTime.now();

    entity.setId(1L);
    entity.setName("test-template");
    entity.setContent("<html><body>Test template</body></html>");
    entity.setUpdatedAt(now);

    assertEquals(1L, entity.getId());
    assertEquals("test-template", entity.getName());
    assertEquals("<html><body>Test template</body></html>", entity.getContent());
    assertTrue(java.time.Duration.between(now, entity.getUpdatedAt()).abs().getSeconds() <= 5);
  }

  @Test
  void equals_ReturnsTrueForSameProperties() {
    LocalDateTime now = LocalDateTime.now();

    Template entity1 =
        Template.builder()
            .id(1L)
            .name("test-template")
            .content("<html><body>Test</body></html>")
            .updatedAt(now)
            .build();

    Template entity2 =
        Template.builder()
            .id(1L)
            .name("test-template")
            .content("<html><body>Test</body></html>")
            .updatedAt(now)
            .build();

    assertEquals(entity1, entity2);
  }

  @Test
  void equals_ReturnsFalseForDifferentProperties() {
    LocalDateTime now = LocalDateTime.now();

    Template entity1 =
        Template.builder()
            .id(1L)
            .name("test-template-1")
            .content("<html><body>Test 1</body></html>")
            .updatedAt(now)
            .build();

    Template entity2 =
        Template.builder()
            .id(2L)
            .name("test-template-2")
            .content("<html><body>Test 2</body></html>")
            .updatedAt(now)
            .build();

    assertNotEquals(entity1, entity2);
  }

  @Test
  void hashCode_IsConsistent() {
    LocalDateTime now = LocalDateTime.now();

    Template entity1 =
        Template.builder()
            .id(1L)
            .name("test-template")
            .content("<html><body>Test</body></html>")
            .updatedAt(now)
            .build();

    Template entity2 =
        Template.builder()
            .id(1L)
            .name("test-template")
            .content("<html><body>Test</body></html>")
            .updatedAt(now)
            .build();

    assertEquals(entity1.hashCode(), entity2.hashCode());
  }

  @Test
  void toString_ContainsRelevantFields() {
    LocalDateTime now = LocalDateTime.now();

    Template entity =
        Template.builder()
            .id(1L)
            .name("test-template")
            .content("<html><body>Test</body></html>")
            .updatedAt(now)
            .build();

    String toString = entity.toString();

    assertTrue(toString.contains("id=1"));
    assertTrue(toString.contains("name='test-template'"));
    assertTrue(toString.contains("updatedAt=" + now));
    assertFalse(toString.contains("<html>"));
  }
}
