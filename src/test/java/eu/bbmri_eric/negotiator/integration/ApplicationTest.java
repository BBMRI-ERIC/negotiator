package eu.bbmri_eric.negotiator.integration;

import eu.bbmri_eric.negotiator.util.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

@IntegrationTest
@Order(1)
@DisplayName("Spring Application Tests")
public class ApplicationTest {
  @Test
  @DisplayName("Load Spring Application Context")
  void loadContext() {}
}
