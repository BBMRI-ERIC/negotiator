package eu.bbmri_eric.negotiator.notification.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.notification.NotificationCreateDTO;
import eu.bbmri_eric.negotiator.notification.NotificationService;
import eu.bbmri_eric.negotiator.post.NewPostEvent;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
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

  @Mock private PersonRepository personRepository;

  @Mock private Negotiation negotiation;

  @Mock private Resource resource1;

  @Mock private Resource resource2;

  @Mock private Person negotiationAuthor;

  @Mock private Person representative1;

  @Mock private Person representative2;

  @Mock private Person postAuthor;

  private NewPostHandler handler;

  @BeforeEach
  void setUp() {
    handler =
        new NewPostHandler(
            notificationService, personService, negotiationRepository, personRepository);

    // Setup common mock behavior
    lenient().when(negotiationAuthor.getId()).thenReturn(100L);
    lenient().when(representative1.getId()).thenReturn(200L);
    lenient().when(representative2.getId()).thenReturn(300L);
    lenient().when(postAuthor.getId()).thenReturn(400L);

    lenient().when(resource1.getRepresentatives()).thenReturn(Set.of(representative1));
    lenient().when(resource2.getRepresentatives()).thenReturn(Set.of(representative2));
    lenient().when(negotiation.getResources()).thenReturn(Set.of(resource1, resource2));
    lenient().when(negotiation.getCreatedBy()).thenReturn(negotiationAuthor);
  }

  @Test
  void getSupportedEventType_ShouldReturnNewPostEvent() {
    // When
    Class<NewPostEvent> eventType = handler.getSupportedEventType();

    // Then
    assertEquals(NewPostEvent.class, eventType);
  }

  @Test
  void notify_WithPublicPost_ShouldNotifyAllParticipantsExceptAuthor() {
    // Given
    String negotiationId = "neg-123";
    Long postAuthorId = 400L;
    NewPostEvent event = new NewPostEvent(this, "post-1", negotiationId, postAuthorId, null);

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));

    // When
    handler.notify(event);

    // Then
    ArgumentCaptor<NotificationCreateDTO> captor =
        ArgumentCaptor.forClass(NotificationCreateDTO.class);
    verify(notificationService).createNotifications(captor.capture());

    NotificationCreateDTO dto = captor.getValue();
    assertNotNull(dto);
    assertEquals("New Post Notification", dto.getTitle());
    assertEquals("A new post has been created in negotiation " + negotiationId, dto.getBody());
    assertEquals(negotiationId, dto.getNegotiationId());

    // Should include negotiation author and representatives, but exclude post author
    List<Long> recipients = dto.getUserIds();
    assertEquals(3, recipients.size());
    assertTrue(recipients.contains(100L)); // negotiation author
    assertTrue(recipients.contains(200L)); // representative1
    assertTrue(recipients.contains(300L)); // representative2
    assertFalse(recipients.contains(400L)); // post author should be excluded
  }

  @Test
  void
      notify_WithPublicPost_WhenPostAuthorIsNegotiationAuthor_ShouldExcludeAuthorFromNotifications() {
    // Given
    String negotiationId = "neg-123";
    Long negotiationAuthorId = 100L; // Same as negotiation author
    NewPostEvent event = new NewPostEvent(this, "post-1", negotiationId, negotiationAuthorId, null);

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));

    // When
    handler.notify(event);

    // Then
    ArgumentCaptor<NotificationCreateDTO> captor =
        ArgumentCaptor.forClass(NotificationCreateDTO.class);
    verify(notificationService).createNotifications(captor.capture());

    NotificationCreateDTO dto = captor.getValue();
    List<Long> recipients = dto.getUserIds();

    // Should only include representatives, not the negotiation author who posted
    assertEquals(2, recipients.size());
    assertTrue(recipients.contains(200L)); // representative1
    assertTrue(recipients.contains(300L)); // representative2
    assertFalse(recipients.contains(100L)); // negotiation author who posted should be excluded
  }

  @Test
  void notify_WithPublicPost_WhenPostAuthorIsRepresentative_ShouldExcludeAuthorFromNotifications() {
    // Given
    String negotiationId = "neg-123";
    Long representativeId = 200L; // Same as representative1
    NewPostEvent event = new NewPostEvent(this, "post-1", negotiationId, representativeId, null);

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));

    // When
    handler.notify(event);

    // Then
    ArgumentCaptor<NotificationCreateDTO> captor =
        ArgumentCaptor.forClass(NotificationCreateDTO.class);
    verify(notificationService).createNotifications(captor.capture());

    NotificationCreateDTO dto = captor.getValue();
    List<Long> recipients = dto.getUserIds();

    // Should include negotiation author and other representative, but not the posting
    // representative
    assertEquals(2, recipients.size());
    assertTrue(recipients.contains(100L)); // negotiation author
    assertTrue(recipients.contains(300L)); // representative2
    assertFalse(recipients.contains(200L)); // representative1 who posted should be excluded
  }

  @Test
  void notify_WithPublicPost_WhenNoOtherParticipants_ShouldNotSendNotifications() {
    // Given
    String negotiationId = "neg-123";
    Long negotiationAuthorId = 100L;
    NewPostEvent event = new NewPostEvent(this, "post-1", negotiationId, negotiationAuthorId, null);

    // Setup negotiation with no representatives and author as post author
    when(negotiation.getResources()).thenReturn(Set.of());
    when(negotiation.getCreatedBy()).thenReturn(negotiationAuthor);
    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));

    // When
    handler.notify(event);

    // Then
    verify(notificationService, never()).createNotifications(any());
  }

  @Test
  void notify_WithPrivatePost_ShouldNotifyOrganizationMembersExceptAuthor() {
    // Given
    String negotiationId = "neg-123";
    Long postAuthorId = 400L;
    Long organizationId = 500L;
    NewPostEvent event =
        new NewPostEvent(this, "post-1", negotiationId, postAuthorId, organizationId);

    UserResponseModel user1 = mock(UserResponseModel.class);
    UserResponseModel user2 = mock(UserResponseModel.class);
    UserResponseModel user3 = mock(UserResponseModel.class); // This will be the post author

    when(user1.getId()).thenReturn("200");
    when(user2.getId()).thenReturn("300");
    when(user3.getId()).thenReturn("400"); // Same as post author

    when(personService.findAllByOrganizationId(organizationId))
        .thenReturn(List.of(user1, user2, user3));

    // When
    handler.notify(event);

    // Then
    ArgumentCaptor<NotificationCreateDTO> captor =
        ArgumentCaptor.forClass(NotificationCreateDTO.class);
    verify(notificationService).createNotifications(captor.capture());

    NotificationCreateDTO dto = captor.getValue();
    assertNotNull(dto);
    assertEquals("New Post Notification", dto.getTitle());
    assertEquals("A new post has been created in negotiation " + negotiationId, dto.getBody());
    assertEquals(negotiationId, dto.getNegotiationId());

    // Should include organization members but exclude post author
    List<Long> recipients = dto.getUserIds();
    assertEquals(2, recipients.size());
    assertTrue(recipients.contains(200L)); // user1
    assertTrue(recipients.contains(300L)); // user2
    assertFalse(recipients.contains(400L)); // post author should be excluded
  }

  @Test
  void notify_WithPrivatePost_WhenOnlyAuthorInOrganization_ShouldNotSendNotifications() {
    // Given
    String negotiationId = "neg-123";
    Long postAuthorId = 400L;
    Long organizationId = 500L;
    NewPostEvent event =
        new NewPostEvent(this, "post-1", negotiationId, postAuthorId, organizationId);

    UserResponseModel authorUser = mock(UserResponseModel.class);
    when(authorUser.getId()).thenReturn("400"); // Same as post author

    when(personService.findAllByOrganizationId(organizationId)).thenReturn(List.of(authorUser));

    // When
    handler.notify(event);

    // Then
    verify(notificationService, never()).createNotifications(any());
  }

  @Test
  void notify_WithPublicPost_WhenNegotiationNotFound_ShouldThrowException() {
    // Given
    String negotiationId = "non-existent";
    NewPostEvent event = new NewPostEvent(this, "post-1", negotiationId, 400L, null);

    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.empty());

    // When & Then
    assertThrows(EntityNotFoundException.class, () -> handler.notify(event));
    verify(notificationService, never()).createNotifications(any());
  }

  @Test
  void notify_WithPrivatePost_WhenEmptyOrganization_ShouldNotSendNotifications() {
    // Given
    String negotiationId = "neg-123";
    Long postAuthorId = 400L;
    Long organizationId = 500L;
    NewPostEvent event =
        new NewPostEvent(this, "post-1", negotiationId, postAuthorId, organizationId);

    when(personService.findAllByOrganizationId(organizationId)).thenReturn(List.of());

    // When
    handler.notify(event);

    // Then
    verify(notificationService, never()).createNotifications(any());
  }

  @Test
  void notify_WithPublicPost_WhenAuthorHasMultipleRoles_ShouldStillExcludeAuthor() {
    // Given - Author is both negotiation creator AND a representative
    String negotiationId = "neg-123";
    Long multiRoleUserId = 100L; // Same as negotiation author
    NewPostEvent event = new NewPostEvent(this, "post-1", negotiationId, multiRoleUserId, null);

    // Setup where negotiation author is also a representative
    when(representative1.getId()).thenReturn(100L); // Same as negotiation author
    when(resource1.getRepresentatives())
        .thenReturn(Set.of(representative1)); // Author is representative
    when(negotiation.getResources()).thenReturn(Set.of(resource1, resource2));
    when(negotiationRepository.findById(negotiationId)).thenReturn(Optional.of(negotiation));

    // When
    handler.notify(event);

    // Then
    ArgumentCaptor<NotificationCreateDTO> captor =
        ArgumentCaptor.forClass(NotificationCreateDTO.class);
    verify(notificationService).createNotifications(captor.capture());

    NotificationCreateDTO dto = captor.getValue();
    List<Long> recipients = dto.getUserIds();

    // Should only include representative2, not the multi-role author
    assertEquals(1, recipients.size());
    assertTrue(recipients.contains(300L)); // representative2
    assertFalse(recipients.contains(100L)); // multi-role author should be excluded
  }
}
