package eu.bbmri_eric.negotiator.email;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

class NotificationEmailSpecificationTest {

  @Test
  void withFilters_WhenAllFiltersProvided_ReturnsNonNullSpecification() {
    String address = "test@example.com";
    LocalDateTime sentAfter = LocalDateTime.now().minusDays(7);
    LocalDateTime sentBefore = LocalDateTime.now();

    Specification<NotificationEmail> spec =
        NotificationEmailSpecification.withFilters(address, sentAfter, sentBefore);

    assertNotNull(spec);
  }

  @Test
  void withFilters_WhenOnlyAddressProvided_ReturnsNonNullSpecification() {
    String address = "test@example.com";

    Specification<NotificationEmail> spec =
        NotificationEmailSpecification.withFilters(address, null, null);

    assertNotNull(spec);
  }

  @Test
  void withFilters_WhenNoFiltersProvided_ReturnsNonNullSpecification() {
    Specification<NotificationEmail> spec =
        NotificationEmailSpecification.withFilters(null, null, null);

    assertNotNull(spec);
  }

  @Test
  void withFilters_WhenEmptyAddressProvided_ReturnsNonNullSpecification() {
    Specification<NotificationEmail> spec =
        NotificationEmailSpecification.withFilters("", null, null);

    assertNotNull(spec);
  }

  @Test
  void withFilters_WhenOnlyDateFiltersProvided_ReturnsNonNullSpecification() {
    LocalDateTime sentAfter = LocalDateTime.now().minusDays(7);
    LocalDateTime sentBefore = LocalDateTime.now();

    Specification<NotificationEmail> spec =
        NotificationEmailSpecification.withFilters(null, sentAfter, sentBefore);

    assertNotNull(spec);
  }

  @Test
  void withFilters_WhenBlankAddressProvided_ReturnsNonNullSpecification() {
    Specification<NotificationEmail> spec =
        NotificationEmailSpecification.withFilters("   ", null, null);

    assertNotNull(spec);
  }
}
