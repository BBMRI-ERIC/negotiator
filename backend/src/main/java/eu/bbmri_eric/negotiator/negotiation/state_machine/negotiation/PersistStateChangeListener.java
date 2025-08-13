package eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation;

import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.post.Post;
import eu.bbmri_eric.negotiator.post.PostRepository;
import eu.bbmri_eric.negotiator.post.PostType;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.recipes.persist.PersistStateMachineHandler;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Service;

/** Listener for changes in the Negotiation lifecycle state machine. */
@Service("persistListener")
@CommonsLog
public class PersistStateChangeListener
    implements PersistStateMachineHandler.PersistStateChangeListener {

  NegotiationRepository negotiationRepository;
  PersonRepository personRepository;
  ApplicationEventPublisher eventPublisher;
  PostRepository postRepository;

  public PersistStateChangeListener(
      NegotiationRepository negotiationRepository,
      PersonRepository personRepository,
      PostRepository postRepository,
      ApplicationEventPublisher eventPublisher) {
    this.negotiationRepository = negotiationRepository;
    this.personRepository = personRepository;
    this.postRepository = postRepository;
    this.eventPublisher = eventPublisher;
  }

  @Override
  @Transactional
  public void onPersist(
      State<String, String> state,
      Message<String> message,
      Transition<String, String> transition,
      StateMachine<String, String> stateMachine) {
    String negotiationId = message.getHeaders().get("negotiationId", String.class);
    String postBody = message.getHeaders().get("postBody", String.class);
    Long postSenderId = message.getHeaders().get("postSenderId", Long.class);

    Negotiation negotiation = getNegotiation(negotiationId);
    updateNegotiationStatus(state, negotiation);

    if (Objects.nonNull(postSenderId) && Objects.nonNull(postBody) && !postBody.isEmpty()) {
      createPostFromMessage(postSenderId, negotiation, postBody);
    }
    publishChangeEvent(state, transition, negotiationId, postBody);
  }

  private void publishChangeEvent(
      State<String, String> state,
      Transition<String, String> transition,
      String negotiationId,
      String postBody) {
    NegotiationEvent event;
    try {
      event = NegotiationEvent.valueOf(transition.getTrigger().getEvent());
    } catch (IllegalArgumentException e) {
      log.error("Error publishing event about Negotiation status change", e);
      return;
    }

    eventPublisher.publishEvent(
        new NegotiationStateChangeEvent(
            this, negotiationId, NegotiationState.valueOf(state.getId()), event, postBody));
  }

  private void createPostFromMessage(Long postSenderId, Negotiation negotiation, String postBody) {
    Person postSender = personRepository.findById(postSenderId).orElse(null);
    Post postEntity =
        Post.builder().negotiation(negotiation).text(postBody).type(PostType.PUBLIC).build();
    postEntity.setCreatedBy(postSender);
    postEntity.setCreationDate(LocalDateTime.now());
    postRepository.save(postEntity);
  }

  private void updateNegotiationStatus(State<String, String> state, Negotiation negotiation) {
    if (Objects.nonNull(negotiation)) {
      negotiation.setCurrentState(NegotiationState.valueOf(state.getId()));
      if (negotiation.getCurrentState().equals(NegotiationState.SUBMITTED)) {
        negotiation.setCreationDate(LocalDateTime.now());
      }
      negotiationRepository.save(negotiation);
    }
  }

  @Nullable
  private Negotiation getNegotiation(String negotiationId) {
    Negotiation negotiation = null;
    if (Objects.nonNull(negotiationId)) {
      negotiation = negotiationRepository.findDetailedById(negotiationId).orElse(null);
    }
    return negotiation;
  }
}
