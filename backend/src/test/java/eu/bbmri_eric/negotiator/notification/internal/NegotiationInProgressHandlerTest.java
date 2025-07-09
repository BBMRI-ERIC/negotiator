package eu.bbmri_eric.negotiator.notification.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import eu.bbmri_eric.negotiator.common.AuthenticatedUserContext;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationStateChangeEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.ResourceLifecycleService;
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

  @Mock private ResourceLifecycleService resourceLifecycleService;

  @Mock private AuthenticatedUserContext authenticatedUserContext;

  @Mock private Negotiation negotiation;

  @Mock private Resource resource1;

  @Mock private Resource resource2;

  private NegotiationInProgressHandler handler;

  @BeforeEach
  void setUp() {
    handler =
        new NegotiationInProgressHandler(
            notificationService,
            negotiationRepository,
            resourceLifecycleService,
            authenticatedUserContext);
  }

  @Test
  void getSupportedEventType_ReturnsCorrectEventType() {
    // When
    Class<NegotiationStateChangeEvent> eventType = handler.getSupportedEventType();

    // Then
    assertEquals(NegotiationStateChangeEvent.class, eventType);
  }

  @Test
  void notify_WhenNegotiationInProgress_NotifiesRepresentatives() {
    // Given
    String negotiationId = "NEG-123";
    NegotiationStateChangeEvent event =
        new NegotiationStateChangeEvent(
            this, negotiationId, NegotiationState.IN_PROGRESS, NegotiationEvent.START, "Test post");

    Person rep1 = createPerson(1L, "rep1@test.com");
    Person rep2 = createPerson(2L, "rep2@test.com");

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));
    when(negotiation.getResources()).thenReturn(Set.of(resource1, resource2));

    when(resource1.getSourceId()).thenReturn("resource-1");
    when(resource2.getSourceId()).thenReturn("resource-2");

    when(resource1.getRepresentatives()).thenReturn(Set.of(rep1, rep2));

    // When
    handler.notify(event);

    // Then
    ArgumentCaptor<NotificationCreateDTO> notificationCaptor =
        ArgumentCaptor.forClass(NotificationCreateDTO.class);
    verify(notificationService).createNotifications(notificationCaptor.capture());

    NotificationCreateDTO notification = notificationCaptor.getValue();
    assertTrue(notification.getUserIds().containsAll(Arrays.asList(1L, 2L)));
    assertEquals("New Negotiation Request", notification.getTitle());
    assertEquals(
        "A new negotiation request requires your attention. Please review the details and respond accordingly.",
        notification.getBody());
    assertEquals(negotiationId, notification.getNegotiationId());
  }

  @Test
  void notify_WhenNegotiationInProgress_UpdatesResourceStatesAndNotifiesRepresentatives() {
    // Given
    String negotiationId = "NEG-123";
    NegotiationStateChangeEvent event =
        new NegotiationStateChangeEvent(
            this, negotiationId, NegotiationState.IN_PROGRESS, NegotiationEvent.START, "Test post");

    Person rep1 = createPerson(1L, "rep1@test.com");
    Person rep2 = createPerson(2L, "rep2@test.com");

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));
    when(negotiation.getResources()).thenReturn(Set.of(resource1, resource2));

    when(resource1.getSourceId()).thenReturn("resource-1");
    when(resource2.getSourceId()).thenReturn("resource-2");

    // resource1 has representatives, resource2 doesn't
    when(resource1.getRepresentatives()).thenReturn(Set.of(rep1, rep2));
    when(resource2.getRepresentatives()).thenReturn(Set.of());

    // When
    handler.notify(event);

    // Then
    // Verify resource state updates
    verify(resourceLifecycleService)
        .sendEvent(negotiationId, "resource-1", NegotiationResourceEvent.CONTACT);
    verify(resourceLifecycleService)
        .sendEvent(negotiationId, "resource-2", NegotiationResourceEvent.MARK_AS_UNREACHABLE);

    // Verify notification sent to representatives
    ArgumentCaptor<NotificationCreateDTO> notificationCaptor =
        ArgumentCaptor.forClass(NotificationCreateDTO.class);
    verify(notificationService).createNotifications(notificationCaptor.capture());

    NotificationCreateDTO notification = notificationCaptor.getValue();
    assertTrue(notification.getUserIds().containsAll(Arrays.asList(1L, 2L)));
    assertEquals("New Negotiation Request", notification.getTitle());
    assertEquals(
        "A new negotiation request requires your attention. Please review the details and respond accordingly.",
        notification.getBody());
    assertEquals(negotiationId, notification.getNegotiationId());
  }

  @Test
  void notify_WhenNegotiationNotFound_DoesNotCreateNotifications() {
    // Given
    String negotiationId = "NEG-123";
    NegotiationStateChangeEvent event =
        new NegotiationStateChangeEvent(
            this, negotiationId, NegotiationState.IN_PROGRESS, NegotiationEvent.START, "Test post");

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.empty());

    // When
    handler.notify(event);

    // Then
    verify(notificationService, never()).createNotifications(any());
    verify(resourceLifecycleService, never()).sendEvent(any(), any(), any());
  }

  @Test
  void notify_WhenAllResourcesHaveNoRepresentatives_OnlyUpdatesStatesNoNotifications() {
    // Given
    String negotiationId = "NEG-123";
    NegotiationStateChangeEvent event =
        new NegotiationStateChangeEvent(
            this, negotiationId, NegotiationState.IN_PROGRESS, NegotiationEvent.START, "Test post");

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));
    when(negotiation.getResources()).thenReturn(Set.of(resource1, resource2));
    when(resource1.getSourceId()).thenReturn("resource-1");
    when(resource2.getSourceId()).thenReturn("resource-2");
    when(resource1.getRepresentatives()).thenReturn(Set.of());
    when(resource2.getRepresentatives()).thenReturn(Set.of());

    // When
    handler.notify(event);

    // Then
    verify(resourceLifecycleService)
        .sendEvent(negotiationId, "resource-1", NegotiationResourceEvent.MARK_AS_UNREACHABLE);
    verify(resourceLifecycleService)
        .sendEvent(negotiationId, "resource-2", NegotiationResourceEvent.MARK_AS_UNREACHABLE);
    verify(notificationService, never()).createNotifications(any());
  }

  @Test
  void notify_WhenNotInProgressState_DoesNothing() {
    // Given
    String negotiationId = "NEG-123";
    NegotiationStateChangeEvent event =
        new NegotiationStateChangeEvent(
            this, negotiationId, NegotiationState.APPROVED, NegotiationEvent.APPROVE, "Test post");

    // When
    handler.notify(event);

    // Then
    verify(negotiationRepository, never()).findById(any());
    verify(notificationService, never()).createNotifications(any());
    verify(resourceLifecycleService, never()).sendEvent(any(), any(), any());
  }

  @Test
  void notify_WhenResourceServiceThrowsException_ContinuesProcessing() {
    // Given
    String negotiationId = "NEG-123";
    NegotiationStateChangeEvent event =
        new NegotiationStateChangeEvent(
            this, negotiationId, NegotiationState.IN_PROGRESS, NegotiationEvent.START, "Test post");

    Person rep1 = createPerson(1L, "rep1@test.com");

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));
    when(negotiation.getResources()).thenReturn(Set.of(resource1, resource2));
    when(resource1.getSourceId()).thenReturn("resource-1");
    when(resource2.getSourceId()).thenReturn("resource-2");
    when(resource1.getRepresentatives()).thenReturn(Set.of(rep1));
    when(resource2.getRepresentatives()).thenReturn(Set.of());

    // Make resource lifecycle service throw exception for first resource
    doThrow(new RuntimeException("State machine error"))
        .when(resourceLifecycleService)
        .sendEvent(negotiationId, "resource-1", NegotiationResourceEvent.CONTACT);

    // When
    handler.notify(event);

    // Then
    // Should still process second resource despite first one failing
    verify(resourceLifecycleService)
        .sendEvent(negotiationId, "resource-2", NegotiationResourceEvent.MARK_AS_UNREACHABLE);
    // Should still send notification despite state update failure
    verify(notificationService).createNotifications(any());
  }

  private Person createPerson(Long id, String email) {
    Person person = new Person();
    person.setId(id);
    person.setEmail(email);
    // Override equals/hashCode to prevent duplicate issues in Set.of()
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
        return Objects.equals(getId(), person.getId());
      }

      @Override
      public int hashCode() {
        return Objects.hash(getId());
      }
    };
  }
}
