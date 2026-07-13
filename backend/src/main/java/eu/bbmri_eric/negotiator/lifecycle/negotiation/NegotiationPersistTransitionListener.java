package eu.bbmri_eric.negotiator.lifecycle.negotiation;

import eu.bbmri_eric.negotiator.lifecycle.statemachine.TransitionListener;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.TransitionOutcome;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.NegotiationStateChangeEvent;
import eu.bbmri_eric.negotiator.post.Post;
import eu.bbmri_eric.negotiator.post.PostRepository;
import eu.bbmri_eric.negotiator.post.PostType;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Persists Negotiation state changes and publishes {@link NegotiationStateChangeEvent}. */
@Component
public class NegotiationPersistTransitionListener
    implements TransitionListener<NegotiationTransitionContext> {

  private final NegotiationRepository negotiationRepository;
  private final PersonRepository personRepository;
  private final PostRepository postRepository;
  private final ApplicationEventPublisher eventPublisher;

  public NegotiationPersistTransitionListener(
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
  public void onTransition(TransitionOutcome<NegotiationTransitionContext> outcome) {
    NegotiationTransitionContext context = outcome.context();
    Negotiation negotiation =
        negotiationRepository.findDetailedById(context.negotiationId()).orElseThrow();
    updateNegotiationStatus(outcome.targetState(), negotiation);
    if (Objects.nonNull(context.userId())
        && Objects.nonNull(context.postBody())
        && !context.postBody().isEmpty()) {
      createPost(context.userId(), negotiation, context.postBody());
    }
    publishChangeEvent(outcome);
  }

  private void updateNegotiationStatus(String targetState, Negotiation negotiation) {
    negotiation.setCurrentState(NegotiationState.valueOf(targetState));
    if (negotiation.getCurrentState().equals(NegotiationState.SUBMITTED)) {
      negotiation.setCreationDate(LocalDateTime.now());
    }
    negotiationRepository.saveAndFlush(negotiation);
  }

  private void createPost(Long postSenderId, Negotiation negotiation, String postBody) {
    Person postSender = personRepository.findById(postSenderId).orElse(null);
    Post postEntity =
        Post.builder().negotiation(negotiation).text(postBody).type(PostType.PUBLIC).build();
    postEntity.setCreatedBy(postSender);
    postEntity.setCreationDate(LocalDateTime.now());
    postRepository.save(postEntity);
  }

  private void publishChangeEvent(TransitionOutcome<NegotiationTransitionContext> outcome) {
    eventPublisher.publishEvent(
        new NegotiationStateChangeEvent(
            this,
            outcome.context().negotiationId(),
            NegotiationState.valueOf(outcome.sourceState()),
            NegotiationState.valueOf(outcome.targetState()),
            NegotiationEvent.valueOf(outcome.event())));
  }
}
