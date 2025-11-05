package eu.bbmri_eric.negotiator.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.negotiator.email.EmailService;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@EnableAsync
@TestPropertySource(properties = "spring.task.execution.pool.core-size=1")
@IntegrationTest(loadTestData = true)
public class EmailNotificationRequestListenerTest {

  @Mock private NotificationRepository notificationRepository;

  @InjectMocks private EmailNotificationRequestListener emailNotificationRequestListener;

  @Mock private NotificationService notificationService;

  @Mock private PersonRepository personRepository;

  @Mock private NegotiationRepository negotiationRepository;

  @Mock private EmailContextBuilder emailContextBuilder;

  @Mock private EmailService emailService;

  @Test
  void testNotification_whenNullNegotiation() {
    NotificationDTO notificationDTO =
        NotificationDTO.builder()
            .id(1L)
            .title("Test Notification")
            .message("This is a test notification message")
            .recipientId(100L)
            .negotiationId(UUID.randomUUID().toString())
            .build();

    Notification notification =
        Notification.builder()
            .id(1L)
            .title("Test Notification")
            .message("This is a test notification message")
            .recipientId(100L)
            .negotiationId(UUID.randomUUID().toString())
            .build();

    Person person = Person.builder().id(100L).email("test@example.com").name("test").build();
    when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
    when(notificationService.findById(1L)).thenReturn(notificationDTO);
    when(personRepository.findById(100L)).thenReturn(Optional.ofNullable(person));
    when(negotiationRepository.findById(notificationDTO.getNegotiationId()))
        .thenReturn(Optional.empty());
    when(emailContextBuilder.buildEmailContent(
            "test", "This is a test notification message", null, null, null))
        .thenReturn("Email Content");

    NewNotificationEvent event = new NewNotificationEvent(1L, 1L);
    emailNotificationRequestListener.onNewNotification(event);
    verify(emailService).sendEmail(person, "Test Notification", "Email Content", null, null);
  }

  @Test
  void testNotification_whenNegotiationPresent_emailAttachedInThread() {
    NotificationDTO notificationDTO =
        NotificationDTO.builder()
            .id(1L)
            .title("Test Notification")
            .message("This is a test notification message")
            .recipientId(100L)
            .negotiationId(UUID.randomUUID().toString())
            .build();

    Notification notification =
        Notification.builder()
            .id(1L)
            .title("Test Notification")
            .message("This is a test notification message")
            .recipientId(100L)
            .negotiationId(UUID.randomUUID().toString())
            .build();

    Person person = Person.builder().id(100L).email("test@example.com").name("test").build();

    Negotiation negotiation =
        Negotiation.builder().id("test-negotiation").title("Test Negotiation").build();
    when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
    when(notificationRepository.existsByRecipientIdAndNegotiationId(100L, "test-negotiation"))
        .thenReturn(true);
    when(notificationService.findById(1L)).thenReturn(notificationDTO);
    when(personRepository.findById(100L)).thenReturn(Optional.ofNullable(person));
    when(negotiationRepository.findById(notificationDTO.getNegotiationId()))
        .thenReturn(Optional.of(negotiation));
    when(emailContextBuilder.buildEmailContent(
            "test",
            "This is a test notification message",
            "test-negotiation",
            "Test Negotiation",
            null))
        .thenReturn("Email Content");
    NewNotificationEvent event = new NewNotificationEvent(1L, 1L);
    emailNotificationRequestListener.onNewNotification(event);
    verify(emailService)
        .sendEmail(
            eq(person),
            eq("Test Notification"),
            eq("Email Content"),
            eq("test-negotiation"),
            any());
  }

  @Test
  void testNotification_whenNegotiationPresent_threadInitialized() {
    NotificationDTO notificationDTO =
        NotificationDTO.builder()
            .id(1L)
            .title("Test Notification")
            .message("This is a test notification message")
            .recipientId(100L)
            .negotiationId(UUID.randomUUID().toString())
            .build();

    Notification notification =
        Notification.builder()
            .id(1L)
            .title("Test Notification")
            .message("This is a test notification message")
            .recipientId(100L)
            .negotiationId(UUID.randomUUID().toString())
            .build();

    Person person = Person.builder().id(100L).email("test@example.com").name("test").build();

    Negotiation negotiation =
        Negotiation.builder().id("test-negotiation").title("Test Negotiation").build();
    when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
    when(notificationRepository.existsByRecipientIdAndNegotiationId(100L, "test-negotiation"))
        .thenReturn(false);
    when(notificationService.findById(1L)).thenReturn(notificationDTO);
    when(personRepository.findById(100L)).thenReturn(Optional.ofNullable(person));
    when(negotiationRepository.findById(notificationDTO.getNegotiationId()))
        .thenReturn(Optional.of(negotiation));
    when(emailContextBuilder.buildEmailContent(
            "test",
            "This is a test notification message",
            "test-negotiation",
            "Test Negotiation",
            null))
        .thenReturn("Email Content");
    NewNotificationEvent event = new NewNotificationEvent(1L, 1L);
    emailNotificationRequestListener.onNewNotification(event);
    verify(emailService)
        .sendEmail(
            eq(person),
            eq("Test Notification"),
            eq("Email Content"),
            eq("test-negotiation"),
            eq("test-negotiation"));
  }
}
