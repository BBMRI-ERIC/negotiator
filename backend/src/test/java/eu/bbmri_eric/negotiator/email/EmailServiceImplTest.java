package eu.bbmri_eric.negotiator.email;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import eu.bbmri_eric.negotiator.user.Person;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
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
class EmailServiceImplTest {

  @Mock private JavaMailSender javaMailSender;

  @Mock private NotificationEmailRepository notificationEmailRepository;

  @Mock private MimeMessage mimeMessage;
  @Mock private EmailContextBuilder emailContextBuilder;

  private EmailServiceImpl emailService;

  private static final String FROM_ADDRESS = "noreply@negotiator.com";
  private static final String VALID_EMAIL = "test@example.com";
  private static final String VALID_SUBJECT = "Test Subject";
  private static final String VALID_MAIL_BODY = "Test mail body content";

  @BeforeEach
  void setUp() {
    emailService = new EmailServiceImpl(javaMailSender, notificationEmailRepository);
    ReflectionTestUtils.setField(emailService, "fromAddress", FROM_ADDRESS);

    // Use lenient stubbing to avoid unnecessary stubbing exceptions for tests that don't use it
    lenient()
        .when(
            emailContextBuilder.buildEmailContent(
                anyString(), anyString(), anyString(), any(), any(), any()))
        .thenReturn(VALID_MAIL_BODY);
  }

  @Test
  void constructor_WithNullJavaMailSender_ThrowsNullPointerException() {
    assertThrows(
        NullPointerException.class, () -> new EmailServiceImpl(null, notificationEmailRepository));
  }

  @Test
  void constructor_WithNullRepository_ThrowsNullPointerException() {
    assertThrows(NullPointerException.class, () -> new EmailServiceImpl(javaMailSender, null));
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

    assertDoesNotThrow(() -> emailService.sendEmail(person, VALID_SUBJECT, VALID_MAIL_BODY));

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
        () -> emailService.sendEmail((Person) null, VALID_SUBJECT, VALID_MAIL_BODY));
  }

  @Test
  void sendEmail_WithPersonHavingInvalidEmail_throwsException() {
    var person = Person.builder().id(1L).email("invalid-email").name("Test User").build();

    assertThrows(
        IllegalArgumentException.class,
        () -> emailService.sendEmail(person, VALID_SUBJECT, VALID_MAIL_BODY));

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
}
