package eu.bbmri_eric.negotiator.negotiation.state_machine.resource;

import eu.bbmri_eric.negotiator.common.AuthenticatedUserContext;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.WrongRequestException;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementRepository;
import eu.bbmri_eric.negotiator.info_submission.InformationSubmissionRepository;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
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
import org.springframework.statemachine.StateMachineException;
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
    NegotiationResourceState currentState = getCurrentStateForResource(negotiationId, resourceId);
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
    if (requirementRepository.existsByForEvent(negotiationResourceEvent)
        && !requirementSubmissionRepository.existsByResource_SourceIdAndNegotiation_Id(
            resourceId, negotiationId)) {
      throw new StateMachineException(
          "The requirement for this operation was not met. Please make sure you have submitted the required form and try again.");
    }
    if (!getPossibleEvents(negotiationId, resourceId).contains(negotiationResourceEvent)) {
      return getCurrentStateForResource(negotiationId, resourceId);
    }
    persistStateMachineHandler
        .handleEventWithStateReactively(
            MessageBuilder.withPayload(negotiationResourceEvent.name())
                .setHeader("negotiationId", negotiationId)
                .setHeader("resourceId", resourceId)
                .build(),
            getCurrentStateForResource(negotiationId, resourceId).name())
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
    return stateMachine.getTransitions().stream()
        .filter(transition -> transition.getSource().getId().equals(resourceState.toString()))
        .filter(
            transition ->
                isSecurityRuleMet(transition.getSecurityRule(), negotiationId, resourceId))
        .map(transition -> transition.getTrigger().getEvent())
        .map(NegotiationResourceEvent::valueOf)
        .collect(Collectors.toSet());
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
