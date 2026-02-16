package eu.bbmri_eric.negotiator.email;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import eu.bbmri_eric.negotiator.email.EmailRateLimitConfig.EmailRateLimitProperties;
import eu.bbmri_eric.negotiator.notification.NotificationService;
import eu.bbmri_eric.negotiator.user.Person;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.apachecommons.CommonsLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@CommonsLog
class EmailServiceImplTest {

  @Mock private JavaMailSender javaMailSender;

  @Mock private NotificationEmailRepository notificationEmailRepository;

  @Mock private MimeMessage mimeMessage;

  private EmailServiceImpl emailService;

  @Mock private NotificationService notificationService;

  private Semaphore emailRateLimitSemaphore;

  private EmailRateLimitProperties rateLimitProperties;

  private static final String FROM_ADDRESS = "noreply@negotiator.com";
  private static final String VALID_EMAIL = "test@example.com";
  private static final String VALID_SUBJECT = "Test Subject";
  private static final String VALID_MAIL_BODY = "Test mail body content";

  @BeforeEach
  void setUp() {
    emailRateLimitSemaphore = new Semaphore(2, true);
    rateLimitProperties = new EmailRateLimitProperties();
    rateLimitProperties.setSemaphoreTimeoutSeconds(30);
    emailService =
        new EmailServiceImpl(
            javaMailSender,
            notificationEmailRepository,
            notificationService,
            emailRateLimitSemaphore,
            rateLimitProperties);
    ReflectionTestUtils.setField(emailService, "fromAddress", FROM_ADDRESS);
  }

  @Test
  void constructor_WithNullJavaMailSender_ThrowsNullPointerException() {
    assertThrows(
        NullPointerException.class,
        () ->
            new EmailServiceImpl(
                null,
                notificationEmailRepository,
                notificationService,
                emailRateLimitSemaphore,
                rateLimitProperties));
  }

  @Test
  void constructor_WithNullRepository_ThrowsNullPointerException() {
    assertThrows(
        NullPointerException.class,
        () ->
            new EmailServiceImpl(
                javaMailSender,
                null,
                notificationService,
                emailRateLimitSemaphore,
                rateLimitProperties));
  }

  @Test
  void constructor_WithNullSemaphore_ThrowsNullPointerException() {
    assertThrows(
        NullPointerException.class,
        () ->
            new EmailServiceImpl(
                javaMailSender,
                notificationEmailRepository,
                notificationService,
                null,
                rateLimitProperties));
  }

  @Test
  void constructor_WithNullProperties_UsesDefaultTimeout() {
    assertDoesNotThrow(
        () ->
            new EmailServiceImpl(
                javaMailSender,
                notificationEmailRepository,
                notificationService,
                emailRateLimitSemaphore,
                null));
  }

  @Test
  void sendEmail_WithValidStringParameters_SendsEmailSuccessfully() {
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

    var notificationEmail =
        NotificationEmail.builder()
            .address(VALID_EMAIL)
            .message(VALID_MAIL_BODY)
            .sentAt(LocalDateTime.now())
            .build();

    when(notificationEmailRepository.save(any(NotificationEmail.class)))
        .thenReturn(notificationEmail);

    assertDoesNotThrow(() -> emailService.sendEmail(VALID_EMAIL, VALID_SUBJECT, VALID_MAIL_BODY));

    verify(javaMailSender).createMimeMessage();
    verify(javaMailSender).send(mimeMessage);
    verify(notificationEmailRepository, times(1)).save(any(NotificationEmail.class));
  }

  @Test
  void sendEmail_WithValidPersonParameter_SendsEmailSuccessfully() {
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

    var person = Person.builder().id(1L).email(VALID_EMAIL).name("Test User").build();

    var notificationEmail =
        NotificationEmail.builder()
            .address(VALID_EMAIL)
            .message(VALID_MAIL_BODY)
            .sentAt(LocalDateTime.now())
            .build();

    when(notificationEmailRepository.save(any(NotificationEmail.class)))
        .thenReturn(notificationEmail);

    assertDoesNotThrow(() -> emailService.sendEmail(person, VALID_SUBJECT, VALID_MAIL_BODY, null));

    verify(javaMailSender).createMimeMessage();
    verify(javaMailSender).send(mimeMessage);
    verify(notificationEmailRepository, times(1)).save(any(NotificationEmail.class));
  }

