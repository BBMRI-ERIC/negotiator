package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationEvent;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationState;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.WrongRequestException;
import lombok.NoArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineEventResult;
import org.springframework.statemachine.data.jpa.JpaStateMachineRepository;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring State Machine implementation of the NegotiationLifecycleService
 */
@Service
@CommonsLog
@NoArgsConstructor
public class NegotiationLifecycleServiceImpl implements NegotiationLifecycleService {

  private static final String SEPARATOR = "---";
  @Autowired
  JpaStateMachineRepository jpaStateMachineRepository;

  @Autowired
  @Qualifier("negotiationStateMachineService")
  private StateMachineService<NegotiationState, NegotiationEvent> springNegotiationStateMachineService;

  @Override
  public void initializeTheStateMachine(String negotiationId) {
    springNegotiationStateMachineService.acquireStateMachine(negotiationId, false);
  }

  @Override
  public NegotiationState getCurrentState(String negotiationId) {
    if (!jpaStateMachineRepository.existsById(negotiationId)) {
      throw new EntityNotFoundException("Negotiation does not exist");
    }
    return this.springNegotiationStateMachineService.acquireStateMachine(negotiationId).getState()
        .getId();
  }

  @Override
  public Set<NegotiationEvent> getPossibleEvents(String negotiationId) {
    if (!jpaStateMachineRepository.existsById(negotiationId)) {
      throw new EntityNotFoundException("StateMachine " + negotiationId + "does not exist.");
    }
    StateMachine<NegotiationState, NegotiationEvent> stateMachine = this.springNegotiationStateMachineService.acquireStateMachine(
        negotiationId);
    return stateMachine.getTransitions().stream()
        .filter(
            transition -> transition.getSource().getId().equals(stateMachine.getState().getId()))
        .map(transition -> transition.getTrigger().getEvent())
        .collect(Collectors.toSet());
  }

  @Override
  public NegotiationState sendEvent(String negotiationId, NegotiationEvent negotiationEvent) {
    return sendEventToMachine(negotiationId, negotiationEvent);
  }

  private NegotiationState sendEventToMachine(String machineId, NegotiationEvent negotiationEvent) {
    if (!jpaStateMachineRepository.existsById(machineId)) {
      throw new EntityNotFoundException("Negotiation " + machineId + "does not exist.");
    }
    if (!getPossibleEvents(machineId).contains(negotiationEvent)) {
      throw new WrongRequestException("Unsupported event");
    }
    StateMachine<NegotiationState, NegotiationEvent> stateMachine = this.springNegotiationStateMachineService.acquireStateMachine(
        machineId, true);
    StateMachineEventResult<NegotiationState, NegotiationEvent> newState = stateMachine.sendEvent(
        Mono.just(MessageBuilder.withPayload(negotiationEvent).build())).blockFirst();
    if (newState != null) {
      log.info("Event was: " + newState.getResultType().toString());
    }
    return stateMachine.getState().getId();
  }

  public void removeStateMachine(String negotiationId) {
    springNegotiationStateMachineService.releaseStateMachine(negotiationId);
  }


}
