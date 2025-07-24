package eu.bbmri_eric.negotiator.email;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class NotificationEmailDTOTest {

  @Test
  void builder_CreatesValidDTO() {
    LocalDateTime now = LocalDateTime.now();

    NotificationEmailDTO dto =
        NotificationEmailDTO.builder()
            .id(1L)
            .address("test@example.com")
            .message("Test message")
            .sentAt(now)
            .build();

    assertEquals(1L, dto.getId());
    assertEquals("test@example.com", dto.getAddress());
    assertEquals("Test message", dto.getMessage());
    assertEquals(now, dto.getSentAt());
  }

  @Test
  void noArgsConstructor_CreatesEmptyDTO() {
    NotificationEmailDTO dto = new NotificationEmailDTO();

    assertNull(dto.getId());
    assertNull(dto.getAddress());
    assertNull(dto.getMessage());
    assertNull(dto.getSentAt());
  }

  @Test
  void allArgsConstructor_CreatesValidDTO() {
    LocalDateTime now = LocalDateTime.now();

    NotificationEmailDTO dto =
        new NotificationEmailDTO(1L, "test@example.com", "Test message", now);

    assertEquals(1L, dto.getId());
    assertEquals("test@example.com", dto.getAddress());
    assertEquals("Test message", dto.getMessage());
    assertEquals(now, dto.getSentAt());
  }

  @Test
  void settersAndGetters_WorkCorrectly() {
    NotificationEmailDTO dto = new NotificationEmailDTO();
    LocalDateTime now = LocalDateTime.now();

    dto.setId(1L);
    dto.setAddress("test@example.com");
    dto.setMessage("Test message");
    dto.setSentAt(now);

    assertEquals(1L, dto.getId());
    assertEquals("test@example.com", dto.getAddress());
    assertEquals("Test message", dto.getMessage());
    assertEquals(now, dto.getSentAt());
  }

  @Test
  void toString_ContainsAllFields() {
    LocalDateTime now = LocalDateTime.now();
    NotificationEmailDTO dto =
        NotificationEmailDTO.builder()
            .id(1L)
            .address("test@example.com")
            .message("Test message")
            .sentAt(now)
            .build();

    String toString = dto.toString();

    assertTrue(toString.contains("1"));
    assertTrue(toString.contains("test@example.com"));
    assertTrue(toString.contains("Test message"));
    assertTrue(toString.contains(now.toString()));
  }

  @Test
  void equals_WithSameValues_ReturnsTrue() {
    LocalDateTime now = LocalDateTime.now();

    NotificationEmailDTO dto1 =
        NotificationEmailDTO.builder()
            .id(1L)
            .address("test@example.com")
            .message("Test message")
            .sentAt(now)
            .build();

    NotificationEmailDTO dto2 =
        NotificationEmailDTO.builder()
            .id(1L)
            .address("test@example.com")
            .message("Test message")
            .sentAt(now)
            .build();

    assertEquals(dto1, dto2);
    assertEquals(dto1.hashCode(), dto2.hashCode());
  }

  @Test
  void equals_WithDifferentValues_ReturnsFalse() {
    LocalDateTime now = LocalDateTime.now();

    NotificationEmailDTO dto1 =
        NotificationEmailDTO.builder()
            .id(1L)
            .address("test@example.com")
            .message("Test message")
            .sentAt(now)
            .build();

    NotificationEmailDTO dto2 =
        NotificationEmailDTO.builder()
            .id(2L)
            .address("test@example.com")
            .message("Test message")
            .sentAt(now)
            .build();

    assertNotEquals(dto1, dto2);
  }
}
