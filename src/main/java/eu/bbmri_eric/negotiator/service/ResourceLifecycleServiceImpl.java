package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.configuration.security.auth.NegotiatorUserDetailsService;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.database.repository.NegotiationRepository;
import eu.bbmri_eric.negotiator.database.repository.NegotiationResourceLifecycleRecordRepository;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.exceptions.WrongRequestException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.recipes.persist.PersistStateMachineHandler;
import org.springframework.statemachine.security.SecurityRule;
import org.springframework.stereotype.Service;

/** Spring State Machine implementation of the ResourceLifecycleService. */
@Service
@CommonsLog
public class ResourceLifecycleServiceImpl implements ResourceLifecycleService {

  @Autowired NegotiationRepository negotiationRepository;

  @Autowired
  NegotiationResourceLifecycleRecordRepository negotiationResourceLifecycleRecordRepository;

  @Autowired
  @Qualifier("resourcePersistHandler")
  private PersistStateMachineHandler persistStateMachineHandler;

  @Autowired
  @Qualifier("resourceStateMachine")
  private StateMachine<String, String> stateMachine;

  @Autowired PersonService personService;

  @Override
  public Set<NegotiationResourceEvent> getPossibleEvents(String negotiationId, String resourceId)
      throws EntityNotFoundException {
    NegotiationResourceState currentState = getCurrentStateForResource(negotiationId, resourceId);
    if (Objects.isNull(currentState)) {
      throw new EntityNotFoundException(resourceId);
    }
    return getPossibleEventsForCurrentStateMachine(negotiationId, resourceId, currentState);
  }

  @Override
  public NegotiationResourceState sendEvent(
      String negotiationId, String resourceId, NegotiationResourceEvent negotiationEvent)
      throws WrongRequestException, EntityNotFoundException {
    if (!getPossibleEvents(negotiationId, resourceId).contains(negotiationEvent)) {
      return getCurrentStateForResource(negotiationId, resourceId);
    }
    persistStateMachineHandler
        .handleEventWithStateReactively(
            MessageBuilder.withPayload(negotiationEvent.name())
                .setHeader("negotiationId", negotiationId)
                .setHeader("resourceId", resourceId)
                .build(),
            getCurrentStateForResource(negotiationId, resourceId).name())
        .subscribe();
    return getCurrentStateForResource(negotiationId, resourceId);
  }

  private NegotiationResourceState getCurrentStateForResource(
      String negotiationId, String resourceId) throws EntityNotFoundException {
    return negotiationResourceLifecycleRecordRepository
        .findNegotiationResourceStateById(negotiationId, resourceId)
        .orElseThrow(() -> new EntityNotFoundException("No such negotiation found."));
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
    if (securityRule == null || securityRule.getExpression() == null) {
      return true;
    }
    if (securityRule.getExpression().equals("isCreator")) {
      Long creatorId;
      try {
        creatorId = NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId();
      } catch (ClassCastException e) {
        return false;
      }
      return negotiationRepository.existsByIdAndCreatedBy_Id(negotiationId, creatorId);
    } else if (securityRule.getExpression().equals("isRepresentative")) {
      return personService.isRepresentativeOfAnyResource(
          NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId(),
          List.of(resourceId));
    } else {
      return true;
    }
  }
}
