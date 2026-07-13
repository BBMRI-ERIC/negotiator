package eu.bbmri_eric.negotiator.lifecycle.negotiation;

import eu.bbmri_eric.negotiator.common.AuthenticatedUserContext;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.TransitionExecutor;
import eu.bbmri_eric.negotiator.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.NegotiationState;
import jakarta.transaction.Transactional;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/** stateless4j-backed implementation of the NegotiationLifecycleService. */
@Service
public class NegotiationLifecycleServiceImpl implements NegotiationLifecycleService {

  private final NegotiationRepository negotiationRepository;
  private final TransitionExecutor<NegotiationTransitionContext> executor;

  public NegotiationLifecycleServiceImpl(
      NegotiationRepository negotiationRepository,
      @Qualifier("negotiationTransitionExecutor")
          TransitionExecutor<NegotiationTransitionContext> executor) {
    this.negotiationRepository = negotiationRepository;
    this.executor = executor;
  }

  @Override
  public Set<NegotiationEvent> getPossibleEvents(String negotiationId)
      throws EntityNotFoundException {
    String currentState = getCurrentStateForNegotiation(negotiationId).name();
    NegotiationTransitionContext context = buildContext(negotiationId, null);
    return executor.permittedEvents(currentState, context).stream()
        .map(NegotiationEvent::valueOf)
        .collect(Collectors.toSet());
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
    String currentState = getCurrentStateForNegotiation(negotiationId).name();
    NegotiationTransitionContext context = buildContext(negotiationId, message);
    return NegotiationState.valueOf(
        executor.fire(currentState, negotiationEvent.name(), context).targetState());
  }

  private NegotiationTransitionContext buildContext(String negotiationId, String message) {
    return new NegotiationTransitionContext(
        negotiationId,
        new HashSet<>(AuthenticatedUserContext.getRolesOrEmpty()),
        message,
        AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalIdOrNull());
  }

  private NegotiationState getCurrentStateForNegotiation(String negotiationId) {
    return negotiationRepository
        .findNegotiationStateById(negotiationId)
        .orElseThrow(() -> new EntityNotFoundException(negotiationId));
  }
}
