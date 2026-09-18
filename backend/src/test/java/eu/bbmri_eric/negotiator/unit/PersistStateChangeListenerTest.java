package eu.bbmri_eric.negotiator.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.PersistStateChangeListener;
import eu.bbmri_eric.negotiator.post.NewPostEvent;
import eu.bbmri_eric.negotiator.post.Post;
import eu.bbmri_eric.negotiator.post.PostRepository;
import eu.bbmri_eric.negotiator.post.PostType;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.Transition;
import org.springframework.statemachine.trigger.Trigger;

@ExtendWith(MockitoExtension.class)
class PersistStateChangeListenerTest {

  @Mock private NegotiationRepository negotiationRepository;
  @Mock private PersonRepository personRepository;
  @Mock private PostRepository postRepository;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private PersistStateChangeListener listener;

  @Mock private State<String, String> state;
  @Mock private Transition<String, String> transition;
  @Mock private StateMachine<String, String> stateMachine;
  @Mock private Trigger<String, String> trigger;

  private static final String NEGOTIATION_ID = "negotiation-123";
  private static final Long SENDER_ID = 42L;
  private static final String POST_BODY = "State updated";
  private static final String APPROVE = "APPROVE";

  @Test
  void onPersist_whenAllPostFieldsPresent_createsPost() {
    stubWhenNegotiationStateUpdated();

    Negotiation negotiation = Negotiation.builder().id(NEGOTIATION_ID).build();
    Person sender = new Person();

    when(negotiationRepository.findDetailedById(NEGOTIATION_ID))
        .thenReturn(Optional.of(negotiation));
    when(personRepository.findById(SENDER_ID)).thenReturn(Optional.of(sender));
    when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

    Message<String> message =
        MessageBuilder.withPayload(APPROVE)
            .setHeader("negotiationId", NEGOTIATION_ID)
            .setHeader("postBody", POST_BODY)
            .setHeader("postSenderId", SENDER_ID)
            .build();

    assertDoesNotThrow(() -> listener.onPersist(state, message, transition, stateMachine));

    ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
    verify(postRepository).save(postCaptor.capture());
    Post savedPost = postCaptor.getValue();
    assertEquals(POST_BODY, savedPost.getText());
    assertEquals(negotiation, savedPost.getNegotiation());
    assertEquals(PostType.PUBLIC, savedPost.getType());
    assertEquals(sender, savedPost.getCreatedBy());
  }

  @Test
  void onPersist_whenPostBodyIsNull_doesNotCreatePost() {
    stubWhenNegotiationStateUpdated();

    Negotiation negotiation = Negotiation.builder().build();
    when(negotiationRepository.findDetailedById(NEGOTIATION_ID))
        .thenReturn(Optional.of(negotiation));

    Message<String> message =
        MessageBuilder.withPayload(APPROVE)
            .setHeader("negotiationId", NEGOTIATION_ID)
            .setHeader("postSenderId", SENDER_ID)
            .build();

    assertDoesNotThrow(() -> listener.onPersist(state, message, transition, stateMachine));

    verify(postRepository, never()).save(any());
    verify(eventPublisher, never()).publishEvent(any(NewPostEvent.class));
  }

  @Test
  void onPersist_whenPostBodyIsEmpty_doesNotCreatePost() {
    stubWhenNegotiationStateUpdated();

    Negotiation negotiation = Negotiation.builder().build();
    when(negotiationRepository.findDetailedById(NEGOTIATION_ID))
        .thenReturn(Optional.of(negotiation));

    Message<String> message =
        MessageBuilder.withPayload(APPROVE)
            .setHeader("negotiationId", NEGOTIATION_ID)
            .setHeader("postBody", "")
            .setHeader("postSenderId", SENDER_ID)
            .build();

    assertDoesNotThrow(() -> listener.onPersist(state, message, transition, stateMachine));

    verify(postRepository, never()).save(any());
    verify(eventPublisher, never()).publishEvent(any(NewPostEvent.class));
  }

  @Test
  void onPersist_whenPostSenderIdIsNull_doesNotCreatePost() {
    stubWhenNegotiationStateUpdated();

    Negotiation negotiation = Negotiation.builder().build();
    when(negotiationRepository.findDetailedById(NEGOTIATION_ID))
        .thenReturn(Optional.of(negotiation));

    Message<String> message =
        MessageBuilder.withPayload(APPROVE)
            .setHeader("negotiationId", NEGOTIATION_ID)
            .setHeader("postBody", POST_BODY)
            .build();

    assertDoesNotThrow(() -> listener.onPersist(state, message, transition, stateMachine));

    verify(postRepository, never()).save(any());
    verify(eventPublisher, never()).publishEvent(any(NewPostEvent.class));
  }

  @Test
  void onPersist_whenNegotiationNotFound_throwsException() {
    when(negotiationRepository.findDetailedById(NEGOTIATION_ID)).thenReturn(Optional.empty());

    Message<String> message =
        MessageBuilder.withPayload(APPROVE)
            .setHeader("negotiationId", NEGOTIATION_ID)
            .setHeader("postBody", POST_BODY)
            .setHeader("postSenderId", SENDER_ID)
            .build();

    assertThrows(
        IllegalStateException.class,
        () -> listener.onPersist(state, message, transition, stateMachine));
  }

  private void stubWhenNegotiationStateUpdated() {
    when(state.getId()).thenReturn(NegotiationState.IN_PROGRESS.name());
    when(transition.getTrigger()).thenReturn(trigger);
    when(trigger.getEvent()).thenReturn(APPROVE);
    when(transition.getSource()).thenReturn(state);
  }
}
