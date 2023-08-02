package eu.bbmri.eric.csit.service.negotiator.integration;


import static org.junit.jupiter.api.Assertions.assertFalse;

import eu.bbmri.eric.csit.service.negotiator.service.NotificationService;
import eu.bbmri.eric.csit.service.negotiator.service.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

public class NotificationServiceTest {
  private NotificationService notificationService;

  @BeforeEach
  void setUp() {
    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    mailSender.setHost("");
    mailSender.setHost("");
    notificationService = new NotificationServiceImpl(mailSender);
    }

  @Test
  void sendEmail_nullRecipient_returnsFalse() {
    assertFalse(notificationService.sendEmail(null));
  }

  @Test
  void sendEmail_recipientIsRandomString_returnsFalse() {
    assertFalse(notificationService.sendEmail("random"));
  }

  @Test
  void sendEmail_validRecipientInvalidSMTPConfig_returnsFalse() {
    assertFalse(notificationService.sendEmail("test@test.com"));
  }
}
