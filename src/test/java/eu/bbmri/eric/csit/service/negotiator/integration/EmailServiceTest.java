package eu.bbmri.eric.csit.service.negotiator.integration;

import eu.bbmri.eric.csit.service.negotiator.service.EmailService;
import eu.bbmri.eric.csit.service.negotiator.service.EmailServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Disabled
public class EmailServiceTest {
  private EmailService emailService;

  @BeforeEach
  void setUp() {
    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    mailSender.setHost("0.0.0.0");
    mailSender.setPort(1025);
    emailService = new EmailServiceImpl(mailSender);
  }
}
