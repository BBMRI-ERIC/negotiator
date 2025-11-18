package eu.bbmri_eric.negotiator.notification.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
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
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ResourceNotificationServiceTest {

  @Mock private NotificationService notificationService;
  @Mock private NegotiationRepository negotiationRepository;
  @Mock private Negotiation negotiation;
  @Mock private Resource resource1;
  @Mock private Resource resource2;
  @Mock private Resource resource3;
  @Mock private ApplicationEventPublisher applicationEventPublisher;

  private ResourceNotificationService service;

  @BeforeEach
  void setUp() {
    service = new ResourceNotificationService(notificationService, negotiationRepository);
  }

  @Test
  void notifyResourceRepresentatives_WhenNegotiationNotFound_ThrowsEntityNotFoundException() {
    String negotiationId = "123";
    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.empty());

    assertThrows(
        EntityNotFoundException.class, () -> service.notifyResourceRepresentatives(negotiationId));

    verify(notificationService, never()).createNotifications(any());
  }

  @Test
  void notifyResourceRepresentatives_WhenNegotiationNotInProgress_DoesNotProcessResources() {
    String negotiationId = "123";
    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));
    when(negotiation.getCurrentState()).thenReturn(NegotiationState.SUBMITTED);

    service.notifyResourceRepresentatives(negotiationId);

    verify(negotiation, never()).getResources();
    verify(notificationService, never()).createNotifications(any());
  }

  @Test
  void notifyResourceRepresentatives_WhenResourcesHaveExistingStates_SkipsThoseResources() {
    String negotiationId = "123";
    Person rep1 = createPerson(1L);

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));
    when(negotiation.getCurrentState()).thenReturn(NegotiationState.IN_PROGRESS);
    when(negotiation.getResources()).thenReturn(Set.of(resource1, resource2));

    when(resource1.getSourceId()).thenReturn("resource-1");
    when(resource2.getSourceId()).thenReturn("resource-2");

    // resource1 already has a state, resource2 doesn't
    when(negotiation.getCurrentStateForResource("resource-1"))
        .thenReturn(NegotiationResourceState.REPRESENTATIVE_CONTACTED);
    when(negotiation.getCurrentStateForResource("resource-2")).thenReturn(null);

    when(resource2.getRepresentatives()).thenReturn(Set.of(rep1));

    service.notifyResourceRepresentatives(negotiationId);

    // Only resource2 should be processed
    verify(negotiation, never()).setStateForResource(eq("resource-1"), any());
    verify(negotiation)
        .setStateForResource("resource-2", NegotiationResourceState.REPRESENTATIVE_CONTACTED);

    ArgumentCaptor<NotificationCreateDTO> notificationCaptor =
        ArgumentCaptor.forClass(NotificationCreateDTO.class);
    verify(notificationService).createNotifications(notificationCaptor.capture());

    NotificationCreateDTO notification = notificationCaptor.getValue();
    assertEquals(1, notification.getUserIds().size());
    assertTrue(notification.getUserIds().contains(1L));
  }

  @Test
  void notifyResourceRepresentatives_WhenResourcesHaveNoRepresentatives_SetsUnreachableState() {
    String negotiationId = "123";

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));
    when(negotiation.getCurrentState()).thenReturn(NegotiationState.IN_PROGRESS);
    when(negotiation.getResources()).thenReturn(Set.of(resource1, resource2));

    when(resource1.getSourceId()).thenReturn("resource-1");
    when(resource2.getSourceId()).thenReturn("resource-2");
    when(negotiation.getCurrentStateForResource("resource-1")).thenReturn(null);
    when(negotiation.getCurrentStateForResource("resource-2")).thenReturn(null);

    when(resource1.getRepresentatives()).thenReturn(Set.of());
    when(resource2.getRepresentatives()).thenReturn(Set.of());

    service.notifyResourceRepresentatives(negotiationId);

    verify(negotiation)
        .setStateForResource("resource-1", NegotiationResourceState.REPRESENTATIVE_UNREACHABLE);
    verify(negotiation)
        .setStateForResource("resource-2", NegotiationResourceState.REPRESENTATIVE_UNREACHABLE);

    // No notifications should be sent since no representatives
    verify(notificationService, never()).createNotifications(any());
  }

  @Test
  void
      notifyResourceRepresentatives_WhenResourcesHaveRepresentatives_SetsContactedStateAndNotifies() {
    String negotiationId = "123";
    Person rep1 = createPerson(1L);
    Person rep2 = createPerson(2L);

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));
    when(negotiation.getCurrentState()).thenReturn(NegotiationState.IN_PROGRESS);
    when(negotiation.getResources()).thenReturn(Set.of(resource1, resource2));

    when(resource1.getSourceId()).thenReturn("resource-1");
    when(resource2.getSourceId()).thenReturn("resource-2");
    when(negotiation.getCurrentStateForResource("resource-1")).thenReturn(null);
    when(negotiation.getCurrentStateForResource("resource-2")).thenReturn(null);

    when(resource1.getRepresentatives()).thenReturn(Set.of(rep1));
    when(resource2.getRepresentatives()).thenReturn(Set.of(rep2));

    service.notifyResourceRepresentatives(negotiationId);

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
    assertEquals(ResourceNotificationService.TITLE, notification.getTitle());
    assertEquals(ResourceNotificationService.BODY, notification.getBody());
    assertEquals(negotiationId, notification.getNegotiationId());
  }

  @Test
  void notifyResourceRepresentatives_WhenMixedResourceStates_ProcessesOnlyResourcesWithoutStates() {
    String negotiationId = "123";
    Person rep1 = createPerson(1L);

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));
    when(negotiation.getCurrentState()).thenReturn(NegotiationState.IN_PROGRESS);
    when(negotiation.getResources()).thenReturn(Set.of(resource1, resource2, resource3));

    when(resource1.getSourceId()).thenReturn("resource-1");
    when(resource2.getSourceId()).thenReturn("resource-2");
    when(resource3.getSourceId()).thenReturn("resource-3");

    // resource1: no state, has representative
    when(negotiation.getCurrentStateForResource("resource-1")).thenReturn(null);
    when(resource1.getRepresentatives()).thenReturn(Set.of(rep1));

    // resource2: already has state (should be skipped)
    when(negotiation.getCurrentStateForResource("resource-2"))
        .thenReturn(NegotiationResourceState.REPRESENTATIVE_CONTACTED);

    // resource3: no state, no representatives
    when(negotiation.getCurrentStateForResource("resource-3")).thenReturn(null);
    when(resource3.getRepresentatives()).thenReturn(Set.of());

    service.notifyResourceRepresentatives(negotiationId);

    verify(negotiation)
        .setStateForResource("resource-1", NegotiationResourceState.REPRESENTATIVE_CONTACTED);
    verify(negotiation, never()).setStateForResource(eq("resource-2"), any());
    verify(negotiation)
        .setStateForResource("resource-3", NegotiationResourceState.REPRESENTATIVE_UNREACHABLE);

    ArgumentCaptor<NotificationCreateDTO> notificationCaptor =
        ArgumentCaptor.forClass(NotificationCreateDTO.class);
    verify(notificationService).createNotifications(notificationCaptor.capture());

    NotificationCreateDTO notification = notificationCaptor.getValue();
    assertEquals(1, notification.getUserIds().size());
    assertTrue(notification.getUserIds().contains(1L));
  }

  @Test
  void notifyResourceRepresentatives_WhenRepresentativesContainNulls_FiltersOutNulls() {
    String negotiationId = "123";
    Person rep1 = createPerson(1L);

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));
    when(negotiation.getCurrentState()).thenReturn(NegotiationState.IN_PROGRESS);
    when(negotiation.getResources()).thenReturn(Set.of(resource1));

    when(resource1.getSourceId()).thenReturn("resource-1");
    when(negotiation.getCurrentStateForResource("resource-1")).thenReturn(null);

    // Create set with null value
    Set<Person> representativesWithNull = new HashSet<>();
    representativesWithNull.add(rep1);
    representativesWithNull.add(null);
    when(resource1.getRepresentatives()).thenReturn(representativesWithNull);

    service.notifyResourceRepresentatives(negotiationId);

    verify(negotiation)
        .setStateForResource("resource-1", NegotiationResourceState.REPRESENTATIVE_CONTACTED);

    ArgumentCaptor<NotificationCreateDTO> notificationCaptor =
        ArgumentCaptor.forClass(NotificationCreateDTO.class);
    verify(notificationService).createNotifications(notificationCaptor.capture());

    NotificationCreateDTO notification = notificationCaptor.getValue();
    assertEquals(1, notification.getUserIds().size());
    assertTrue(notification.getUserIds().contains(1L));
  }

  @Test
  void
      notifyResourceRepresentatives_WhenMultipleResourcesWithSameRepresentative_DoesNotDuplicateNotifications() {
    String negotiationId = "123";
    Person rep1 = createPerson(1L);

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));
    when(negotiation.getCurrentState()).thenReturn(NegotiationState.IN_PROGRESS);
    when(negotiation.getResources()).thenReturn(Set.of(resource1, resource2));

    when(resource1.getSourceId()).thenReturn("resource-1");
    when(resource2.getSourceId()).thenReturn("resource-2");
    when(negotiation.getCurrentStateForResource("resource-1")).thenReturn(null);
    when(negotiation.getCurrentStateForResource("resource-2")).thenReturn(null);

    // Both resources have the same representative
    when(resource1.getRepresentatives()).thenReturn(Set.of(rep1));
    when(resource2.getRepresentatives()).thenReturn(Set.of(rep1));

    service.notifyResourceRepresentatives(negotiationId);

    verify(negotiation)
        .setStateForResource("resource-1", NegotiationResourceState.REPRESENTATIVE_CONTACTED);
    verify(negotiation)
        .setStateForResource("resource-2", NegotiationResourceState.REPRESENTATIVE_CONTACTED);

    ArgumentCaptor<NotificationCreateDTO> notificationCaptor =
        ArgumentCaptor.forClass(NotificationCreateDTO.class);
    verify(notificationService).createNotifications(notificationCaptor.capture());

    NotificationCreateDTO notification = notificationCaptor.getValue();
    assertEquals(1, notification.getUserIds().size());
    assertTrue(notification.getUserIds().contains(1L));
  }

  @Test
  void notifyResourceRepresentatives_WhenNoResourcesNeedProcessing_DoesNotCreateNotifications() {
    String negotiationId = "123";

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));
    when(negotiation.getCurrentState()).thenReturn(NegotiationState.IN_PROGRESS);
    when(negotiation.getResources()).thenReturn(Set.of(resource1, resource2));

    when(resource1.getSourceId()).thenReturn("resource-1");
    when(resource2.getSourceId()).thenReturn("resource-2");

    // Both resources already have states
    when(negotiation.getCurrentStateForResource("resource-1"))
        .thenReturn(NegotiationResourceState.REPRESENTATIVE_CONTACTED);
    when(negotiation.getCurrentStateForResource("resource-2"))
        .thenReturn(NegotiationResourceState.REPRESENTATIVE_UNREACHABLE);

    service.notifyResourceRepresentatives(negotiationId);

    verify(negotiation, never()).setStateForResource(anyString(), any());
    verify(notificationService, never()).createNotifications(any());
  }

  private Person createPerson(Long id) {
    Person person = mock(Person.class);
    when(person.getId()).thenReturn(id);
    return person;
  }
}
