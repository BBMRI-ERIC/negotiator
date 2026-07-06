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

@Component
public class NegotiationPersistListener implements TransitionListener<NegotiationTransitionContext> {

  private final NegotiationRepository negotiationRepository;
  private final PersonRepository personRepository;
  private final PostRepository postRepository;
  private final ApplicationEventPublisher eventPublisher;

  public NegotiationPersistListener(
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
        negotiationRepository.findDetailedById(context.negotiationId()).orElse(null);
    if (negotiation == null) {
      return;
    }

    NegotiationState toState = NegotiationState.valueOf(outcome.toState());
    negotiation.setCurrentState(toState);
    if (toState.equals(NegotiationState.SUBMITTED)) {
      negotiation.setCreationDate(LocalDateTime.now());
    }
    negotiationRepository.saveAndFlush(negotiation);

    if (Objects.nonNull(context.senderId())
        && Objects.nonNull(context.postBody())
        && !context.postBody().isEmpty()) {
      createPost(context.senderId(), negotiation, context.postBody());
    }

    eventPublisher.publishEvent(
        new NegotiationStateChangeEvent(
            this,
            context.negotiationId(),
            NegotiationState.valueOf(outcome.fromState()),
            toState,
            NegotiationEvent.valueOf(outcome.event())));
  }

  private void createPost(Long senderId, Negotiation negotiation, String postBody) {
    Person postSender = personRepository.findById(senderId).orElse(null);
    Post post = Post.builder().negotiation(negotiation).text(postBody).type(PostType.PUBLIC).build();
    post.setCreatedBy(postSender);
    post.setCreationDate(LocalDateTime.now());
    postRepository.save(post);
  }
}
