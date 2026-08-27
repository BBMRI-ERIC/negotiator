package eu.bbmri_eric.negotiator.negotiation.state_machine.resource;

import eu.bbmri_eric.negotiator.common.AuthenticatedUserContext;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.common.exceptions.LifecycleConflictException;
import eu.bbmri_eric.negotiator.common.exceptions.LifecycleProblemType;
import eu.bbmri_eric.negotiator.common.exceptions.WrongRequestException;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementRepository;
import eu.bbmri_eric.negotiator.info_submission.InformationSubmissionRepository;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.user.PersonService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.recipes.persist.PersistStateMachineHandler;
import org.springframework.statemachine.security.SecurityRule;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Service;

/** Spring State Machine implementation of the ResourceLifecycleService. */
@Service
@CommonsLog
public class ResourceLifecycleServiceImpl implements ResourceLifecycleService {

  private final NegotiationRepository negotiationRepository;
  private final InformationRequirementRepository requirementRepository;
  private final InformationSubmissionRepository requirementSubmissionRepository;

  private final PersistStateMachineHandler persistStateMachineHandler;

  private final StateMachine<String, String> stateMachine;

  PersonService personService;

  public ResourceLifecycleServiceImpl(
      NegotiationRepository negotiationRepository,
      InformationRequirementRepository requirementRepository,
      InformationSubmissionRepository requirementSubmissionRepository,
      @Qualifier("resourcePersistHandler") PersistStateMachineHandler persistStateMachineHandler,
      @Qualifier("resourceStateMachine") StateMachine<String, String> stateMachine,
      PersonService personService) {
    this.negotiationRepository = negotiationRepository;
    this.requirementRepository = requirementRepository;
    this.requirementSubmissionRepository = requirementSubmissionRepository;
    this.persistStateMachineHandler = persistStateMachineHandler;
    this.stateMachine = stateMachine;
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

  public Map<String, Object> getStateMachineDiagram() {
    Map<String, Object> diagram = new HashMap<>();
    State<String, String> initialState = stateMachine.getInitialState();

    traverseState(initialState, diagram, stateMachine);

    return diagram;
  }

  private void traverseState(
      State<String, String> state,
      Map<String, Object> diagram,
      StateMachine<String, String> stateMachine) {
    Map<String, Object> transitions = new HashMap<>();
    String stateId = state.getId();

    for (Transition<String, String> transition : stateMachine.getTransitions()) {
      if (transition.getSource().getId().equals(stateId)) {
        Map<String, Object> transitionMap = new HashMap<>();
        transitionMap.put("target", transition.getTarget().getId());
        transitionMap.put("event", transition.getTrigger().getEvent());
        transitions.put(transition.getTrigger().getEvent(), transitionMap);
        traverseState(transition.getTarget(), transitionMap, stateMachine);
      }
    }

    if (!transitions.isEmpty()) {
      diagram.put(stateId, transitions);
    }
  }

  @Override
  public NegotiationResourceState sendEvent(
      String negotiationId, String resourceId, NegotiationResourceEvent negotiationResourceEvent)
      throws WrongRequestException, EntityNotFoundException {
    if (requirementRepository.existsByForEvent(negotiationResourceEvent.name())
        && !requirementSubmissionRepository.existsByResource_SourceIdAndNegotiation_Id(
            resourceId, negotiationId)) {
      throw new LifecycleConflictException(
          LifecycleProblemType.UNMET_REQUIREMENT,
          "%s cannot fire for this Resource until its Information Requirement form is submitted."
              .formatted(negotiationResourceEvent.getLabel()));
    }
    NegotiationResourceState currentState = getCurrentStateForResource(negotiationId, resourceId);
    Transition<String, String> transition =
        stateMachine.getTransitions().stream()
            .filter(t -> t.getSource().getId().equals(currentState.name()))
            .filter(t -> t.getTrigger().getEvent().equals(negotiationResourceEvent.name()))
            .findFirst()
            .orElseThrow(
                () ->
                    new LifecycleConflictException(
                        LifecycleProblemType.NO_TRANSITION,
                        "The Resource is %s, and no Transition leaves that State on %s."
                            .formatted(
                                currentState.getLabel(), negotiationResourceEvent.getLabel())));
    if (!isSecurityRuleMet(transition.getSecurityRule(), negotiationId, resourceId)) {
      throw new ForbiddenRequestException(
          "You are not allowed to %s for this Resource"
              .formatted(negotiationResourceEvent.getLabel().toLowerCase()));
    }
    NegotiationState negotiationState = getNegotiation(negotiationId).getCurrentState();
    if (!negotiationState.equals(NegotiationState.IN_PROGRESS)) {
      throw new LifecycleConflictException(
          LifecycleProblemType.DOMAIN_STATE_CONFLICT,
          "%s cannot fire while the Negotiation is %s, only while it is %s."
              .formatted(
                  negotiationResourceEvent.getLabel(),
                  negotiationState.getLabel(),
                  NegotiationState.IN_PROGRESS.getLabel()));
    }
    persistStateMachineHandler
        .handleEventWithStateReactively(
            MessageBuilder.withPayload(negotiationResourceEvent.name())
                .setHeader("negotiationId", negotiationId)
                .setHeader("resourceId", resourceId)
                .build(),
            currentState.name())
        .subscribe();
    return getCurrentStateForResource(negotiationId, resourceId);
  }

  private NegotiationResourceState getCurrentStateForResource(
      String negotiationId, String resourceId) throws EntityNotFoundException {
    return negotiationRepository
        .findNegotiationResourceStateById(negotiationId, resourceId)
        .orElseThrow(() -> new EntityNotFoundException(negotiationId));
  }

  private Set<NegotiationResourceEvent> getPossibleEventsForCurrentStateMachine(
      String negotiationId, String resourceId, NegotiationResourceState resourceState) {
    Negotiation negotiation = getNegotiation(negotiationId);
    if (!negotiation.getCurrentState().equals(NegotiationState.IN_PROGRESS)) {
      return Set.of();
    }
    return stateMachine.getTransitions().stream()
        .filter(transition -> transition.getSource().getId().equals(resourceState.toString()))
        .filter(
            transition ->
                isSecurityRuleMet(transition.getSecurityRule(), negotiationId, resourceId))
        .map(transition -> transition.getTrigger().getEvent())
        .map(NegotiationResourceEvent::valueOf)
        .collect(Collectors.toSet());
  }

  private Negotiation getNegotiation(String negotiationId) {
    return negotiationRepository
        .findById(negotiationId)
        .orElseThrow(() -> new EntityNotFoundException(negotiationId));
  }

  private boolean isSecurityRuleMet(
      SecurityRule securityRule, String negotiationId, String resourceId) {
    if (securityRule == null || securityRule.getAttributes().isEmpty()) {
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
    if (securityRule.getAttributes().contains("isCreator")) {
      return negotiationRepository.existsByIdAndCreatedBy_Id(negotiationId, creatorId);
    } else if (securityRule.getAttributes().contains("isRepresentative")) {
      return personService.isRepresentativeOfAnyResource(
          AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId(), List.of(resourceId));
    } else if (securityRule.getAttributes().contains("isAdmin")) {
      return Objects.isNull(SecurityContextHolder.getContext().getAuthentication())
          || AuthenticatedUserContext.isCurrentlyAuthenticatedUserAdmin();
    }
    return true;
  }
}
