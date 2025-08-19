package eu.bbmri_eric.negotiator.notification.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.governance.resource.ResourceRepository;
import eu.bbmri_eric.negotiator.notification.NotificationCreateDTO;
import eu.bbmri_eric.negotiator.notification.NotificationService;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import eu.bbmri_eric.negotiator.user.RepresentativeAddedEvent;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
public class RepresentativeAddedHandlerTest {

  @Mock private NotificationService notificationService;
  @Mock private PersonRepository personRepository;
  @Mock private ResourceRepository resourceRepository;

  private RepresentativeAddedHandler handler;
  private Long resourceId = 1L;
  private Long representativeId = 1L;
  private ListAppender<ILoggingEvent> listAppender;

  @BeforeEach
  void setUp() {
    handler =
        new RepresentativeAddedHandler(notificationService, personRepository, resourceRepository);

    Logger logger = (Logger) LoggerFactory.getLogger(RepresentativeAddedHandler.class);
    listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);
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

    List<ILoggingEvent> logsList = listAppender.list;
    assertEquals(1, logsList.size());
    assertEquals(Level.INFO, logsList.get(0).getLevel());
    assertEquals(
        "Notified John Doe about being assigned as a representative for resource: resource-123",
        logsList.get(0).getFormattedMessage());
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

    List<ILoggingEvent> logsList = listAppender.list;
    assertEquals(1, logsList.size());
    assertEquals(Level.WARN, logsList.get(0).getLevel());
    assertEquals(
        "Failed to notify representative since representative not found.",
        logsList.get(0).getFormattedMessage());
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

    List<ILoggingEvent> logsList = listAppender.list;
    assertEquals(1, logsList.size());
    assertEquals(Level.WARN, logsList.get(0).getLevel());
    assertEquals(
        "Failed to notify representative since resource not found.",
        logsList.get(0).getFormattedMessage());
  }

  @Test
  void notify_RepresentativeAndResourceNotFound_DoesNotCallNotificationService() {
    when(personRepository.findById(representativeId)).thenReturn(Optional.empty());
    when(resourceRepository.findById(resourceId)).thenReturn(Optional.empty());

    RepresentativeAddedEvent event =
        new RepresentativeAddedEvent(this, resourceId, representativeId);

    handler.notify(event);

    verify(notificationService, never()).createNotifications(any(NotificationCreateDTO.class));

    List<ILoggingEvent> logsList = listAppender.list;
    assertEquals(1, logsList.size());
    assertEquals(Level.WARN, logsList.get(0).getLevel());
    assertEquals(
        "Failed to notify representative since representative and resource not found.",
        logsList.get(0).getFormattedMessage());
  }
}
