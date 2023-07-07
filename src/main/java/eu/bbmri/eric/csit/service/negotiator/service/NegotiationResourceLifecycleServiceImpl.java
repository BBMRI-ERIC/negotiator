package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationResourceEvent;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationResourceState;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.WrongRequestException;
import java.util.Set;
import java.util.stream.Collectors;
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

@Service
@CommonsLog
public class NegotiationResourceLifecycleServiceImpl
    implements NegotiationResourceLifecycleService {

  private static final String SEPARATOR = "---";
  @Autowired JpaStateMachineRepository jpaStateMachineRepository;

  @Autowired
  @Qualifier("negotiationResourceStateMachineService")
  private StateMachineService<NegotiationResourceState, NegotiationResourceEvent>
      springNegotiationResourceStateMachineService;

  @Override
  public void initializeTheStateMachine(String negotiationId, String resourceId) {
    String resourceNegotiationId = createMachineId(negotiationId, resourceId);
    springNegotiationResourceStateMachineService.acquireStateMachine(resourceNegotiationId, false);
  }

  @Override
  public NegotiationResourceState getCurrentState(String negotiationId, String resourceId)
      throws EntityNotFoundException {
    String negotiationResourceId = createMachineId(negotiationId, resourceId);
    if (!jpaStateMachineRepository.existsById(negotiationResourceId)) {
      throw new EntityNotFoundException("Combination of Negotiation and Resource does not exist");
    }
    return this.springNegotiationResourceStateMachineService
        .acquireStateMachine(negotiationResourceId)
        .getState()
        .getId();
  }

  @Override
  public Set<NegotiationResourceEvent> getPossibleEvents(String negotiationId, String resourceId)
      throws EntityNotFoundException {
    String machineId = createMachineId(negotiationId, resourceId);
    if (!jpaStateMachineRepository.existsById(machineId)) {
      throw new EntityNotFoundException("StateMachine " + machineId + "does not exist.");
    }
    StateMachine<NegotiationResourceState, NegotiationResourceEvent> stateMachine =
        this.springNegotiationResourceStateMachineService.acquireStateMachine(machineId);
    return stateMachine.getTransitions().stream()
        .filter(
            transition -> transition.getSource().getId().equals(stateMachine.getState().getId()))
        .map(transition -> transition.getTrigger().getEvent())
        .collect(Collectors.toSet());
  }

  @Override
  public NegotiationResourceState sendEvent(
      String negotiationId, String resourceId, NegotiationResourceEvent negotiationEvent)
      throws WrongRequestException, EntityNotFoundException {
    StateMachine<NegotiationResourceState, NegotiationResourceEvent> stateMachine =
        this.springNegotiationResourceStateMachineService.acquireStateMachine(
            createMachineId(negotiationId, resourceId), true);
    StateMachineEventResult<NegotiationResourceState, NegotiationResourceEvent> newState =
        stateMachine
            .sendEvent(Mono.just(MessageBuilder.withPayload(negotiationEvent).build()))
            .blockFirst();
    if (newState != null) {
      log.info("Event was: " + newState.getResultType().toString());
    }
    return stateMachine.getState().getId();
  }

  private String createMachineId(String negotiationId, String resourceId) {
    return negotiationId + SEPARATOR + resourceId;
  }
}
