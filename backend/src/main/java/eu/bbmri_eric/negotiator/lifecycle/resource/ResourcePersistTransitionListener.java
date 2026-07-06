package eu.bbmri_eric.negotiator.lifecycle.resource;

import eu.bbmri_eric.negotiator.lifecycle.statemachine.TransitionListener;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.TransitionOutcome;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.negotiation.NegotiationResourceState;
import eu.bbmri_eric.negotiator.negotiation.ResourceStateChangeEvent;
import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Persists Resource state changes and publishes {@link ResourceStateChangeEvent}. */
@Component
public class ResourcePersistTransitionListener
    implements TransitionListener<ResourceTransitionContext> {

  private final NegotiationRepository negotiationRepository;
  private final ApplicationEventPublisher eventPublisher;

  public ResourcePersistTransitionListener(
      NegotiationRepository negotiationRepository, ApplicationEventPublisher eventPublisher) {
    this.negotiationRepository = negotiationRepository;
    this.eventPublisher = eventPublisher;
  }

  @Override
  @Transactional
  public void onTransition(TransitionOutcome<ResourceTransitionContext> outcome) {
    ResourceTransitionContext context = outcome.context();
    Negotiation negotiation = negotiationRepository.findById(context.negotiationId()).orElseThrow();
    NegotiationResourceState fromState =
        negotiation.getCurrentStateForResource(context.resourceId());
    negotiation.setStateForResource(
        context.resourceId(), NegotiationResourceState.valueOf(outcome.targetState()));
    NegotiationResourceState toState = negotiation.getCurrentStateForResource(context.resourceId());
    eventPublisher.publishEvent(
        new ResourceStateChangeEvent(
            this,
            negotiation.getId(),
            context.resourceId(),
            fromState,
            toState,
            NegotiationResourceEvent.valueOf(outcome.event())));
    negotiationRepository.save(negotiation);
  }
}
