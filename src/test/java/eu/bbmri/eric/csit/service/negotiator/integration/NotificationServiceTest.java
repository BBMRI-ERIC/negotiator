package eu.bbmri.eric.csit.service.negotiator.integration;

import eu.bbmri.eric.csit.service.negotiator.service.NotificationService;
import eu.bbmri.eric.csit.service.negotiator.service.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Disabled
public class NotificationServiceTest {
  private NotificationService notificationService;

  @BeforeEach
  void setUp() {
    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    mailSender.setHost("0.0.0.0");
    mailSender.setPort(1025);
    notificationService = new NotificationServiceImpl(mailSender);
  }
}
