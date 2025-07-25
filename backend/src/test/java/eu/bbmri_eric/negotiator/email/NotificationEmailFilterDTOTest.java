package eu.bbmri_eric.negotiator.email;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class NotificationEmailFilterDTOTest {

  @Test
  void defaultConstructor_SetsDefaultValues() {
    NotificationEmailFilterDTO filter = new NotificationEmailFilterDTO();

    assertEquals(0, filter.getPage());
    assertEquals(20, filter.getSize());
    assertEquals("sentAt,desc", filter.getSort());
    assertNull(filter.getAddress());
    assertNull(filter.getSentAfter());
    assertNull(filter.getSentBefore());
  }

  @Test
  void allArgsConstructor_SetsAllValues() {
    LocalDateTime sentAfter = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
    LocalDateTime sentBefore = LocalDateTime.of(2024, 12, 31, 23, 59, 59);

    NotificationEmailFilterDTO filter =
        new NotificationEmailFilterDTO(
            1, 10, "test@example.com", sentAfter, sentBefore, "address,asc");

    assertEquals(1, filter.getPage());
    assertEquals(10, filter.getSize());
    assertEquals("test@example.com", filter.getAddress());
    assertEquals(sentAfter, filter.getSentAfter());
    assertEquals(sentBefore, filter.getSentBefore());
    assertEquals("address,asc", filter.getSort());
  }

  @Test
  void settersAndGetters_WorkCorrectly() {
    NotificationEmailFilterDTO filter = new NotificationEmailFilterDTO();
    LocalDateTime sentAfter = LocalDateTime.of(2024, 6, 1, 0, 0, 0);
    LocalDateTime sentBefore = LocalDateTime.of(2024, 6, 30, 23, 59, 59);

    filter.setPage(2);
    filter.setSize(50);
    filter.setAddress("user@domain.com");
    filter.setSentAfter(sentAfter);
    filter.setSentBefore(sentBefore);
    filter.setSort("sentAt,asc");

    assertEquals(2, filter.getPage());
    assertEquals(50, filter.getSize());
    assertEquals("user@domain.com", filter.getAddress());
    assertEquals(sentAfter, filter.getSentAfter());
    assertEquals(sentBefore, filter.getSentBefore());
    assertEquals("sentAt,asc", filter.getSort());
  }

  @Test
  void dateRangeValidation_WorksCorrectly() {
    NotificationEmailFilterDTO filter = new NotificationEmailFilterDTO();
    LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 9, 0, 0);
    LocalDateTime endDate = LocalDateTime.of(2024, 1, 31, 17, 30, 0);

    filter.setSentAfter(startDate);
    filter.setSentBefore(endDate);

    assertTrue(filter.getSentAfter().isBefore(filter.getSentBefore()));
    assertEquals(startDate, filter.getSentAfter());
    assertEquals(endDate, filter.getSentBefore());
  }

  @Test
  void equality_WorksCorrectly() {
    LocalDateTime date1 = LocalDateTime.of(2024, 3, 15, 12, 0, 0);
    LocalDateTime date2 = LocalDateTime.of(2024, 3, 20, 18, 30, 0);

    NotificationEmailFilterDTO filter1 =
        new NotificationEmailFilterDTO(1, 25, "test@example.com", date1, date2, "sentAt,asc");

    NotificationEmailFilterDTO filter2 =
        new NotificationEmailFilterDTO(1, 25, "test@example.com", date1, date2, "sentAt,asc");

    assertEquals(filter1.getPage(), filter2.getPage());
    assertEquals(filter1.getSize(), filter2.getSize());
    assertEquals(filter1.getAddress(), filter2.getAddress());
    assertEquals(filter1.getSentAfter(), filter2.getSentAfter());
    assertEquals(filter1.getSentBefore(), filter2.getSentBefore());
    assertEquals(filter1.getSort(), filter2.getSort());
  }

  @Test
  void nullDateValues_HandledCorrectly() {
    NotificationEmailFilterDTO filter = new NotificationEmailFilterDTO();

    filter.setSentAfter(null);
    filter.setSentBefore(null);

    assertNull(filter.getSentAfter());
    assertNull(filter.getSentBefore());
  }

  @Test
  void differentTimeZones_HandledCorrectly() {
    // Test with different times in the same day
    LocalDateTime morning = LocalDateTime.of(2024, 7, 16, 8, 30, 0);
    LocalDateTime evening = LocalDateTime.of(2024, 7, 16, 20, 45, 30);

    NotificationEmailFilterDTO filter = new NotificationEmailFilterDTO();
    filter.setSentAfter(morning);
    filter.setSentBefore(evening);

    assertTrue(filter.getSentAfter().isBefore(filter.getSentBefore()));
    assertEquals(8, filter.getSentAfter().getHour());
    assertEquals(30, filter.getSentAfter().getMinute());
    assertEquals(20, filter.getSentBefore().getHour());
    assertEquals(45, filter.getSentBefore().getMinute());
    assertEquals(30, filter.getSentBefore().getSecond());
  }
}
