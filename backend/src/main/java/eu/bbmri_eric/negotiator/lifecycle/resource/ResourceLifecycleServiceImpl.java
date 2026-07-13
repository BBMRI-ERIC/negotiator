package eu.bbmri_eric.negotiator.lifecycle.resource;

import eu.bbmri_eric.negotiator.common.AuthenticatedUserContext;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.StateMachineDefinition;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.TransitionDescriptor;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.TransitionExecutor;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.negotiation.NegotiationResourceState;
import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/** stateless4j-backed implementation of the ResourceLifecycleService. */
@Service
public class ResourceLifecycleServiceImpl implements ResourceLifecycleService {

  private final NegotiationRepository negotiationRepository;
  private final StateMachineDefinition definition;
  private final TransitionExecutor<ResourceTransitionContext> executor;

  public ResourceLifecycleServiceImpl(
      NegotiationRepository negotiationRepository,
      @Qualifier("resourceStateMachineDefinition") StateMachineDefinition definition,
      @Qualifier("resourceTransitionExecutor")
          TransitionExecutor<ResourceTransitionContext> executor) {
    this.negotiationRepository = negotiationRepository;
    this.definition = definition;
    this.executor = executor;
  }

  @Override
  public Set<NegotiationResourceEvent> getPossibleEvents(String negotiationId, String resourceId)
      throws EntityNotFoundException {
    NegotiationResourceState currentState;
    try {
      currentState = getCurrentStateForResource(negotiationId, resourceId);
    } catch (EntityNotFoundException e) {
      return Set.of();
    }
    if (Objects.isNull(currentState)) {
      throw new EntityNotFoundException(resourceId);
    }
    ResourceTransitionContext context = buildContext(negotiationId, resourceId);
    return executor.permittedEvents(currentState.name(), context).stream()
        .map(NegotiationResourceEvent::valueOf)
        .collect(Collectors.toSet());
  }

  @Override
  public Map<String, Object> getStateMachineDiagram() {
    Map<String, Object> diagram = new HashMap<>();
    traverseState(definition.initialState(), diagram);
    return diagram;
  }

  private void traverseState(String stateId, Map<String, Object> diagram) {
    Map<String, Object> transitions = new HashMap<>();
    for (TransitionDescriptor transition : definition.transitionsFrom(stateId)) {
      Map<String, Object> transitionMap = new HashMap<>();
      transitionMap.put("target", transition.targetState());
      transitionMap.put("event", transition.event());
      transitions.put(transition.event(), transitionMap);
      traverseState(transition.targetState(), transitionMap);
    }
    if (!transitions.isEmpty()) {
      diagram.put(stateId, transitions);
    }
  }

  @Override
  @Transactional
  public NegotiationResourceState sendEvent(
      String negotiationId, String resourceId, NegotiationResourceEvent negotiationResourceEvent) {
    String currentState = getCurrentStateForResource(negotiationId, resourceId).name();
    ResourceTransitionContext context = buildContext(negotiationId, resourceId);
    return NegotiationResourceState.valueOf(
        executor.fire(currentState, negotiationResourceEvent.name(), context).targetState());
  }

  private ResourceTransitionContext buildContext(String negotiationId, String resourceId) {
    return new ResourceTransitionContext(
        negotiationId,
        new HashSet<>(AuthenticatedUserContext.getRolesOrEmpty()),
        resourceId,
        AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalIdOrNull());
  }

  private NegotiationResourceState getCurrentStateForResource(
      String negotiationId, String resourceId) throws EntityNotFoundException {
    return negotiationRepository
        .findNegotiationResourceStateById(negotiationId, resourceId)
        .orElseThrow(() -> new EntityNotFoundException(negotiationId));
  }
}
