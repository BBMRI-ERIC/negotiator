package eu.bbmri_eric.negotiator.email;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

class NotificationEmailMappingTest {

  private ModelMapper modelMapper;

  @BeforeEach
  void setUp() {
    modelMapper = new ModelMapper();
  }

  @Test
  void mapEntityToDTO_MapsAllFields() {
    LocalDateTime now = LocalDateTime.now();
    NotificationEmail entity =
        NotificationEmail.builder()
            .id(1L)
            .address("test@example.com")
            .message("Test message")
            .sentAt(now)
            .build();

    NotificationEmailDTO dto = modelMapper.map(entity, NotificationEmailDTO.class);

    assertEquals(entity.getId(), dto.getId());
    assertEquals(entity.getAddress(), dto.getAddress());
    assertEquals(entity.getMessage(), dto.getMessage());
    assertEquals(entity.getSentAt(), dto.getSentAt());
  }

  @Test
  void mapDTOToEntity_MapsAllFields() {
    LocalDateTime now = LocalDateTime.now();
    NotificationEmailDTO dto =
        NotificationEmailDTO.builder()
            .id(1L)
            .address("test@example.com")
            .message("Test message")
            .sentAt(now)
            .build();

    NotificationEmail entity = modelMapper.map(dto, NotificationEmail.class);

    assertEquals(dto.getId(), entity.getId());
    assertEquals(dto.getAddress(), entity.getAddress());
    assertEquals(dto.getMessage(), entity.getMessage());
    assertEquals(dto.getSentAt(), entity.getSentAt());
  }

  @Test
  void mapEntityWithNullValues_HandlesGracefully() {
    NotificationEmail entity =
        NotificationEmail.builder().id(null).address(null).message(null).sentAt(null).build();

    NotificationEmailDTO dto = modelMapper.map(entity, NotificationEmailDTO.class);

    assertNull(dto.getId());
    assertNull(dto.getAddress());
    assertNull(dto.getMessage());
    assertNull(dto.getSentAt());
  }

  @Test
  void mapDTOWithNullValues_HandlesGracefully() {
    NotificationEmailDTO dto =
        NotificationEmailDTO.builder().id(null).address(null).message(null).sentAt(null).build();

    NotificationEmail entity = modelMapper.map(dto, NotificationEmail.class);

    assertNull(entity.getId());
    assertNull(entity.getAddress());
    assertNull(entity.getMessage());
    assertNull(entity.getSentAt());
  }

  @Test
  void mapEntityToDTO_WithLongMessage_MapsCorrectly() {
    String longMessage =
        "This is a very long message that might be stored in a TEXT column. ".repeat(100);
    NotificationEmail entity =
        NotificationEmail.builder()
            .id(1L)
            .address("test@example.com")
            .message(longMessage)
            .sentAt(LocalDateTime.now())
            .build();

    NotificationEmailDTO dto = modelMapper.map(entity, NotificationEmailDTO.class);

    assertEquals(longMessage, dto.getMessage());
  }
}
