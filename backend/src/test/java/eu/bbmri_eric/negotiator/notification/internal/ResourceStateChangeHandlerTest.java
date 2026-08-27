package eu.bbmri_eric.negotiator.notification.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.EnumBackedLifecycleCatalog;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.ResourceStateChangeEvent;
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
class ResourceStateChangeHandlerTest {

  @Mock private NotificationService notificationService;

  @Mock private NegotiationRepository negotiationRepository;

  @Mock private Negotiation negotiation;

  @Mock private Person requester;

  private ResourceStateChangeHandler handler;

  @BeforeEach
  void setUp() {
    handler =
        new ResourceStateChangeHandler(
            negotiationRepository, notificationService, new EnumBackedLifecycleCatalog());
    lenient().when(requester.getId()).thenReturn(42L);
    lenient().when(negotiation.getCreatedBy()).thenReturn(requester);
    lenient().when(negotiation.getTitle()).thenReturn("Test Negotiation");
  }

  @Test
  void getSupportedEventType_ReturnsCorrectEventType() {
    assertEquals(ResourceStateChangeEvent.class, handler.getSupportedEventType());
  }

  @Test
  void notify_NamesBothStatesByTheirHumanLabels() {
    when(negotiationRepository.findById("NEG-123")).thenReturn(Optional.of(negotiation));

    handler.notify(
        new ResourceStateChangeEvent(
            this, "NEG-123", "resource-1", "SUBMITTED", "REPRESENTATIVE_CONTACTED", "CONTACT"));

    NotificationCreateDTO notification = capturedNotification();
    assertEquals(List.of(42L), notification.getUserIds());
    assertEquals("Request Status update", notification.getTitle());
    assertEquals(
        "Resource resource-1 had a change of status in your request Test Negotiation, from Submitted to Representative Contacted",
        notification.getBody());
    assertEquals("NEG-123", notification.getNegotiationId());
  }

  @Test
  void notify_NamesLabelsThatDifferFromTheStateNames() {
    when(negotiationRepository.findById("NEG-456")).thenReturn(Optional.of(negotiation));

    handler.notify(
        new ResourceStateChangeEvent(
            this,
            "NEG-456",
            "resource-2",
            "RESOURCE_UNAVAILABLE_WILLING_TO_COLLECT",
            "RESOURCE_MADE_AVAILABLE",
            "GRANT_ACCESS_TO_RESOURCE"));

    assertEquals(
        "Resource resource-2 had a change of status in your request Test Negotiation, from Resource Unavailable, Willing to Collect to Resource Made Available",
        capturedNotification().getBody());
  }

  @Test
  void notify_WhenNegotiationNotFound_ThrowsEntityNotFoundException() {
    when(negotiationRepository.findById("NEG-789")).thenReturn(Optional.empty());

    ResourceStateChangeEvent event =
        new ResourceStateChangeEvent(
            this, "NEG-789", "resource-3", "SUBMITTED", "REPRESENTATIVE_CONTACTED", "CONTACT");

    assertThrows(EntityNotFoundException.class, () -> handler.notify(event));
    verify(notificationService, never()).createNotifications(any());
  }

  private NotificationCreateDTO capturedNotification() {
    ArgumentCaptor<NotificationCreateDTO> captor =
        ArgumentCaptor.forClass(NotificationCreateDTO.class);
    verify(notificationService).createNotifications(captor.capture());
    return captor.getValue();
  }
}
