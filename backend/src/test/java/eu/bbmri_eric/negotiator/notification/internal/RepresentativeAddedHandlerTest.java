package eu.bbmri_eric.negotiator.notification.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.governance.resource.ResourceRepository;
import eu.bbmri_eric.negotiator.notification.NotificationCreateDTO;
import eu.bbmri_eric.negotiator.notification.NotificationService;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import eu.bbmri_eric.negotiator.user.RepresentativeAddedEvent;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RepresentativeAddedHandlerTest {

  @Mock private NotificationService notificationService;
  @Mock private PersonRepository personRepository;
  @Mock private ResourceRepository resourceRepository;

  private RepresentativeAddedHandler handler;
  private Long resourceId = 1L;
  private Long representativeId = 1L;

  @BeforeEach
  void setUp() {
    handler =
        new RepresentativeAddedHandler(notificationService, personRepository, resourceRepository);
  }

  @Test
  void getSupportedEventType_ReturnsCorrectEventType() {
    Class<RepresentativeAddedEvent> eventType = handler.getSupportedEventType();
    assertEquals(RepresentativeAddedEvent.class, eventType);
  }

  @Test
  void notify_CallsNotificationService() {
    Person mockPerson = mock(Person.class);
    Resource mockResource = mock(Resource.class);

    when(personRepository.findById(representativeId)).thenReturn(Optional.of(mockPerson));
    when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(mockResource));
    when(mockPerson.getName()).thenReturn("John Doe");
    when(mockResource.getName()).thenReturn("Test Resource");
    when(mockResource.getSourceId()).thenReturn("resource-123");

    RepresentativeAddedEvent event =
        new RepresentativeAddedEvent(this, resourceId, representativeId);

    handler.notify(event);

    verify(notificationService).createNotifications(any(NotificationCreateDTO.class));
  }

  @Test
  void notify_RepresentativeNotFound_DoesNotCallNotificationService() {
    Resource mockResource = mock(Resource.class);

    when(personRepository.findById(representativeId)).thenReturn(Optional.empty());
    when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(mockResource));

    RepresentativeAddedEvent event =
        new RepresentativeAddedEvent(this, resourceId, representativeId);

    handler.notify(event);

    verify(notificationService, never()).createNotifications(any(NotificationCreateDTO.class));
  }

  @Test
  void notify_ResourceNotFound_DoesNotCallNotificationService() {
    Person mockPerson = mock(Person.class);

    when(personRepository.findById(representativeId)).thenReturn(Optional.of(mockPerson));
    when(resourceRepository.findById(resourceId)).thenReturn(Optional.empty());

    RepresentativeAddedEvent event =
        new RepresentativeAddedEvent(this, resourceId, representativeId);

    handler.notify(event);

    verify(notificationService, never()).createNotifications(any(NotificationCreateDTO.class));
  }

  @Test
  void notify_RepresentativeAndResourceNotFound_DoesNotCallNotificationService() {
    when(personRepository.findById(representativeId)).thenReturn(Optional.empty());
    when(resourceRepository.findById(resourceId)).thenReturn(Optional.empty());

    RepresentativeAddedEvent event =
        new RepresentativeAddedEvent(this, resourceId, representativeId);

    handler.notify(event);

    verify(notificationService, never()).createNotifications(any(NotificationCreateDTO.class));
  }
}
