package eu.bbmri_eric.negotiator.lifecycle.negotiation;

import com.github.oxo42.stateless4j.StateMachine;
import eu.bbmri_eric.negotiator.common.AuthenticatedUserContext;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.StateMachineDefinition;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.StateMachineFactory;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.TransitionDescriptor;
import eu.bbmri_eric.negotiator.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.NegotiationState;
import jakarta.transaction.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/** stateless4j-backed implementation of the NegotiationLifecycleService. */
@Service
public class NegotiationLifecycleServiceImpl implements NegotiationLifecycleService {

  private final NegotiationRepository negotiationRepository;
  private final StateMachineDefinition definition;
  private final StateMachineFactory<NegotiationTransitionContext> factory;

  public NegotiationLifecycleServiceImpl(
      NegotiationRepository negotiationRepository,
      @Qualifier("negotiationStateMachineDefinition") StateMachineDefinition definition,
      @Qualifier("negotiationStateMachineFactory")
          StateMachineFactory<NegotiationTransitionContext> factory) {
    this.negotiationRepository = negotiationRepository;
    this.definition = definition;
    this.factory = factory;
  }

  @Override
  public Set<NegotiationEvent> getPossibleEvents(String negotiationId)
      throws EntityNotFoundException {
    return getPossibleEventsForCurrentStateMachine(negotiationId);
  }

  @Override
  @Transactional
  public NegotiationState sendEvent(String negotiationId, NegotiationEvent negotiationEvent) {
    return doSendEvent(negotiationId, negotiationEvent, null);
  }

  @Override
  @Transactional
  public NegotiationState sendEvent(
      String negotiationId, NegotiationEvent negotiationEvent, String message) {
    return doSendEvent(negotiationId, negotiationEvent, message);
  }

  private NegotiationState doSendEvent(
      String negotiationId, NegotiationEvent negotiationEvent, String message) {
    if (!getPossibleEvents(negotiationId).contains(negotiationEvent)) {
      throw new ForbiddenRequestException(
          "You are not allowed to %s the Negotiation"
              .formatted(negotiationEvent.getLabel().toLowerCase()));
    }
    NegotiationTransitionContext context =
        new NegotiationTransitionContext(
            negotiationId,
            new HashSet<>(AuthenticatedUserContext.getRoles()),
            message,
            AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId());
    StateMachine<String, String> machine =
        factory.build(getCurrentStateForNegotiation(negotiationId).name(), context);
    machine.fire(negotiationEvent.name());
    return getCurrentStateForNegotiation(negotiationId);
  }

  private NegotiationState getCurrentStateForNegotiation(String negotiationId) {
    return negotiationRepository
        .findNegotiationStateById(negotiationId)
        .orElseThrow(() -> new EntityNotFoundException(negotiationId));
  }

  private Set<NegotiationEvent> getPossibleEventsForCurrentStateMachine(String negotiationId) {
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
    String currentState = getCurrentStateForNegotiation(negotiationId).name();
    return definition.transitionsFrom(currentState).stream()
        .filter(transition -> isSecurityAttributeMet(transition, roles))
        .map(TransitionDescriptor::event)
        .map(NegotiationEvent::valueOf)
        .collect(Collectors.toSet());
  }

  private boolean isSecurityAttributeMet(TransitionDescriptor transition, List<String> roles) {
    return transition.securityAttributes().isEmpty()
        || transition.securityAttributes().stream().anyMatch(roles::contains);
  }
}
