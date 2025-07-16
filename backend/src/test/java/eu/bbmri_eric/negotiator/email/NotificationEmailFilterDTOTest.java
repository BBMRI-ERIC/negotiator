package eu.bbmri_eric.negotiator.email;

import static org.junit.jupiter.api.Assertions.*;

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
    NotificationEmailFilterDTO filter =
        new NotificationEmailFilterDTO(
            1, 10, "test@example.com", "2024-01-01T00:00:00", "2024-12-31T23:59:59", "address,asc");

    assertEquals(1, filter.getPage());
    assertEquals(10, filter.getSize());
    assertEquals("test@example.com", filter.getAddress());
    assertEquals("2024-01-01T00:00:00", filter.getSentAfter());
    assertEquals("2024-12-31T23:59:59", filter.getSentBefore());
    assertEquals("address,asc", filter.getSort());
  }

  @Test
  void settersAndGetters_WorkCorrectly() {
    NotificationEmailFilterDTO filter = new NotificationEmailFilterDTO();

    filter.setPage(2);
    filter.setSize(50);
    filter.setAddress("user@domain.com");
    filter.setSentAfter("2024-06-01T00:00:00");
    filter.setSentBefore("2024-06-30T23:59:59");
    filter.setSort("sentAt,asc");

    assertEquals(2, filter.getPage());
    assertEquals(50, filter.getSize());
    assertEquals("user@domain.com", filter.getAddress());
    assertEquals("2024-06-01T00:00:00", filter.getSentAfter());
    assertEquals("2024-06-30T23:59:59", filter.getSentBefore());
    assertEquals("sentAt,asc", filter.getSort());
  }

  @Test
  void toString_ContainsAllFields() {
    NotificationEmailFilterDTO filter =
        new NotificationEmailFilterDTO(
            1, 10, "test@example.com", "2024-01-01T00:00:00", "2024-12-31T23:59:59", "address,asc");

    String toString = filter.toString();

    assertTrue(toString.contains("1"));
    assertTrue(toString.contains("10"));
    assertTrue(toString.contains("test@example.com"));
    assertTrue(toString.contains("2024-01-01T00:00:00"));
    assertTrue(toString.contains("2024-12-31T23:59:59"));
    assertTrue(toString.contains("address,asc"));
  }

  @Test
  void equals_WithSameValues_ReturnsTrue() {
    NotificationEmailFilterDTO filter1 =
        new NotificationEmailFilterDTO(
            1, 10, "test@example.com", "2024-01-01T00:00:00", "2024-12-31T23:59:59", "address,asc");

    NotificationEmailFilterDTO filter2 =
        new NotificationEmailFilterDTO(
            1, 10, "test@example.com", "2024-01-01T00:00:00", "2024-12-31T23:59:59", "address,asc");

    assertEquals(filter1, filter2);
    assertEquals(filter1.hashCode(), filter2.hashCode());
  }

  @Test
  void equals_WithDifferentValues_ReturnsFalse() {
    NotificationEmailFilterDTO filter1 =
        new NotificationEmailFilterDTO(
            1, 10, "test@example.com", "2024-01-01T00:00:00", "2024-12-31T23:59:59", "address,asc");

    NotificationEmailFilterDTO filter2 =
        new NotificationEmailFilterDTO(
            2, 10, "test@example.com", "2024-01-01T00:00:00", "2024-12-31T23:59:59", "address,asc");

    assertNotEquals(filter1, filter2);
  }

  @Test
  void builderPattern_WorksCorrectly() {
    NotificationEmailFilterDTO filter =
        NotificationEmailFilterDTO.builder()
            .page(3)
            .size(25)
            .address("builder@test.com")
            .sentAfter("2024-07-01T00:00:00")
            .sentBefore("2024-07-31T23:59:59")
            .sort("message,desc")
            .build();

    assertEquals(3, filter.getPage());
    assertEquals(25, filter.getSize());
    assertEquals("builder@test.com", filter.getAddress());
    assertEquals("2024-07-01T00:00:00", filter.getSentAfter());
    assertEquals("2024-07-31T23:59:59", filter.getSentBefore());
    assertEquals("message,desc", filter.getSort());
  }

  @Test
  void implementsFilterDTO_Interface() {
    NotificationEmailFilterDTO filter = new NotificationEmailFilterDTO();

    assertTrue(filter instanceof eu.bbmri_eric.negotiator.common.FilterDTO);
  }
}
