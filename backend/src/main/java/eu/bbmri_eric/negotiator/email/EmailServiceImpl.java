package eu.bbmri_eric.negotiator.email;

import eu.bbmri_eric.negotiator.user.Person;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@CommonsLog
@Service
public class EmailServiceImpl implements EmailService {

  private static final String EMAIL_REGEX_PATTERN =
      "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$";
  private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX_PATTERN);
  private static final String UTF_8_ENCODING = "utf-8";

  private static final String ERROR_INVALID_EMAIL = "Email address must not be null or empty";
  private static final String ERROR_INVALID_SUBJECT = "Subject must not be null or empty";
  private static final String ERROR_INVALID_BODY = "Mail body must not be null";
  private static final String ERROR_EMAIL_FORMAT = "Invalid email address format: ";
  private static final String ERROR_BUILD_MESSAGE = "Failed to build email message";
  private static final String ERROR_SMTP_CONFIG =
      "Failed to send email due to SMTP configuration issues";

  private final JavaMailSender javaMailSender;
  private final NotificationEmailRepository notificationEmailRepository;

  @Value("${negotiator.email-address}")
  private String fromAddress;

  EmailServiceImpl(
      JavaMailSender javaMailSender, NotificationEmailRepository notificationEmailRepository) {
    this.javaMailSender = Objects.requireNonNull(javaMailSender, "JavaMailSender must not be null");
    this.notificationEmailRepository =
        Objects.requireNonNull(
            notificationEmailRepository, "NotificationEmailRepository must not be null");
  }

  @Override
  public void sendEmail(Person recipient, String subject, String mailBody) {
    Objects.requireNonNull(recipient, "Recipient must not be null");
    var recipientEmail = recipient.getEmail();
    try {
      deliverEmail(recipientEmail, subject, mailBody);
    } catch (Exception e) {
      log.error("Failed to send email to person " + recipient.getId() + ": " + e.getMessage());
    }
  }

  @Override
  public void sendEmail(String emailAddress, String subject, String message) {
    validateEmailParameters(emailAddress, subject, message);
    deliverEmail(emailAddress, subject, message);
  }

  private void deliverEmail(String recipientAddress, String subject, String content) {
    var mimeMessage = buildMimeMessage(recipientAddress, subject, content);
    try {
      javaMailSender.send(mimeMessage);
    } catch (MailSendException e) {
      log.error(
          "Failed to send email to "
              + recipientAddress
              + ". SMTP configuration error: "
              + e.getMessage());
      throw new RuntimeException(ERROR_SMTP_CONFIG, e);
    }
    recordEmailNotification(recipientAddress, content);
  }

  private void validateEmailParameters(String emailAddress, String subject, String mailBody) {
    if (Objects.isNull(emailAddress) || emailAddress.trim().isEmpty()) {
      throw new IllegalArgumentException(ERROR_INVALID_EMAIL);
    }
    if (Objects.isNull(subject) || subject.trim().isEmpty()) {
      throw new IllegalArgumentException(ERROR_INVALID_SUBJECT);
    }
    if (Objects.isNull(mailBody)) {
      throw new IllegalArgumentException(ERROR_INVALID_BODY);
    }
  }

  private MimeMessage buildMimeMessage(String emailAddress, String subject, String mailBody) {
    var mimeMessage = javaMailSender.createMimeMessage();
    try {
      var helper = new MimeMessageHelper(mimeMessage, UTF_8_ENCODING);
      helper.setText(mailBody, true);
      helper.setTo(emailAddress);
      helper.setSubject(subject);
      helper.setFrom(fromAddress);
      return mimeMessage;
    } catch (MessagingException e) {
      log.error("Failed to configure email message: " + e.getMessage());
      throw new RuntimeException(ERROR_BUILD_MESSAGE, e);
    }
  }

  private void recordEmailNotification(String emailAddress, String mailBody) {
    notificationEmailRepository.save(
        NotificationEmail.builder()
            .address(emailAddress)
            .message(mailBody)
            .sentAt(LocalDateTime.now())
            .build());
  }
}
