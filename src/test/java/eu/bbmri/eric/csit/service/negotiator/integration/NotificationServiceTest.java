package eu.bbmri.eric.csit.service.negotiator.integration;


import eu.bbmri.eric.csit.service.negotiator.service.NotificationService;
import eu.bbmri.eric.csit.service.negotiator.service.NotificationServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NotificationServiceTest {
  private final NotificationService notificationService = new NotificationServiceImpl();
  @Test
  void sendEmail_nullRecipient_returnsFalse() {
    assertFalse(notificationService.sendEmail(null));
  }

  @Test
  void sendEmail_recipientIsRandomString_returnsFalse() {
    assertFalse(notificationService.sendEmail("random"));
  }

  @Test
  void sendEmail_validRecipientEmail_returns() {
    assertTrue(notificationService.sendEmail("test@test.com"));
  }
}