  @Test
  void sendEmail_WithNullEmailAddress_ThrowsIllegalArgumentException() {
    var exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> emailService.sendEmail((String) null, VALID_SUBJECT, VALID_MAIL_BODY));

    assertEquals("Email address must not be empty and must be valid", exception.getMessage());
  }

  @Test
  void sendEmail_WithEmptyEmailAddress_ThrowsIllegalArgumentException() {
    var exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> emailService.sendEmail("", VALID_SUBJECT, VALID_MAIL_BODY));

    assertEquals("Email address must not be empty and must be valid", exception.getMessage());
  }

  @Test
  void sendEmail_WithBlankEmailAddress_ThrowsIllegalArgumentException() {
    var exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> emailService.sendEmail("   ", VALID_SUBJECT, VALID_MAIL_BODY));

    assertEquals("Email address must not be empty and must be valid", exception.getMessage());
  }

  @Test
  void sendEmail_WithNullSubject_ThrowsIllegalArgumentException() {
    var exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> emailService.sendEmail(VALID_EMAIL, null, VALID_MAIL_BODY));

    assertEquals("Subject must not be null or empty", exception.getMessage());
  }

  @Test
  void sendEmail_WithEmptySubject_ThrowsIllegalArgumentException() {
    var exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> emailService.sendEmail(VALID_EMAIL, "", VALID_MAIL_BODY));

    assertEquals("Subject must not be null or empty", exception.getMessage());
  }

  @Test
  void sendEmail_WithBlankSubject_ThrowsIllegalArgumentException() {
    var exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> emailService.sendEmail(VALID_EMAIL, "   ", VALID_MAIL_BODY));

    assertEquals("Subject must not be null or empty", exception.getMessage());
  }

  @Test
  void sendEmail_WithNullMailBody_ThrowsIllegalArgumentException() {
    var exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> emailService.sendEmail(VALID_EMAIL, VALID_SUBJECT, null));

    assertEquals("Mail body must not be null", exception.getMessage());
  }

  @Test
  void sendEmail_WithInvalidEmailFormat_ThrowsIllegalArgumentException() {
    var invalidEmail = "invalid-email-format";
    var exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> emailService.sendEmail(invalidEmail, VALID_SUBJECT, VALID_MAIL_BODY));
    assertEquals("Email address must not be empty and must be valid", exception.getMessage());
  }

  @Test
  void sendEmail_WithNullPerson_ThrowsNullPointerException() {
    assertThrows(
        NullPointerException.class,
        () -> emailService.sendEmail((Person) null, VALID_SUBJECT, VALID_MAIL_BODY, null));
  }

  @Test
  void sendEmail_WithPersonHavingInvalidEmail_throwsException() {
    var person = Person.builder().id(1L).email("invalid-email").name("Test User").build();

    assertThrows(
        IllegalArgumentException.class,
        () -> emailService.sendEmail(person, VALID_SUBJECT, VALID_MAIL_BODY, null));

    verifyNoInteractions(javaMailSender);
  }

  @Test
  void sendEmail_WithMessagingException_ThrowsRuntimeException() throws MessagingException {
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    doThrow(new MessagingException("MIME configuration error"))
        .when(mimeMessage)
        .setContent(anyString(), anyString());

    var exception =
        assertThrows(
            RuntimeException.class,
            () -> emailService.sendEmail(VALID_EMAIL, VALID_SUBJECT, VALID_MAIL_BODY));

    assertEquals("Failed to build email message", exception.getMessage());
    assertInstanceOf(MessagingException.class, exception.getCause());
  }

  @Test
  void sendEmail_WithMailSendException_ThrowsRuntimeException() {
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

    var notificationEmail =
        NotificationEmail.builder()
            .address(VALID_EMAIL)
            .message(VALID_MAIL_BODY)
            .sentAt(LocalDateTime.now())
            .build();
    doThrow(new MailSendException("SMTP server error")).when(javaMailSender).send(mimeMessage);

    var exception =
        assertThrows(
            RuntimeException.class,
            () -> emailService.sendEmail(VALID_EMAIL, VALID_SUBJECT, VALID_MAIL_BODY));

    assertEquals("Failed to send email due to SMTP configuration issues", exception.getMessage());
    assertInstanceOf(MailSendException.class, exception.getCause());
  }

  @Test
  void sendEmail_CreatesNotificationRecordWithCorrectData() {
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

    var notificationEmail =
        NotificationEmail.builder()
            .address(VALID_EMAIL)
            .message(VALID_MAIL_BODY)
            .sentAt(LocalDateTime.now())
            .build();

    when(notificationEmailRepository.save(any(NotificationEmail.class)))
        .thenReturn(notificationEmail);

    emailService.sendEmail(VALID_EMAIL, VALID_SUBJECT, VALID_MAIL_BODY);

    var captor = ArgumentCaptor.forClass(NotificationEmail.class);
    verify(notificationEmailRepository, times(1)).save(captor.capture());

    var capturedEmail = captor.getAllValues().getFirst();
    assertEquals(VALID_EMAIL, capturedEmail.getAddress());
    assertEquals(VALID_MAIL_BODY, capturedEmail.getMessage());
    assertNotNull(capturedEmail.getSentAt());
  }

  @Test
  void sendEmail_WithValidEmails_DoesNotThrowException() {
    String[] validEmails = {
      "test@example.com",
      "user.name@domain.co.uk",
      "first_last@company-name.org",
      "user+tag@example.net"
    };

    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

    var notificationEmail =
        NotificationEmail.builder()
            .address(VALID_EMAIL)
            .message(VALID_MAIL_BODY)
            .sentAt(LocalDateTime.now())
            .build();

    when(notificationEmailRepository.save(any(NotificationEmail.class)))
        .thenReturn(notificationEmail);

    for (String email : validEmails) {
      assertDoesNotThrow(
          () -> emailService.sendEmail(email, VALID_SUBJECT, VALID_MAIL_BODY),
          "Should accept valid email: " + email);
    }
  }

  @Test
  void sendEmail_WithBasicInvalidEmails_ThrowsIllegalArgumentException() {
    String[] invalidEmails = {"plainaddress", "@missingdomain.com", "spaces in@email.com"};

    for (String email : invalidEmails) {
      assertThrows(
          IllegalArgumentException.class,
          () -> emailService.sendEmail(email, VALID_SUBJECT, VALID_MAIL_BODY),
          "Should reject invalid email: " + email);
    }
  }

  @Test
  void sendEmail_WithEmailsFailingAtMimeLevel_ThrowsRuntimeException() {

    // These email formats pass our basic regex but fail at MIME processing level
    String[] problematicEmails = {
      "double..dot@domain.com", // Double dots in local part
      "missing@.com" // Domain starts with dot
    };

    for (String email : problematicEmails) {
      assertThrows(
          RuntimeException.class,
          () -> emailService.sendEmail(email, VALID_SUBJECT, VALID_MAIL_BODY),
          "Should throw RuntimeException for problematic email: " + email);
    }
  }

  @Test
  void deliverEmail_CallsMethodsInCorrectOrder() {
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

    var notificationEmail =
        NotificationEmail.builder()
            .address(VALID_EMAIL)
            .message(VALID_MAIL_BODY)
            .sentAt(LocalDateTime.now())
            .build();

    when(notificationEmailRepository.save(any(NotificationEmail.class)))
        .thenReturn(notificationEmail);

    emailService.sendEmail(VALID_EMAIL, VALID_SUBJECT, VALID_MAIL_BODY);

    var inOrder = inOrder(javaMailSender, notificationEmailRepository);
    inOrder.verify(javaMailSender).createMimeMessage();
    inOrder.verify(javaMailSender).send(mimeMessage);
    inOrder.verify(notificationEmailRepository).save(any(NotificationEmail.class));
  }

  @Test
  void sendEmail_ReleasesSemaphoreAfterSuccessfulSend() {
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

    var notificationEmail =
        NotificationEmail.builder()
            .address(VALID_EMAIL)
            .message(VALID_MAIL_BODY)
            .sentAt(LocalDateTime.now())
            .build();

    when(notificationEmailRepository.save(any(NotificationEmail.class)))
        .thenReturn(notificationEmail);

    int initialPermits = emailRateLimitSemaphore.availablePermits();

    emailService.sendEmail(VALID_EMAIL, VALID_SUBJECT, VALID_MAIL_BODY);
    assertEquals(
        initialPermits,
        emailRateLimitSemaphore.availablePermits(),
        "Semaphore should release permit after successful email send");
  }

  @Test
  void sendEmail_ReleasesSemaphoreEvenOnFailure() {
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    doThrow(new MailSendException("SMTP error")).when(javaMailSender).send(any(MimeMessage.class));

    int initialPermits = emailRateLimitSemaphore.availablePermits();

    assertThrows(
        RuntimeException.class,
        () -> emailService.sendEmail(VALID_EMAIL, VALID_SUBJECT, VALID_MAIL_BODY));

    // Semaphore should still be released even on failure
    assertEquals(
        initialPermits,
        emailRateLimitSemaphore.availablePermits(),
        "Semaphore should release permit even after failed email send");
  }

  @Test
  void sendEmail_HighVolume_2500Emails_RespectsConcurrencyLimit() throws InterruptedException {
    int maxConcurrentConnections = 2;
    emailRateLimitSemaphore = new Semaphore(maxConcurrentConnections, true);
    rateLimitProperties = new EmailRateLimitProperties();
    rateLimitProperties.setSemaphoreTimeoutSeconds(120);
    rateLimitProperties.setDelayBetweenEmailsMs(0);
    emailService =
        new EmailServiceImpl(
            javaMailSender,
            notificationEmailRepository,
            notificationService,
            emailRateLimitSemaphore,
            rateLimitProperties);
    ReflectionTestUtils.setField(emailService, "fromAddress", FROM_ADDRESS);

    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

    var notificationEmail =
        NotificationEmail.builder()
            .address(VALID_EMAIL)
            .message(VALID_MAIL_BODY)
            .sentAt(LocalDateTime.now())
            .build();

    when(notificationEmailRepository.save(any(NotificationEmail.class)))
        .thenReturn(notificationEmail);

    int totalEmails = 2500;
    int threadPoolSize = 20;

    CountDownLatch completionLatch = new CountDownLatch(totalEmails);
    ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);

    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failureCount = new AtomicInteger(0);
    AtomicInteger maxObservedConcurrency = new AtomicInteger(0);
    AtomicInteger currentConcurrency = new AtomicInteger(0);

    doAnswer(
            invocation -> {
              int concurrent = currentConcurrency.incrementAndGet();
              maxObservedConcurrency.updateAndGet(current -> Math.max(current, concurrent));
              Thread.sleep(1 + (int) (Math.random() * 4));
              currentConcurrency.decrementAndGet();
              return null;
            })
        .when(javaMailSender)
        .send(any(MimeMessage.class));

    long startTime = System.currentTimeMillis();

    for (int i = 0; i < totalEmails; i++) {
      final int emailIndex = i;
      executor.submit(
          () -> {
            try {
              String uniqueEmail = "user" + emailIndex + "@example.com";
              emailService.sendEmail(uniqueEmail, VALID_SUBJECT, VALID_MAIL_BODY);
              successCount.incrementAndGet();
            } catch (Exception e) {
              failureCount.incrementAndGet();
            } finally {
              completionLatch.countDown();
            }
          });
    }

    boolean completed = completionLatch.await(5, TimeUnit.MINUTES);
    long duration = System.currentTimeMillis() - startTime;

    executor.shutdown();
    executor.awaitTermination(10, TimeUnit.SECONDS);

    assertTrue(completed, "All emails should complete within timeout");
    assertEquals(
        totalEmails,
        successCount.get(),
        "All " + totalEmails + " emails should be sent successfully");
    assertEquals(0, failureCount.get(), "No emails should fail");

    assertTrue(
        maxObservedConcurrency.get() <= maxConcurrentConnections,
        "Concurrent SMTP connections should never exceed "
            + maxConcurrentConnections
            + ", but observed: "
            + maxObservedConcurrency.get());

    assertEquals(
        maxConcurrentConnections,
        emailRateLimitSemaphore.availablePermits(),
        "All semaphore permits should be released after completion");

    log.info("High volume test completed:");
    log.info("  - Total emails: %d".formatted(totalEmails));
    log.info("  - Duration: %dms".formatted(duration));
    log.info("  - Throughput: %.2f emails/sec".formatted(totalEmails * 1000.0 / duration));
    log.info("  - Max observed concurrency: %d".formatted(maxObservedConcurrency.get()));
  }

  @Test
  void sendEmail_HighVolume_WithFailures_ReleasesAllSemaphorePermits() throws InterruptedException {
    int maxConcurrentConnections = 2;
    emailRateLimitSemaphore = new Semaphore(maxConcurrentConnections, true);
    rateLimitProperties = new EmailRateLimitProperties();
    rateLimitProperties.setSemaphoreTimeoutSeconds(60);
    rateLimitProperties.setDelayBetweenEmailsMs(0);
    emailService =
        new EmailServiceImpl(
            javaMailSender,
            notificationEmailRepository,
            notificationService,
            emailRateLimitSemaphore,
            rateLimitProperties);
    ReflectionTestUtils.setField(emailService, "fromAddress", FROM_ADDRESS);

    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

    var notificationEmail =
        NotificationEmail.builder()
            .address(VALID_EMAIL)
            .message(VALID_MAIL_BODY)
            .sentAt(LocalDateTime.now())
            .build();

    when(notificationEmailRepository.save(any(NotificationEmail.class)))
        .thenReturn(notificationEmail);

    int totalEmails = 100;
    int failEveryN = 10;

    CountDownLatch completionLatch = new CountDownLatch(totalEmails);
    ExecutorService executor = Executors.newFixedThreadPool(10);

    AtomicInteger sendAttempt = new AtomicInteger(0);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failureCount = new AtomicInteger(0);

    doAnswer(
            invocation -> {
              int attempt = sendAttempt.incrementAndGet();
              if (attempt % failEveryN == 0) {
                throw new MailSendException("Simulated SMTP failure");
              }
              Thread.sleep(1);
              return null;
            })
        .when(javaMailSender)
        .send(any(MimeMessage.class));

    for (int i = 0; i < totalEmails; i++) {
      executor.submit(
          () -> {
            try {
              emailService.sendEmail(VALID_EMAIL, VALID_SUBJECT, VALID_MAIL_BODY);
              successCount.incrementAndGet();
            } catch (Exception e) {
              failureCount.incrementAndGet();
            } finally {
              completionLatch.countDown();
            }
          });
    }

    completionLatch.await(2, TimeUnit.MINUTES);
    executor.shutdown();
    executor.awaitTermination(10, TimeUnit.SECONDS);

    assertEquals(
        maxConcurrentConnections,
        emailRateLimitSemaphore.availablePermits(),
        "All semaphore permits should be released even with failures");

    assertTrue(failureCount.get() > 0, "Test should have some failures to verify cleanup");
    assertEquals(
        totalEmails, successCount.get() + failureCount.get(), "All emails should be accounted for");
  }
}
