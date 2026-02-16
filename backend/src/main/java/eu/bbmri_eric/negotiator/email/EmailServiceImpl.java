package eu.bbmri_eric.negotiator.email;

import eu.bbmri_eric.negotiator.email.EmailRateLimitConfig.EmailRateLimitProperties;
import eu.bbmri_eric.negotiator.notification.NotificationService;
import eu.bbmri_eric.negotiator.user.Person;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Implementation of EmailService with rate limiting to prevent SMTP throttling.
 *
 * <p>This service uses a semaphore to limit the number of concurrent SMTP connections, which is
 * particularly important for email providers like Microsoft 365 that enforce strict connection
 * limits (e.g., max 3 concurrent connections).
 *
 * <p>The implementation is designed to handle high-volume scenarios (1000+ emails) by queuing email
 * tasks and processing them with controlled concurrency.
 */
@CommonsLog
@Service
public class EmailServiceImpl implements EmailService {
  private static final String UTF_8_ENCODING = "utf-8";

  private static final String ERROR_INVALID_EMAIL =
      "Email address must not be empty and must be valid";
  private static final String ERROR_INVALID_SUBJECT = "Subject must not be null or empty";
  private static final String ERROR_INVALID_BODY = "Mail body must not be null";
  private static final String ERROR_BUILD_MESSAGE = "Failed to build email message";
  private static final String ERROR_SMTP_CONFIG =
      "Failed to send email due to SMTP configuration issues";

  private final JavaMailSender javaMailSender;
  private final NotificationEmailRepository notificationEmailRepository;
  private final NotificationService notificationService;
  private final Semaphore emailRateLimitSemaphore;
  private final int semaphoreTimeoutSeconds;
  private final long delayBetweenEmailsMs;

  @Value("${negotiator.email-address}")
  private String fromAddress;

  EmailServiceImpl(
      JavaMailSender javaMailSender,
      NotificationEmailRepository notificationEmailRepository,
      NotificationService notificationService,
      Semaphore emailRateLimitSemaphore,
      EmailRateLimitProperties rateLimitProperties) {
    this.javaMailSender = Objects.requireNonNull(javaMailSender, "JavaMailSender must not be null");
    this.notificationEmailRepository =
        Objects.requireNonNull(
            notificationEmailRepository, "NotificationEmailRepository must not be null");
    this.notificationService = notificationService;
    this.emailRateLimitSemaphore =
        Objects.requireNonNull(
            emailRateLimitSemaphore, "Email rate limit semaphore must not be null");
    this.semaphoreTimeoutSeconds =
        rateLimitProperties != null ? rateLimitProperties.getSemaphoreTimeoutSeconds() : 120;
    this.delayBetweenEmailsMs =
        rateLimitProperties != null ? rateLimitProperties.getDelayBetweenEmailsMs() : 1000;
  }

  @Override
  public void sendEmail(Person recipient, String subject, String content, String negotiationId) {
    Objects.requireNonNull(recipient, "Recipient must not be null");
    validateEmailParameters(recipient.getEmail(), subject, content);
    var recipientEmail = recipient.getEmail();
    try {
      String messageId = resolveMessageId(recipient.getId(), negotiationId);
      deliverEmail(recipientEmail, subject, content, negotiationId, messageId);
    } catch (Exception e) {
      log.error(
          String.format(
              "Failed to send email to person %s: %s", recipient.getId(), e.getMessage()));
    }
  }

  @Override
  public void sendEmail(String emailAddress, String subject, String content) {
    validateEmailParameters(emailAddress, subject, content);
    deliverEmail(emailAddress, subject, content, null, null);
  }

  /**
   * Resolves the Message-ID for email threading.
   *
   * <p>The first email in a thread uses the negotiationId as Message-ID to establish the thread.
   * Subsequent emails use a unique UUID to avoid duplicate Message-IDs while still referencing the
   * original thread via In-Reply-To and References headers.
   */
  private String resolveMessageId(Long recipientId, String negotiationId) {
    long existingCount =
        notificationService.countByRecipientIdAndNegotiationId(recipientId, negotiationId);
    return existingCount >= 1 ? UUID.randomUUID().toString() : negotiationId;
  }

