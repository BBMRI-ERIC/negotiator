package eu.bbmri_eric.negotiator.notification.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationStateChangeEvent;
import eu.bbmri_eric.negotiator.notification.NotificationCreateDTO;
import eu.bbmri_eric.negotiator.notification.NotificationService;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NegotiationSubmissionHandlerTest {

  @Mock private NotificationService notificationService;

  @Mock private PersonRepository personRepository;

  private NegotiationSubmissionHandler handler;

  @BeforeEach
  void setUp() {
    handler = new NegotiationSubmissionHandler(notificationService, personRepository);
  }

  @Test
  void getSupportedEventType_ReturnsCorrectEventType() {
    // When
    Class<NegotiationStateChangeEvent> eventType = handler.getSupportedEventType();

    // Then
    assertEquals(NegotiationStateChangeEvent.class, eventType);
  }

  @Test
  void notify_WhenNegotiationSubmitted_NotifiesAllAdmins() {
    // Given
    String negotiationId = "NEG-123";
    NegotiationStateChangeEvent event =
        new NegotiationStateChangeEvent(
            this,
            negotiationId,
            NegotiationState.DRAFT,
            NegotiationState.SUBMITTED,
            NegotiationEvent.SUBMIT);

    Person admin1 = createPerson(1L, "admin1@test.com");
    Person admin2 = createPerson(2L, "admin2@test.com");
    List<Person> admins = Arrays.asList(admin1, admin2);

    when(personRepository.findAllByAdminIsTrue()).thenReturn(admins);

    // When
    handler.notify(event);

    // Then
    ArgumentCaptor<NotificationCreateDTO> notificationCaptor =
        ArgumentCaptor.forClass(NotificationCreateDTO.class);
    verify(notificationService).createNotifications(notificationCaptor.capture());

    NotificationCreateDTO notification = notificationCaptor.getValue();
    assertEquals(Arrays.asList(1L, 2L), notification.getUserIds());
    assertEquals("New Request", notification.getTitle());
    assertEquals("A new Request has been submitted for review", notification.getBody());
    assertEquals(negotiationId, notification.getNegotiationId());
  }

  @Test
  void notify_WhenNoAdmins_DoesNotCreateNotifications() {
    // Given
    String negotiationId = "NEG-123";
    NegotiationStateChangeEvent event =
        new NegotiationStateChangeEvent(
            this,
            negotiationId,
            NegotiationState.DRAFT,
            NegotiationState.SUBMITTED,
            NegotiationEvent.SUBMIT);

    when(personRepository.findAllByAdminIsTrue()).thenReturn(Collections.emptyList());

    // When
    handler.notify(event);

    // Then
    verify(notificationService, never()).createNotifications(any());
  }

  @Test
  void notify_WhenNotSubmittedState_DoesNotCreateNotifications() {
    // Given
    String negotiationId = "NEG-123";
    NegotiationStateChangeEvent event =
        new NegotiationStateChangeEvent(
            this,
            negotiationId,
            NegotiationState.SUBMITTED,
            NegotiationState.IN_PROGRESS,
            NegotiationEvent.APPROVE);

    // When
    handler.notify(event);

    // Then
    verify(personRepository, never()).findAllByAdminIsTrue();
    verify(notificationService, never()).createNotifications(any());
  }

  @Test
  void notify_WhenMultipleStateChanges_OnlyNotifiesForSubmitted() {
    // Given
    String negotiationId = "NEG-123";
    Person admin = createPerson(1L, "admin@test.com");
    when(personRepository.findAllByAdminIsTrue()).thenReturn(List.of(admin));

    // Test various state changes
    List<NegotiationState> nonSubmittedStates =
        Arrays.asList(
            NegotiationState.IN_PROGRESS,
            NegotiationState.APPROVED,
            NegotiationState.DECLINED,
            NegotiationState.ABANDONED);

    for (NegotiationState state : nonSubmittedStates) {
      NegotiationStateChangeEvent event =
          new NegotiationStateChangeEvent(
              this, negotiationId, NegotiationState.SUBMITTED, state, NegotiationEvent.APPROVE);

      // When
      handler.notify(event);
    }

    // Then - should not have been called for any of these states
    verify(notificationService, never()).createNotifications(any());

    // But should work for SUBMITTED
    NegotiationStateChangeEvent submittedEvent =
        new NegotiationStateChangeEvent(
            this,
            negotiationId,
            NegotiationState.DRAFT,
            NegotiationState.SUBMITTED,
            NegotiationEvent.SUBMIT);
    handler.notify(submittedEvent);

    verify(notificationService, times(1)).createNotifications(any());
  }

  private Person createPerson(Long id, String email) {
    Person person = new Person();
    person.setId(id);
    person.setEmail(email);
    return person;
  }
}
