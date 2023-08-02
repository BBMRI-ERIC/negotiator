package eu.bbmri.eric.csit.service.negotiator.unit.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri.eric.csit.service.negotiator.service.NotificationService;
import org.junit.jupiter.api.Test;

public class NotificationServiceTest {
  private final NotificationService notificationService =
      new NotificationService() {
        @Override
        public boolean sendEmailToResourceRepresentatives(String resourceId, String mailBody) {
          return true;
        }
      };

  @Test
  void sendEmailToResourceRepresentatives_emptyContent_Ok() {
    assertTrue(notificationService.sendEmailToResourceRepresentatives("collection:1", ""));
  }
}
