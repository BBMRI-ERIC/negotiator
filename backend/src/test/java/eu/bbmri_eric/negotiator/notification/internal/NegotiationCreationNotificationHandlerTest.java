package eu.bbmri_eric.negotiator.notification.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.NewNegotiationEvent;
import eu.bbmri_eric.negotiator.notification.NotificationCreateDTO;
import eu.bbmri_eric.negotiator.notification.NotificationService;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NewNegotiationHandlerTest {

  @Mock private NotificationService notificationService;

  @Mock private NegotiationRepository negotiationRepository;

  @Mock private PersonRepository personRepository;

  @Mock private Negotiation negotiation;

  @Mock private Person researcher;

  private NewNegotiationHandler handler;

  @BeforeEach
  void setUp() {
    handler =
        new NewNegotiationHandler(notificationService, negotiationRepository, personRepository);

    // Setup common mock behavior using lenient stubs
    lenient().when(researcher.getId()).thenReturn(100L);
    lenient().when(negotiation.getCreatedBy()).thenReturn(researcher);
  }

  @Test
  void getSupportedEventType_ReturnsCorrectEventType() {
    // When
    Class<NewNegotiationEvent> eventType = handler.getSupportedEventType();

    // Then
    assertEquals(NewNegotiationEvent.class, eventType);
  }

  @Test
  void notify_WhenNewNegotiation_SendsBothResearcherAndAdminNotifications() {
    // Given
    String negotiationId = "NEG-123";
    NewNegotiationEvent event = new NewNegotiationEvent(this, negotiationId);

    Person admin1 = createPerson(1L, "admin1@test.com");
    Person admin2 = createPerson(2L, "admin2@test.com");
    List<Person> admins = Arrays.asList(admin1, admin2);

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));
    when(personRepository.findAllByAdminIsTrue()).thenReturn(admins);

    // When
    handler.notify(event);

    // Then
    ArgumentCaptor<NotificationCreateDTO> notificationCaptor =
        ArgumentCaptor.forClass(NotificationCreateDTO.class);
    verify(notificationService, times(2)).createNotifications(notificationCaptor.capture());

    List<NotificationCreateDTO> notifications = notificationCaptor.getAllValues();

    // First notification should be for researcher
    NotificationCreateDTO researcherNotification = notifications.get(0);
    assertEquals(List.of(100L), researcherNotification.getUserIds());
    assertEquals(NewNegotiationHandler.TITLE, researcherNotification.getTitle());
    assertEquals(NewNegotiationHandler.BODY, researcherNotification.getBody());
    assertEquals(negotiationId, researcherNotification.getNegotiationId());

    // Second notification should be for admins
    NotificationCreateDTO adminNotification = notifications.get(1);
    assertTrue(adminNotification.getUserIds().containsAll(Arrays.asList(1L, 2L)));
    assertEquals("New Request", adminNotification.getTitle());
    assertEquals("A new Request has been submitted for review", adminNotification.getBody());
    assertEquals(negotiationId, adminNotification.getNegotiationId());
  }

  @Test
  void notify_WhenNegotiationNotFound_OnlyNotifiesAdmins() {
    // Given
    String negotiationId = "NEG-123";
    NewNegotiationEvent event = new NewNegotiationEvent(this, negotiationId);

    Person admin = createPerson(1L, "admin@test.com");
    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.empty());
    when(personRepository.findAllByAdminIsTrue()).thenReturn(List.of(admin));

    // When
    handler.notify(event);

    // Then
    ArgumentCaptor<NotificationCreateDTO> notificationCaptor =
        ArgumentCaptor.forClass(NotificationCreateDTO.class);
    verify(notificationService, times(1)).createNotifications(notificationCaptor.capture());

    NotificationCreateDTO notification = notificationCaptor.getValue();
    assertEquals(List.of(1L), notification.getUserIds());
    assertEquals("New Request", notification.getTitle());
  }

  @Test
  void notify_WhenNoAdmins_OnlyNotifiesResearcher() {
    // Given
    String negotiationId = "NEG-123";
    NewNegotiationEvent event = new NewNegotiationEvent(this, negotiationId);

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));
    when(personRepository.findAllByAdminIsTrue()).thenReturn(Collections.emptyList());

    // When
    handler.notify(event);

    // Then
    ArgumentCaptor<NotificationCreateDTO> notificationCaptor =
        ArgumentCaptor.forClass(NotificationCreateDTO.class);
    verify(notificationService, times(1)).createNotifications(notificationCaptor.capture());

    NotificationCreateDTO notification = notificationCaptor.getValue();
    assertEquals(List.of(100L), notification.getUserIds());
    assertEquals("Negotiation Created Successfully", notification.getTitle());
  }

  @Test
  void notify_WhenNeitherNegotiationNorAdminsExist_DoesNotCreateNotifications() {
    // Given
    String negotiationId = "NEG-123";
    NewNegotiationEvent event = new NewNegotiationEvent(this, negotiationId);

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.empty());
    when(personRepository.findAllByAdminIsTrue()).thenReturn(Collections.emptyList());

    // When
    handler.notify(event);

    // Then
    verify(notificationService, never()).createNotifications(any());
  }

  @Test
  void createResearcherConfirmationNotification_CreatesCorrectNotification() {
    // Given
    String negotiationId = "NEG-123";
    NewNegotiationEvent event = new NewNegotiationEvent(this, negotiationId);

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));
    when(personRepository.findAllByAdminIsTrue()).thenReturn(Collections.emptyList());

    // When
    handler.notify(event);

    // Then
    ArgumentCaptor<NotificationCreateDTO> notificationCaptor =
        ArgumentCaptor.forClass(NotificationCreateDTO.class);
    verify(notificationService).createNotifications(notificationCaptor.capture());

    NotificationCreateDTO notification = notificationCaptor.getValue();
    assertEquals(List.of(100L), notification.getUserIds());
    assertEquals("Negotiation Created Successfully", notification.getTitle());
    assertTrue(notification.getBody().contains("created successfully"));
    assertTrue(notification.getBody().contains("review by our administrators"));
    assertEquals(negotiationId, notification.getNegotiationId());
  }

  @Test
  void notifyAdminsAboutNewNegotiation_CreatesCorrectNotification() {
    // Given
    String negotiationId = "NEG-123";
    NewNegotiationEvent event = new NewNegotiationEvent(this, negotiationId);

    Person admin1 = createPerson(1L, "admin1@test.com");
    Person admin2 = createPerson(2L, "admin2@test.com");
    List<Person> admins = Arrays.asList(admin1, admin2);

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.empty());
    when(personRepository.findAllByAdminIsTrue()).thenReturn(admins);

    // When
    handler.notify(event);

    // Then
    ArgumentCaptor<NotificationCreateDTO> notificationCaptor =
        ArgumentCaptor.forClass(NotificationCreateDTO.class);
    verify(notificationService).createNotifications(notificationCaptor.capture());

    NotificationCreateDTO notification = notificationCaptor.getValue();
    assertTrue(notification.getUserIds().containsAll(Arrays.asList(1L, 2L)));
    assertEquals("New Request", notification.getTitle());
    assertEquals("A new Request has been submitted for review", notification.getBody());
    assertEquals(negotiationId, notification.getNegotiationId());
  }

  private Person createPerson(Long id, String email) {
    Person person = new Person();
    person.setId(id);
    person.setEmail(email);
    return person;
  }
}
