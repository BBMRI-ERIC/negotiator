package eu.bbmri.eric.csit.service.negotiator.integration;


import eu.bbmri.eric.csit.service.negotiator.service.NotificationService;
import eu.bbmri.eric.csit.service.negotiator.service.NotificationServiceImpl;
import org.junit.jupiter.api.Test;

public class NotificationServiceTest {
  private final NotificationService notificationService = new NotificationServiceImpl();
  @Test
  void sendEmail_nullRecipient_returnsFalse() {

  }
}
