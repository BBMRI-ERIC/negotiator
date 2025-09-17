package eu.bbmri_eric.negotiator.notification.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.notification.NotificationCreateDTO;
import eu.bbmri_eric.negotiator.notification.NotificationService;
import eu.bbmri_eric.negotiator.post.NewPostEvent;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonService;
import eu.bbmri_eric.negotiator.user.UserResponseModel;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NewPostHandlerTest {

  @Mock private NotificationService notificationService;
  @Mock private PersonService personService;
  @Mock private NegotiationRepository negotiationRepository;
  @Mock private Negotiation negotiation;
  @Mock private Resource resource1;
  @Mock private Resource resource2;
  @Mock private Person negotiationAuthor;
  @Mock private Person representative1;
  @Mock private Person representative2;

  private NewPostHandler handler;

  @BeforeEach
  void setUp() {
    handler = new NewPostHandler(notificationService, personService, negotiationRepository);

    lenient().when(negotiationAuthor.getId()).thenReturn(100L);
    lenient().when(representative1.getId()).thenReturn(200L);
    lenient().when(representative2.getId()).thenReturn(300L);
    lenient().when(resource1.getRepresentatives()).thenReturn(Set.of(representative1));
    lenient().when(resource2.getRepresentatives()).thenReturn(Set.of(representative2));
    lenient().when(negotiation.getResources()).thenReturn(Set.of(resource1, resource2));
    lenient().when(negotiation.getCreatedBy()).thenReturn(negotiationAuthor);
    lenient().when(negotiation.getTitle()).thenReturn("Important Negotiation");
  }

  @Test
  void getSupportedEventType_ShouldReturnNewPostEvent() {
    Class<NewPostEvent> eventType = handler.getSupportedEventType();
    assertEquals(NewPostEvent.class, eventType);
  }

  @Test
  void notify_WithPublicPost_ShouldNotifyAllParticipantsExceptAuthor() {
    String negotiationId = "neg-123";
    Long postAuthorId = 400L;
    NewPostEvent event = new NewPostEvent(this, "post-1", negotiationId, postAuthorId, null);

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));

    handler.notify(event);

    ArgumentCaptor<NotificationCreateDTO> captor =
        ArgumentCaptor.forClass(NotificationCreateDTO.class);
    verify(notificationService).createNotifications(captor.capture());

    NotificationCreateDTO dto = captor.getValue();
    assertNotNull(dto);
    assertEquals("New Post Notification", dto.getTitle());
    assertEquals(
        "A new message was posted in negotiation " + "Important Negotiation", dto.getBody());
    assertEquals(negotiationId, dto.getNegotiationId());

    List<Long> recipients = dto.getUserIds();
    assertEquals(3, recipients.size());
    assertTrue(recipients.contains(100L));
    assertTrue(recipients.contains(200L));
    assertTrue(recipients.contains(300L));
    assertFalse(recipients.contains(400L));
  }

  @Test
  void
      notify_WithPublicPost_WhenPostAuthorIsNegotiationAuthor_ShouldExcludeAuthorFromNotifications() {
    String negotiationId = "neg-123";
    Long negotiationAuthorId = 100L;
    NewPostEvent event = new NewPostEvent(this, "post-1", negotiationId, negotiationAuthorId, null);

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));

    handler.notify(event);

    ArgumentCaptor<NotificationCreateDTO> captor =
        ArgumentCaptor.forClass(NotificationCreateDTO.class);
    verify(notificationService).createNotifications(captor.capture());

    NotificationCreateDTO dto = captor.getValue();
    List<Long> recipients = dto.getUserIds();

    assertEquals(2, recipients.size());
    assertTrue(recipients.contains(200L));
    assertTrue(recipients.contains(300L));
    assertFalse(recipients.contains(100L));
  }

  @Test
  void notify_WithPublicPost_WhenPostAuthorIsRepresentative_ShouldExcludeAuthorFromNotifications() {
    String negotiationId = "neg-123";
    Long representativeId = 200L;
    NewPostEvent event = new NewPostEvent(this, "post-1", negotiationId, representativeId, null);

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));

    handler.notify(event);

    ArgumentCaptor<NotificationCreateDTO> captor =
        ArgumentCaptor.forClass(NotificationCreateDTO.class);
    verify(notificationService).createNotifications(captor.capture());

    NotificationCreateDTO dto = captor.getValue();
    List<Long> recipients = dto.getUserIds();

    assertEquals(2, recipients.size());
    assertTrue(recipients.contains(100L));
    assertTrue(recipients.contains(300L));
    assertFalse(recipients.contains(200L));
  }

  @Test
  void notify_WithPublicPost_WhenOnlyAuthorExists_ShouldNotSendNotifications() {
    String negotiationId = "neg-123";
    Long negotiationAuthorId = 100L;
    NewPostEvent event = new NewPostEvent(this, "post-1", negotiationId, negotiationAuthorId, null);

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));
    when(negotiation.getResources()).thenReturn(Set.of());

    handler.notify(event);

    verify(notificationService, never()).createNotifications(any());
  }

  @Test
  void notify_WithPrivatePost_ShouldNotifyOrganizationMembersExceptAuthor() {
    String negotiationId = "neg-123";
    Long postAuthorId = 400L;
    Long organizationId = 500L;
    NewPostEvent event =
        new NewPostEvent(this, "post-1", negotiationId, postAuthorId, organizationId);

    UserResponseModel user1 = mock(UserResponseModel.class);
    UserResponseModel user2 = mock(UserResponseModel.class);
    UserResponseModel user3 = mock(UserResponseModel.class);

    when(user1.getId()).thenReturn("200");
    when(user2.getId()).thenReturn("300");
    when(user3.getId()).thenReturn("400");

    when(personService.findAllByOrganizationId(organizationId))
        .thenReturn(List.of(user1, user2, user3));
    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));

    handler.notify(event);

    ArgumentCaptor<NotificationCreateDTO> captor =
        ArgumentCaptor.forClass(NotificationCreateDTO.class);
    verify(notificationService).createNotifications(captor.capture());

    NotificationCreateDTO dto = captor.getValue();
    List<Long> recipients = dto.getUserIds();

    assertEquals(3, recipients.size());
    assertTrue(recipients.contains(100L));
    assertTrue(recipients.contains(200L));
    assertTrue(recipients.contains(300L));
    assertFalse(recipients.contains(400L));
  }

  @Test
  void notify_WithPrivatePost_WhenNegotiationNotFound_ShouldThrowException() {
    String negotiationId = "neg-123";
    Long postAuthorId = 400L;
    Long organizationId = 500L;
    NewPostEvent event =
        new NewPostEvent(this, "post-1", negotiationId, postAuthorId, organizationId);

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.empty());

    assertThrows(RuntimeException.class, () -> handler.notify(event));
    verify(notificationService, never()).createNotifications(any());
  }
}
