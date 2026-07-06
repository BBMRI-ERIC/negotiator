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

@Component("lifecycleResourcePersistListener")
public class ResourcePersistListener implements TransitionListener<ResourceTransitionContext> {

  private final NegotiationRepository negotiationRepository;
  private final ApplicationEventPublisher eventPublisher;

  public ResourcePersistListener(
      NegotiationRepository negotiationRepository, ApplicationEventPublisher eventPublisher) {
    this.negotiationRepository = negotiationRepository;
    this.eventPublisher = eventPublisher;
  }

  @Override
  @Transactional
  public void onTransition(TransitionOutcome<ResourceTransitionContext> outcome) {
    ResourceTransitionContext context = outcome.context();
    Negotiation negotiation = negotiationRepository.findById(context.negotiationId()).orElse(null);
    if (negotiation == null) {
      return;
    }

    NegotiationResourceState fromState =
        negotiation.getCurrentStateForResource(context.resourceId());
    negotiation.setStateForResource(
        context.resourceId(), NegotiationResourceState.valueOf(outcome.toState()));
    NegotiationResourceState toState = negotiation.getCurrentStateForResource(context.resourceId());
    negotiationRepository.save(negotiation);

    eventPublisher.publishEvent(
        new ResourceStateChangeEvent(
            this,
            context.negotiationId(),
            context.resourceId(),
            fromState,
            toState,
            NegotiationResourceEvent.valueOf(outcome.event())));
  }
}
