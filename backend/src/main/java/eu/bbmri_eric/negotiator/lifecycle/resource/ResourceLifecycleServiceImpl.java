package eu.bbmri_eric.negotiator.lifecycle.resource;

import eu.bbmri_eric.negotiator.common.AuthenticatedUserContext;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementRepository;
import eu.bbmri_eric.negotiator.info_submission.InformationSubmissionRepository;
import eu.bbmri_eric.negotiator.lifecycle.TransitionPreconditionException;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.BeanResolver;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.NegotiatorStateMachine;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.StateMachineDefinition;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.StateMachineFactory;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.TransitionDescriptor;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.TransitionListener;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.negotiation.NegotiationResourceState;
import eu.bbmri_eric.negotiator.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.user.PersonService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class ResourceLifecycleServiceImpl implements ResourceLifecycleService {

  private final NegotiationRepository negotiationRepository;
  private final InformationRequirementRepository requirementRepository;
  private final InformationSubmissionRepository requirementSubmissionRepository;
  private final PersonService personService;
  private final StateMachineDefinition definition;
  private final StateMachineFactory<ResourceTransitionContext> factory;

  public ResourceLifecycleServiceImpl(
      NegotiationRepository negotiationRepository,
      InformationRequirementRepository requirementRepository,
      InformationSubmissionRepository requirementSubmissionRepository,
      PersonService personService,
      BeanResolver resolver,
      List<TransitionListener<ResourceTransitionContext>> listeners) {
    this.negotiationRepository = negotiationRepository;
    this.requirementRepository = requirementRepository;
    this.requirementSubmissionRepository = requirementSubmissionRepository;
    this.personService = personService;
    this.definition = ResourceStateMachineDefinitions.definition();
    this.factory = new StateMachineFactory<>(definition, resolver, listeners);
  }

  @Override
  public Set<NegotiationResourceEvent> getPossibleEvents(String negotiationId, String resourceId) {
    NegotiationResourceState currentState;
    try {
      currentState = getCurrentStateForResource(negotiationId, resourceId);
    } catch (EntityNotFoundException e) {
      return Set.of();
    }
    if (Objects.isNull(currentState)) {
      throw new EntityNotFoundException(resourceId);
    }
    Negotiation negotiation =
        negotiationRepository
            .findById(negotiationId)
            .orElseThrow(() -> new EntityNotFoundException(negotiationId));
    if (!negotiation.getCurrentState().equals(NegotiationState.IN_PROGRESS)) {
      return Set.of();
    }
    String state = currentState.name();
    return definition.transitions().stream()
        .filter(transition -> transition.sourceState().equals(state))
        .filter(transition -> isSecurityRuleMet(transition, negotiationId, resourceId))
        .map(TransitionDescriptor::event)
        .map(NegotiationResourceEvent::valueOf)
        .collect(Collectors.toSet());
  }

  @Override
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
    NegotiationResourceState currentState = getCurrentStateForResource(negotiationId, resourceId);
    ResourceTransitionContext context =
        new ResourceTransitionContext(
            negotiationId, Set.copyOf(AuthenticatedUserContext.getRoles()), resourceId);
    NegotiatorStateMachine<ResourceTransitionContext> machine =
        factory.build(currentState.name(), context);
    machine.fire(negotiationResourceEvent.name());
    return getCurrentStateForResource(negotiationId, resourceId);
  }

  @Override
  public Map<String, Object> getStateMachineDiagram() {
    Map<String, Object> diagram = new HashMap<>();
    traverseState(NegotiationResourceState.SUBMITTED.name(), diagram);
    return diagram;
  }

  private void traverseState(String stateId, Map<String, Object> diagram) {
    Map<String, Object> transitionsMap = new HashMap<>();
    for (TransitionDescriptor transition : definition.transitions()) {
      if (transition.sourceState().equals(stateId)) {
        Map<String, Object> transitionMap = new HashMap<>();
        transitionMap.put("target", transition.targetState());
        transitionMap.put("event", transition.event());
        transitionsMap.put(transition.event(), transitionMap);
        traverseState(transition.targetState(), transitionMap);
      }
    }
    if (!transitionsMap.isEmpty()) {
      diagram.put(stateId, transitionsMap);
    }
  }

  private NegotiationResourceState getCurrentStateForResource(
      String negotiationId, String resourceId) {
    return negotiationRepository
        .findNegotiationResourceStateById(negotiationId, resourceId)
        .orElseThrow(() -> new EntityNotFoundException(negotiationId));
  }

  private boolean isSecurityRuleMet(
      TransitionDescriptor transition, String negotiationId, String resourceId) {
    Set<String> attributes = transition.securityAttributes();
    if (attributes.isEmpty()) {
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
    if (attributes.contains("isCreator")) {
      return negotiationRepository.existsByIdAndCreatedBy_Id(negotiationId, creatorId);
    } else if (attributes.contains("isRepresentative")) {
      return personService.isRepresentativeOfAnyResource(
          AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId(), List.of(resourceId));
    } else if (attributes.contains("isAdmin")) {
      return Objects.isNull(SecurityContextHolder.getContext().getAuthentication())
          || AuthenticatedUserContext.isCurrentlyAuthenticatedUserAdmin();
    }
    return true;
  }
}
