package eu.bbmri_eric.negotiator.lifecycle.negotiation;

import eu.bbmri_eric.negotiator.common.AuthenticatedUserContext;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.BeanResolver;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.NegotiatorStateMachine;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.StateMachineDefinition;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.StateMachineFactory;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.TransitionDescriptor;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.TransitionListener;
import eu.bbmri_eric.negotiator.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.NegotiationState;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class NegotiationLifecycleServiceImpl implements NegotiationLifecycleService {

  private final NegotiationRepository negotiationRepository;
  private final StateMachineDefinition definition;
  private final StateMachineFactory<NegotiationTransitionContext> factory;

  public NegotiationLifecycleServiceImpl(
      NegotiationRepository negotiationRepository,
      BeanResolver resolver,
      List<TransitionListener<NegotiationTransitionContext>> listeners) {
    this.negotiationRepository = negotiationRepository;
    this.definition = NegotiationStateMachineDefinitions.definition();
    this.factory = new StateMachineFactory<>(definition, resolver, listeners);
  }

  @Override
  public Set<NegotiationEvent> getPossibleEvents(String negotiationId) {
    Long userId;
    try {
      userId = AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId();
    } catch (ClassCastException e) {
      throw new ForbiddenRequestException("You are not allowed to perform this action");
    }
    List<String> roles = AuthenticatedUserContext.getRoles();
    if (!roles.contains("ROLE_ADMIN")
        && !negotiationRepository.existsByIdAndCreatedBy_Id(negotiationId, userId)) {
      return Set.of();
    }
    String currentState = getCurrentState(negotiationId).name();
    return definition.transitions().stream()
        .filter(transition -> transition.sourceState().equals(currentState))
        .filter(
            transition ->
                transition.securityAttributes().isEmpty()
                    || transition.securityAttributes().stream().anyMatch(roles::contains))
        .map(TransitionDescriptor::event)
        .map(NegotiationEvent::valueOf)
        .collect(Collectors.toSet());
  }

  @Override
  public NegotiationState sendEvent(String negotiationId, NegotiationEvent negotiationEvent) {
    return sendEvent(negotiationId, negotiationEvent, null);
  }

  @Override
  public NegotiationState sendEvent(
      String negotiationId, NegotiationEvent negotiationEvent, String message) {
    if (!getPossibleEvents(negotiationId).contains(negotiationEvent)) {
      throw new ForbiddenRequestException(
          "You are not allowed to %s the Negotiation"
              .formatted(negotiationEvent.getLabel().toLowerCase()));
    }
    NegotiationState currentState = getCurrentState(negotiationId);
    NegotiationTransitionContext context =
        new NegotiationTransitionContext(
            negotiationId,
            Set.copyOf(AuthenticatedUserContext.getRoles()),
            message,
            AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId());
    NegotiatorStateMachine<NegotiationTransitionContext> machine =
        factory.build(currentState.name(), context);
    machine.fire(negotiationEvent.name());
    return getCurrentState(negotiationId);
  }

  private NegotiationState getCurrentState(String negotiationId) {
    return negotiationRepository
        .findNegotiationStateById(negotiationId)
        .orElseThrow(() -> new EntityNotFoundException(negotiationId));
  }
}
