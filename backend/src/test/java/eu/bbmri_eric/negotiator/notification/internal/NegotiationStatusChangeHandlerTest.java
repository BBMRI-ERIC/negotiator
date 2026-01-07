package eu.bbmri_eric.negotiator.notification.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationStateChangeEvent;
import eu.bbmri_eric.negotiator.notification.NotificationCreateDTO;
import eu.bbmri_eric.negotiator.notification.NotificationService;
import eu.bbmri_eric.negotiator.user.Person;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NegotiationStatusChangeHandlerTest {

  @Mock private NotificationService notificationService;

  @Mock private NegotiationRepository negotiationRepository;

  @Mock private Negotiation negotiation;

  @Mock private Person researcher;

  private NegotiationStatusChangeHandler handler;

  @BeforeEach
  void setUp() {
    handler = new NegotiationStatusChangeHandler(notificationService, negotiationRepository);

    // Setup common mock behavior - only when needed, using lenient stubs
    lenient().when(researcher.getId()).thenReturn(123L);
    lenient().when(negotiation.getCreatedBy()).thenReturn(researcher);
    lenient().when(negotiation.getTitle()).thenReturn("Test Negotiation");
  }

  @Test
  void getSupportedEventType_ReturnsCorrectEventType() {
    // When
    Class<NegotiationStateChangeEvent> eventType = handler.getSupportedEventType();

    // Then
    assertEquals(NegotiationStateChangeEvent.class, eventType);
  }

  @Test
  void notify_WhenNegotiationSubmitted_SendsConfirmationNotification() {
    // Given
    String negotiationId = "NEG-123";
    NegotiationStateChangeEvent event =
        new NegotiationStateChangeEvent(
            this, negotiationId, NegotiationState.SUBMITTED, NegotiationEvent.SUBMIT, "Test post");

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));

    // When
    handler.notify(event);

    // Then
    ArgumentCaptor<NotificationCreateDTO> notificationCaptor =
        ArgumentCaptor.forClass(NotificationCreateDTO.class);
    verify(notificationService).createNotifications(notificationCaptor.capture());

    NotificationCreateDTO notification = notificationCaptor.getValue();
    assertEquals(List.of(123L), notification.getUserIds());
    assertEquals("New notification for Negotiation:", notification.getTitle());
    assertEquals(
        "Your negotiation request has been successfully submitted and is now under review. You will be notified of any updates.",
        notification.getBody());
    assertEquals(negotiationId, notification.getNegotiationId());
  }

  @Test
  void notify_WhenNegotiationApproved_SendsStatusChangeNotification() {
    // Given
    String negotiationId = "NEG-123";
    NegotiationStateChangeEvent event =
        new NegotiationStateChangeEvent(
            this,
            negotiationId,
            NegotiationState.IN_PROGRESS,
            NegotiationEvent.APPROVE,
            "Test post");

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));

    // When
    handler.notify(event);

    // Then
    ArgumentCaptor<NotificationCreateDTO> notificationCaptor =
        ArgumentCaptor.forClass(NotificationCreateDTO.class);
    verify(notificationService).createNotifications(notificationCaptor.capture());

    NotificationCreateDTO notification = notificationCaptor.getValue();
    assertEquals(List.of(123L), notification.getUserIds());
    assertEquals("New notification for Negotiation:", notification.getTitle());
    assertTrue(notification.getBody().contains("Test Negotiation"));
    assertTrue(notification.getBody().contains("has been approved"));
    assertEquals(negotiationId, notification.getNegotiationId());
  }

  @Test
  void notify_WhenNegotiationDeclined_SendsStatusChangeNotification() {
    // Given
    String negotiationId = "NEG-123";
    NegotiationStateChangeEvent event =
        new NegotiationStateChangeEvent(
            this, negotiationId, NegotiationState.DECLINED, NegotiationEvent.DECLINE, "Test post");

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));

    // When
    handler.notify(event);

    // Then
    ArgumentCaptor<NotificationCreateDTO> notificationCaptor =
        ArgumentCaptor.forClass(NotificationCreateDTO.class);
    verify(notificationService).createNotifications(notificationCaptor.capture());

    NotificationCreateDTO notification = notificationCaptor.getValue();
    assertEquals(List.of(123L), notification.getUserIds());
    assertEquals("New notification for Negotiation:", notification.getTitle());
    assertTrue(notification.getBody().contains("Test Negotiation"));
    assertTrue(notification.getBody().contains("has been rejected"));
    assertEquals(negotiationId, notification.getNegotiationId());
  }

  @Test
  void notify_WhenNegotiationAbandoned_SendsStatusChangeNotification() {
    // Given
    String negotiationId = "NEG-123";
    NegotiationStateChangeEvent event =
        new NegotiationStateChangeEvent(
            this, negotiationId, NegotiationState.ABANDONED, NegotiationEvent.ABANDON, "Test post");

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));

    // When
    handler.notify(event);

    // Then
    ArgumentCaptor<NotificationCreateDTO> notificationCaptor =
        ArgumentCaptor.forClass(NotificationCreateDTO.class);
    verify(notificationService).createNotifications(notificationCaptor.capture());

    NotificationCreateDTO notification = notificationCaptor.getValue();
    assertEquals(List.of(123L), notification.getUserIds());
    assertEquals("New notification for Negotiation:", notification.getTitle());
    assertTrue(notification.getBody().contains("Test Negotiation"));
    assertTrue(notification.getBody().contains("has been marked as abandoned"));
    assertEquals(negotiationId, notification.getNegotiationId());
  }

  @Test
  void notify_WhenNegotiationNotFound_DoesNotCreateNotifications() {
    // Given
    String negotiationId = "NEG-123";
    NegotiationStateChangeEvent event =
        new NegotiationStateChangeEvent(
            this, negotiationId, NegotiationState.SUBMITTED, NegotiationEvent.SUBMIT, "Test post");

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.empty());

    // When
    handler.notify(event);

    // Then
    verify(notificationService, never()).createNotifications(any());
  }

  @Test
  void notify_WhenUnhandledState_DoesNotCreateNotifications() {
    // Given
    String negotiationId = "NEG-123";
    NegotiationStateChangeEvent event =
        new NegotiationStateChangeEvent(
            this, negotiationId, NegotiationState.DRAFT, NegotiationEvent.SUBMIT, "Test post");

    // When
    handler.notify(event);

    // Then
    verify(notificationService, never()).createNotifications(any());
  }

  @Test
  void notify_MessageContentValidation() {
    // Given
    String negotiationId = "NEG-123";
    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));

    // Test IN_PROGRESS message content
    NegotiationStateChangeEvent inProgressEvent =
        new NegotiationStateChangeEvent(
            this,
            negotiationId,
            NegotiationState.IN_PROGRESS,
            NegotiationEvent.APPROVE,
            "Test post");
    handler.notify(inProgressEvent);

    ArgumentCaptor<NotificationCreateDTO> captor =
        ArgumentCaptor.forClass(NotificationCreateDTO.class);
    verify(notificationService, times(1)).createNotifications(captor.capture());

    String approvedMessage = captor.getValue().getBody();
    assertTrue(approvedMessage.contains("has been approved"));
    assertTrue(approvedMessage.contains("Test Negotiation"));

    // Reset mocks for next test
    reset(notificationService);

    // Test DECLINED message content
    NegotiationStateChangeEvent declinedEvent =
        new NegotiationStateChangeEvent(
            this, negotiationId, NegotiationState.DECLINED, NegotiationEvent.DECLINE, "Test post");
    handler.notify(declinedEvent);

    verify(notificationService, times(1)).createNotifications(captor.capture());
    String declinedMessage = captor.getValue().getBody();
    assertTrue(declinedMessage.contains("has been rejected"));
    assertTrue(declinedMessage.contains("Test Negotiation"));

    // Reset mocks for next test
    reset(notificationService);

    // Test ABANDONED message content
    NegotiationStateChangeEvent abandonedEvent =
        new NegotiationStateChangeEvent(
            this, negotiationId, NegotiationState.ABANDONED, NegotiationEvent.ABANDON, "Test post");
    handler.notify(abandonedEvent);

    verify(notificationService, times(1)).createNotifications(captor.capture());
    String abandonedMessage = captor.getValue().getBody();
    assertTrue(abandonedMessage.contains("has been marked as abandoned"));
    assertTrue(abandonedMessage.contains("Test Negotiation"));
  }
}
