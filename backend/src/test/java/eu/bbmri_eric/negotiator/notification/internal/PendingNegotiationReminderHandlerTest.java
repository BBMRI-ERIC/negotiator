package eu.bbmri_eric.negotiator.notification.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.notification.NotificationCreateDTO;
import eu.bbmri_eric.negotiator.notification.NotificationService;
import eu.bbmri_eric.negotiator.user.Person;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PendingNegotiationReminderHandlerTest {

  @Mock private NotificationService notificationService;

  @Mock private NegotiationRepository negotiationRepository;

  @Mock private Negotiation negotiation1;

  @Mock private Negotiation negotiation2;

  @Mock private Resource resource1;

  @Mock private Resource resource2;

  private PendingNegotiationReminderHandler handler;

  @BeforeEach
  void setUp() {
    handler = new PendingNegotiationReminderHandler(notificationService, negotiationRepository);
  }

  @Test
  void getSupportedEventType_ReturnsCorrectEventType() {
    // When
    Class<PendingNegotiationReminderEvent> eventType = handler.getSupportedEventType();

    // Then
    assertEquals(PendingNegotiationReminderEvent.class, eventType);
  }

  @Test
  void notify_WhenPendingNegotiationsExist_SendsReminderNotifications() {
    // Given
    PendingNegotiationReminderEvent event = new PendingNegotiationReminderEvent(this);

    Person rep1 = createPerson(1L, "rep1@test.com");
    Person rep2 = createPerson(2L, "rep2@test.com");

    // Setup negotiation1 with contacted representatives
    when(negotiation1.getId()).thenReturn("123");
    when(negotiation1.getTitle()).thenReturn("Negotiation 1");
    when(negotiation1.getResources()).thenReturn(Set.of(resource1));
    when(resource1.getSourceId()).thenReturn("resource-1");
    when(negotiation1.getCurrentStateForResource("resource-1"))
        .thenReturn(NegotiationResourceState.REPRESENTATIVE_CONTACTED);
    when(resource1.getRepresentatives()).thenReturn(Set.of(rep1, rep2));

    // Setup negotiation2 with no contacted representatives - only stub what's needed
    when(negotiation2.getResources()).thenReturn(Set.of(resource2));
    when(resource2.getSourceId()).thenReturn("resource-2");
    when(negotiation2.getCurrentStateForResource("resource-2"))
        .thenReturn(NegotiationResourceState.SUBMITTED);

    when(negotiationRepository.findAllCreatedOn(any(LocalDateTime.class)))
        .thenReturn(Set.of(negotiation1, negotiation2));

    // When
    handler.notify(event);

    // Then
    ArgumentCaptor<NotificationCreateDTO> notificationCaptor =
        ArgumentCaptor.forClass(NotificationCreateDTO.class);
    verify(notificationService, times(1)).createNotifications(notificationCaptor.capture());

    NotificationCreateDTO notification = notificationCaptor.getValue();
    assertTrue(notification.getUserIds().containsAll(Arrays.asList(1L, 2L)));
    assertEquals("Pending Negotiation Reminder", notification.getTitle());
    assertTrue(notification.getBody().contains("pending negotiation request"));
    assertTrue(notification.getBody().contains("Negotiation 1"));
    assertEquals("123", notification.getNegotiationId());
  }

  @Test
  void notify_WhenNoPendingNegotiations_DoesNotCreateNotifications() {
    // Given
    PendingNegotiationReminderEvent event = new PendingNegotiationReminderEvent(this);

    when(negotiationRepository.findAllCreatedOn(any(LocalDateTime.class))).thenReturn(Set.of());

    // When
    handler.notify(event);

    // Then
    verify(notificationService, never()).createNotifications(any());
  }

  @Test
  void notify_WhenNoRepresentativesToNotify_DoesNotCreateNotifications() {
    // Given
    PendingNegotiationReminderEvent event = new PendingNegotiationReminderEvent(this);

    when(negotiation1.getResources()).thenReturn(Set.of(resource1));
    when(resource1.getSourceId()).thenReturn("resource-1");
    when(negotiation1.getCurrentStateForResource("resource-1"))
        .thenReturn(NegotiationResourceState.SUBMITTED); // Not REPRESENTATIVE_CONTACTED

    when(negotiationRepository.findAllCreatedOn(any(LocalDateTime.class)))
        .thenReturn(Set.of(negotiation1));

    // When
    handler.notify(event);

    // Then
    verify(notificationService, never()).createNotifications(any());
  }

  @Test
  void notify_CallsRepositoryWithCorrectDate() {
    // Given
    PendingNegotiationReminderEvent event = new PendingNegotiationReminderEvent(this);
    LocalDateTime fiveDaysAgo = LocalDateTime.now().minusDays(5);

    when(negotiationRepository.findAllCreatedOn(any(LocalDateTime.class))).thenReturn(Set.of());

    // When
    handler.notify(event);

    // Then
    ArgumentCaptor<LocalDateTime> dateCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
    verify(negotiationRepository).findAllCreatedOn(dateCaptor.capture());

    LocalDateTime capturedDate = dateCaptor.getValue();
    // Allow for small time differences in test execution
    assertTrue(Math.abs(capturedDate.getDayOfYear() - fiveDaysAgo.getDayOfYear()) <= 1);
  }

  @Test
  void notify_OnlyNotifiesRepresentativesFromContactedResources() {
    // Given
    PendingNegotiationReminderEvent event = new PendingNegotiationReminderEvent(this);

    Person contactedRep = createPerson(1L, "contacted@test.com");

    when(negotiation1.getId()).thenReturn("123");
    when(negotiation1.getTitle()).thenReturn("Test Negotiation");
    when(negotiation1.getResources()).thenReturn(Set.of(resource1, resource2));

    when(resource1.getSourceId()).thenReturn("resource-1");
    when(resource2.getSourceId()).thenReturn("resource-2");

    // Only resource1 is in REPRESENTATIVE_CONTACTED state
    when(negotiation1.getCurrentStateForResource("resource-1"))
        .thenReturn(NegotiationResourceState.REPRESENTATIVE_CONTACTED);
    when(negotiation1.getCurrentStateForResource("resource-2"))
        .thenReturn(NegotiationResourceState.SUBMITTED);

    when(resource1.getRepresentatives()).thenReturn(Set.of(contactedRep));

    when(negotiationRepository.findAllCreatedOn(any(LocalDateTime.class)))
        .thenReturn(Set.of(negotiation1));

    // When
    handler.notify(event);

    // Then
    ArgumentCaptor<NotificationCreateDTO> notificationCaptor =
        ArgumentCaptor.forClass(NotificationCreateDTO.class);
    verify(notificationService).createNotifications(notificationCaptor.capture());

    NotificationCreateDTO notification = notificationCaptor.getValue();
    assertEquals(List.of(1L), notification.getUserIds()); // Only contacted representative
  }

  @Test
  void notify_HandlesMultipleNegotiationsWithDifferentRepresentatives() {
    // Given
    PendingNegotiationReminderEvent event = new PendingNegotiationReminderEvent(this);

    Person rep1 = createPerson(1L, "rep1@test.com");
    Person rep2 = createPerson(2L, "rep2@test.com");
    Person rep3 = createPerson(3L, "rep3@test.com");

    // Create and fully configure resource3 mock first
    Resource resource3 = mock(Resource.class);
    when(resource3.getSourceId()).thenReturn("resource-3");
    when(resource3.getRepresentatives()).thenReturn(Set.of(rep3));

    // Setup first negotiation
    when(negotiation1.getId()).thenReturn("123");
    when(negotiation1.getTitle()).thenReturn("Negotiation 1");
    when(negotiation1.getResources()).thenReturn(Set.of(resource1));
    when(resource1.getSourceId()).thenReturn("resource-1");
    when(resource1.getRepresentatives()).thenReturn(Set.of(rep1, rep2));
    when(negotiation1.getCurrentStateForResource("resource-1"))
        .thenReturn(NegotiationResourceState.REPRESENTATIVE_CONTACTED);

    // Setup second negotiation with different resource
    when(negotiation2.getId()).thenReturn("456");
    when(negotiation2.getTitle()).thenReturn("Negotiation 2");
    when(negotiation2.getResources()).thenReturn(Set.of(resource3));
    when(negotiation2.getCurrentStateForResource("resource-3"))
        .thenReturn(NegotiationResourceState.REPRESENTATIVE_CONTACTED);

    when(negotiationRepository.findAllCreatedOn(any(LocalDateTime.class)))
        .thenReturn(Set.of(negotiation1, negotiation2));

    // When
    handler.notify(event);

    // Then
    ArgumentCaptor<NotificationCreateDTO> notificationCaptor =
        ArgumentCaptor.forClass(NotificationCreateDTO.class);
    verify(notificationService, times(2)).createNotifications(notificationCaptor.capture());

    List<NotificationCreateDTO> notifications = notificationCaptor.getAllValues();
    assertEquals(2, notifications.size());

    // Find notifications by negotiation ID since order is not guaranteed
    NotificationCreateDTO notification1 =
        notifications.stream()
            .filter(n -> "123".equals(n.getNegotiationId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Notification for 123 not found"));

    NotificationCreateDTO notification2 =
        notifications.stream()
            .filter(n -> "456".equals(n.getNegotiationId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Notification for 456 not found"));

    // Verify first notification (123)
    assertTrue(notification1.getUserIds().containsAll(List.of(1L, 2L)));
    assertEquals("123", notification1.getNegotiationId());

    // Verify second notification (456)
    assertEquals(List.of(3L), notification2.getUserIds());
    assertEquals("456", notification2.getNegotiationId());
  }

  private Person createPerson(Long id, String email) {
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
