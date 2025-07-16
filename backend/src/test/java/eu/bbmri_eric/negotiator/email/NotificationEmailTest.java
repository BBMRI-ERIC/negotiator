package eu.bbmri_eric.negotiator.email;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class NotificationEmailTest {

  @Test
  void builder_CreatesValidEntity() {
    LocalDateTime now = LocalDateTime.now();

    NotificationEmail entity =
        NotificationEmail.builder()
            .id(1L)
            .address("test@example.com")
            .message("Test message")
            .sentAt(now)
            .build();

    assertEquals(1L, entity.getId());
    assertEquals("test@example.com", entity.getAddress());
    assertEquals("Test message", entity.getMessage());
    assertEquals(now, entity.getSentAt());
  }

  @Test
  void noArgsConstructor_CreatesEmptyEntity() {
    NotificationEmail entity = new NotificationEmail();

    assertNull(entity.getId());
    assertNull(entity.getAddress());
    assertNull(entity.getMessage());
    assertNotNull(entity.getSentAt()); // sentAt has default value
  }

  @Test
  void allArgsConstructor_CreatesValidEntity() {
    LocalDateTime now = LocalDateTime.now();

    NotificationEmail entity = new NotificationEmail(1L, "test@example.com", "Test message", now);

    assertEquals(1L, entity.getId());
    assertEquals("test@example.com", entity.getAddress());
    assertEquals("Test message", entity.getMessage());
    assertEquals(now, entity.getSentAt());
  }

  @Test
  void settersAndGetters_WorkCorrectly() {
    NotificationEmail entity = new NotificationEmail();
    LocalDateTime now = LocalDateTime.now();

    entity.setId(1L);
    entity.setAddress("test@example.com");
    entity.setMessage("Test message");
    entity.setSentAt(now);

    assertEquals(1L, entity.getId());
    assertEquals("test@example.com", entity.getAddress());
    assertEquals("Test message", entity.getMessage());
    assertEquals(now, entity.getSentAt());
  }

  @Test
  void defaultSentAt_IsNotNull() {
    NotificationEmail entity = new NotificationEmail();

    assertNotNull(entity.getSentAt());
    assertTrue(entity.getSentAt().isBefore(LocalDateTime.now().plusSeconds(1)));
  }

  @Test
  void equals_WithSameValues_ReturnsTrue() {
    LocalDateTime now = LocalDateTime.now();

    NotificationEmail entity1 =
        NotificationEmail.builder()
            .id(1L)
            .address("test@example.com")
            .message("Test message")
            .sentAt(now)
            .build();

    NotificationEmail entity2 =
        NotificationEmail.builder()
            .id(1L)
            .address("test@example.com")
            .message("Test message")
            .sentAt(now)
            .build();

    assertEquals(entity1, entity2);
    assertEquals(entity1.hashCode(), entity2.hashCode());
  }

  @Test
  void equals_WithDifferentValues_ReturnsFalse() {
    LocalDateTime now = LocalDateTime.now();

    NotificationEmail entity1 =
        NotificationEmail.builder()
            .id(1L)
            .address("test@example.com")
            .message("Test message")
            .sentAt(now)
            .build();

    NotificationEmail entity2 =
        NotificationEmail.builder()
            .id(2L)
            .address("test@example.com")
            .message("Test message")
            .sentAt(now)
            .build();

    assertNotEquals(entity1, entity2);
  }
}
