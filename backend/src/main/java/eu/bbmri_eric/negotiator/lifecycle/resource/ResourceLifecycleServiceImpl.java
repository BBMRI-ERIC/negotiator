package eu.bbmri_eric.negotiator.lifecycle.resource;

import eu.bbmri_eric.negotiator.common.AuthenticatedUserContext;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementRepository;
import eu.bbmri_eric.negotiator.info_submission.InformationSubmissionRepository;
import eu.bbmri_eric.negotiator.lifecycle.TransitionPreconditionException;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.StateMachineDefinition;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.TransitionDescriptor;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.TransitionExecutor;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.negotiation.NegotiationResourceState;
import eu.bbmri_eric.negotiator.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.user.PersonService;
import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** stateless4j-backed implementation of the ResourceLifecycleService. */
@Service
public class ResourceLifecycleServiceImpl implements ResourceLifecycleService {

  private final NegotiationRepository negotiationRepository;
  private final InformationRequirementRepository requirementRepository;
  private final InformationSubmissionRepository requirementSubmissionRepository;
  private final StateMachineDefinition definition;
  private final TransitionExecutor<ResourceTransitionContext> executor;
  private final PersonService personService;

  public ResourceLifecycleServiceImpl(
      NegotiationRepository negotiationRepository,
      InformationRequirementRepository requirementRepository,
      InformationSubmissionRepository requirementSubmissionRepository,
      @Qualifier("resourceStateMachineDefinition") StateMachineDefinition definition,
      @Qualifier("resourceTransitionExecutor")
          TransitionExecutor<ResourceTransitionContext> executor,
      PersonService personService) {
    this.negotiationRepository = negotiationRepository;
    this.requirementRepository = requirementRepository;
    this.requirementSubmissionRepository = requirementSubmissionRepository;
    this.definition = definition;
    this.executor = executor;
    this.personService = personService;
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
    return getPossibleEventsForCurrentStateMachine(negotiationId, resourceId, currentState);
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
    if (requirementRepository.existsByForEvent(negotiationResourceEvent)
        && !requirementSubmissionRepository.existsByResource_SourceIdAndNegotiation_Id(
            resourceId, negotiationId)) {
      throw new TransitionPreconditionException(
          "The requirement for this operation was not met. Please make sure you have submitted the required form and try again.");
    }
    if (!getPossibleEvents(negotiationId, resourceId).contains(negotiationResourceEvent)) {
      return getCurrentStateForResource(negotiationId, resourceId);
    }
    ResourceTransitionContext context =
        new ResourceTransitionContext(
            negotiationId, new HashSet<>(AuthenticatedUserContext.getRoles()), resourceId);
    String currentState = getCurrentStateForResource(negotiationId, resourceId).name();
    return NegotiationResourceState.valueOf(
        executor.fire(currentState, negotiationResourceEvent.name(), context).targetState());
  }

  private NegotiationResourceState getCurrentStateForResource(
      String negotiationId, String resourceId) throws EntityNotFoundException {
    return negotiationRepository
        .findNegotiationResourceStateById(negotiationId, resourceId)
        .orElseThrow(() -> new EntityNotFoundException(negotiationId));
  }

  private Set<NegotiationResourceEvent> getPossibleEventsForCurrentStateMachine(
      String negotiationId, String resourceId, NegotiationResourceState resourceState) {
    Negotiation negotiation =
        negotiationRepository
            .findById(negotiationId)
            .orElseThrow(() -> new EntityNotFoundException(negotiationId));
    if (!negotiation.getCurrentState().equals(NegotiationState.IN_PROGRESS)) {
      return Set.of();
    }
    return definition.transitionsFrom(resourceState.name()).stream()
        .filter(
            transition ->
                isSecurityAttributeMet(transition.securityAttributes(), negotiationId, resourceId))
        .map(TransitionDescriptor::event)
        .map(NegotiationResourceEvent::valueOf)
        .collect(Collectors.toSet());
  }

  private boolean isSecurityAttributeMet(
      Set<String> securityAttributes, String negotiationId, String resourceId) {
    if (securityAttributes.isEmpty()) {
      return true;
    }
    Long creatorId;
    try {
      creatorId = AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId();
    } catch (ClassCastException e) {
      return false;
    } catch (NullPointerException e) {
      creatorId = 0L;
    }
    if (securityAttributes.contains("isCreator")) {
      return negotiationRepository.existsByIdAndCreatedBy_Id(negotiationId, creatorId);
    } else if (securityAttributes.contains("isRepresentative")) {
      return personService.isRepresentativeOfAnyResource(
          AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId(), List.of(resourceId));
    } else if (securityAttributes.contains("isAdmin")) {
      return Objects.isNull(SecurityContextHolder.getContext().getAuthentication())
          || AuthenticatedUserContext.isCurrentlyAuthenticatedUserAdmin();
    }
    return true;
  }
}