  private void deliverEmail(
      String recipientAddress,
      String subject,
      String content,
      String negotiationId,
      String messageId) {
    var mimeMessage = buildMimeMessage(recipientAddress, subject, content);

    boolean permitAcquired = false;
    try {
      permitAcquired = acquireRateLimitPermit(recipientAddress);
      setEmailHeaders(mimeMessage, negotiationId, messageId);
      sendAndDelay(mimeMessage, recipientAddress);
    } catch (MailSendException | MessagingException e) {
      handleMailException(recipientAddress, e);
    } catch (InterruptedException e) {
      handleInterruptedException(e);
    } finally {
      releasePermitIfAcquired(permitAcquired);
    }
    recordEmailNotification(recipientAddress, content);
  }

  private boolean acquireRateLimitPermit(String recipientAddress) throws InterruptedException {
    log.debug(
        String.format(
            "Waiting for email rate limit permit. Available permits: %d",
            emailRateLimitSemaphore.availablePermits()));

    boolean permitAcquired =
        emailRateLimitSemaphore.tryAcquire(semaphoreTimeoutSeconds, TimeUnit.SECONDS);

    if (!permitAcquired) {
      log.warn(
          String.format(
              "Timeout waiting for email rate limit permit for recipient: %s. Queue may be overloaded.",
              recipientAddress));
      throw new RuntimeException("Timeout waiting for email rate limit permit");
    }

    log.debug(
        String.format(
            "Acquired email rate limit permit. Remaining permits: %d",
            emailRateLimitSemaphore.availablePermits()));

    return true;
  }

  private void setEmailHeaders(MimeMessage mimeMessage, String negotiationId, String messageId)
      throws MessagingException {
    if (negotiationId == null) {
      return;
    }
    String domain = extractDomainFromAddress();
    mimeMessage.setHeader("Message-ID", "<" + messageId + "@" + domain + ">");
    mimeMessage.setHeader("In-Reply-To", "<" + negotiationId + "@" + domain + ">");
    mimeMessage.setHeader("References", "<" + negotiationId + "@" + domain + ">");
  }

  private String extractDomainFromAddress() {
    return fromAddress.split("@")[1];
  }

  private void sendAndDelay(MimeMessage mimeMessage, String recipientAddress)
      throws InterruptedException {
    javaMailSender.send(mimeMessage);
    log.debug(String.format("Successfully sent email to: %s", recipientAddress));
    applyRateLimitDelay();
  }

  private void applyRateLimitDelay() throws InterruptedException {
    if (delayBetweenEmailsMs > 0) {
      Thread.sleep(delayBetweenEmailsMs);
    }
  }

  private void handleMailException(String recipientAddress, Exception e) {
    log.error(
        String.format(
            "Failed to send email to %s. SMTP configuration error: %s",
            recipientAddress, e.getMessage()));
    throw new RuntimeException(ERROR_SMTP_CONFIG, e);
  }

  private void handleInterruptedException(InterruptedException e) {
    Thread.currentThread().interrupt();
    log.error(
        String.format("Interrupted while waiting for email rate limit permit: %s", e.getMessage()));
    throw new RuntimeException("Email sending interrupted", e);
  }

  private void releasePermitIfAcquired(boolean permitAcquired) {
    if (permitAcquired) {
      emailRateLimitSemaphore.release();
      log.debug(
          String.format(
              "Released email rate limit permit. Available permits: %d",
              emailRateLimitSemaphore.availablePermits()));
    }
  }

  private void validateEmailParameters(String emailAddress, String subject, String mailBody) {
    EmailValidator emailValidator = EmailValidator.getInstance();
    if (Objects.isNull(emailAddress)
        || emailAddress.trim().isEmpty()
        || !emailValidator.isValid(emailAddress)) {
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
