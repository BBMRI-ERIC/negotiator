package eu.bbmri_eric.negotiator.notification.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationStateChangeEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.notification.NotificationCreateDTO;
import eu.bbmri_eric.negotiator.notification.NotificationService;
import eu.bbmri_eric.negotiator.user.Person;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NegotiationInProgressHandlerTest {

  @Mock private NotificationService notificationService;
  @Mock private NegotiationRepository negotiationRepository;
  @Mock private Negotiation negotiation;
  @Mock private Resource resource1;
  @Mock private Resource resource2;

  private NegotiationInProgressHandler handler;

  @BeforeEach
  void setUp() {
    handler = new NegotiationInProgressHandler(notificationService, negotiationRepository);
  }

  @Test
  void getSupportedEventType_ReturnsCorrectEventType() {
    Class<NegotiationStateChangeEvent> eventType = handler.getSupportedEventType();
    assertEquals(NegotiationStateChangeEvent.class, eventType);
  }

  @Test
  void notify_WhenNegotiationInProgress_NotifiesRepresentatives() {
    String negotiationId = "NEG-123";
    NegotiationStateChangeEvent event =
        new NegotiationStateChangeEvent(
            this,
            negotiationId,
            NegotiationState.IN_PROGRESS,
            NegotiationEvent.APPROVE,
            "Test post");

    Person rep1 = createPerson(1L, "rep1@test.com");
    Person rep2 = createPerson(2L, "rep2@test.com");

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));
    when(negotiation.getResources()).thenReturn(Set.of(resource1, resource2));
    when(resource1.getSourceId()).thenReturn("resource-1");
    when(resource2.getSourceId()).thenReturn("resource-2");
    when(resource1.getRepresentatives()).thenReturn(Set.of(rep1));
    when(resource2.getRepresentatives()).thenReturn(Set.of(rep2));

    handler.notify(event);

    verify(negotiation)
        .setStateForResource("resource-1", NegotiationResourceState.REPRESENTATIVE_CONTACTED);
    verify(negotiation)
        .setStateForResource("resource-2", NegotiationResourceState.REPRESENTATIVE_CONTACTED);

    ArgumentCaptor<NotificationCreateDTO> notificationCaptor =
        ArgumentCaptor.forClass(NotificationCreateDTO.class);
    verify(notificationService).createNotifications(notificationCaptor.capture());

    NotificationCreateDTO notification = notificationCaptor.getValue();
    assertEquals(2, notification.getUserIds().size());
    assertTrue(notification.getUserIds().contains(1L));
    assertTrue(notification.getUserIds().contains(2L));
    assertEquals("New Negotiation Request", notification.getTitle());
    assertEquals(
        "A new negotiation request requires your attention. Please review the details and respond accordingly.",
        notification.getBody());
    assertEquals(negotiationId, notification.getNegotiationId());
  }

  @Test
  void notify_WhenNegotiationNotFound_DoesNotCreateNotifications() {
    String negotiationId = "NEG-123";
    NegotiationStateChangeEvent event =
        new NegotiationStateChangeEvent(
            this,
            negotiationId,
            NegotiationState.IN_PROGRESS,
            NegotiationEvent.APPROVE,
            "Test post");

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.empty());

    assertThrows(RuntimeException.class, () -> handler.notify(event));
    verify(notificationService, never()).createNotifications(any());
  }

  @Test
  void notify_WhenAllResourcesHaveNoRepresentatives_OnlyUpdatesStatesNoNotifications() {
    String negotiationId = "NEG-123";
    NegotiationStateChangeEvent event =
        new NegotiationStateChangeEvent(
            this,
            negotiationId,
            NegotiationState.IN_PROGRESS,
            NegotiationEvent.APPROVE,
            "Test post");

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));
    when(negotiation.getResources()).thenReturn(Set.of(resource1, resource2));
    when(resource1.getSourceId()).thenReturn("resource-1");
    when(resource2.getSourceId()).thenReturn("resource-2");
    when(resource1.getRepresentatives()).thenReturn(Set.of());
    when(resource2.getRepresentatives()).thenReturn(Set.of());

    handler.notify(event);

    verify(negotiation)
        .setStateForResource("resource-1", NegotiationResourceState.REPRESENTATIVE_UNREACHABLE);
    verify(negotiation)
        .setStateForResource("resource-2", NegotiationResourceState.REPRESENTATIVE_UNREACHABLE);
    verify(notificationService, never()).createNotifications(any());
  }

  @Test
  void notify_WhenNotInProgressState_DoesNothing() {
    String negotiationId = "NEG-123";
    NegotiationStateChangeEvent event =
        new NegotiationStateChangeEvent(
            this, negotiationId, NegotiationState.SUBMITTED, NegotiationEvent.SUBMIT, "Test post");

    handler.notify(event);

    verify(negotiationRepository, never()).findById(any());
    verify(notificationService, never()).createNotifications(any());
  }

  private Person createPerson(Long id, String email) {
    Person person = new Person();
    person.setId(id);
    person.setEmail(email);

    // Override equals and hashCode to ensure proper Set behavior
    return new Person() {
      @Override
      public Long getId() {
        return id;
      }

      @Override
      public String getEmail() {
        return email;
      }

      @Override
      public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Person person = (Person) obj;
        return Objects.equals(id, person.getId());
      }

      @Override
      public int hashCode() {
        return Objects.hash(id);
      }
    };
  }
}
