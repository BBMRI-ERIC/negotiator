package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationEvent;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationState;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.WrongRequestException;
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
 * Spring State Machine implementation of the NegotiationStateService
 */
@Service
@CommonsLog
public class NegotiationStateServiceImpl implements NegotiationStateService{
    private static final String SEPARATOR = "---";

    @Autowired
    @Qualifier("negotiationStateMachineService")
    private StateMachineService<NegotiationState, NegotiationEvent> springNegotiationStateMachineService;


    @Autowired
    @Qualifier("negotiationResourceStateMachineService")
    private StateMachineService<NegotiationState, NegotiationEvent> springNegotiationResourceStateMachineService;

    @Autowired
    JpaStateMachineRepository jpaStateMachineRepository;

    @Override
    public void initializeTheStateMachine(String negotiationId) {
        springNegotiationStateMachineService.acquireStateMachine(negotiationId, false);
    }

    @Override
    public void initializeTheStateMachine(String negotiationId, String resourceId) {
        String resourceNegotiationId = negotiationId + SEPARATOR + resourceId;
        springNegotiationResourceStateMachineService.acquireStateMachine(resourceNegotiationId, false);
    }

    @Override
    public NegotiationState getCurrentState(String negotiationId) {
        if (!jpaStateMachineRepository.existsById(negotiationId)){
            throw new EntityNotFoundException("Negotiation does not exist");
        }
        return this.springNegotiationStateMachineService.acquireStateMachine(negotiationId).getState().getId();
    }

    @Override
    public NegotiationState getCurrentState(String negotiationId, String resourceId) {
        String negotiationResourceId = createMachineId(negotiationId, resourceId);
        if (!jpaStateMachineRepository.existsById(negotiationResourceId)){
            throw new EntityNotFoundException("Combination of Negotiation and Resource does not exist");
        }
        return this.springNegotiationStateMachineService.acquireStateMachine(negotiationResourceId).getState().getId();
    }

    @Override
    public Set<NegotiationEvent> getPossibleEvents(String negotiationId) {
        if (!jpaStateMachineRepository.existsById(negotiationId)) {
            throw new EntityNotFoundException("StateMachine " + negotiationId + "does not exist.");
        }
        StateMachine<NegotiationState, NegotiationEvent> stateMachine = this.springNegotiationStateMachineService.acquireStateMachine(negotiationId);
        return stateMachine.getTransitions().stream()
                .filter(transition -> transition.getSource().getId().equals(stateMachine.getState().getId()))
                .map(transition -> transition.getTrigger().getEvent())
                .collect(Collectors.toSet());
    }

    @Override
    public Set<NegotiationEvent> getPossibleEvents(String negotiationId, String resourceId) {
        String machineId = createMachineId(negotiationId, resourceId);
        if (!jpaStateMachineRepository.existsById(machineId)) {
            throw new EntityNotFoundException("StateMachine " + machineId + "does not exist.");
        }
        StateMachine<NegotiationState, NegotiationEvent> stateMachine = this.springNegotiationResourceStateMachineService.acquireStateMachine(machineId);
        return stateMachine.getTransitions().stream()
                .filter(transition -> transition.getSource().getId().equals(stateMachine.getState().getId()))
                .map(transition -> transition.getTrigger().getEvent())
                .collect(Collectors.toSet());
    }

    @Override
    public NegotiationState sendEvent(String negotiationId, NegotiationEvent negotiationEvent) {
        return sendEventToMachine(negotiationId, negotiationEvent);
    }

    @Override
    public NegotiationState sendEvent(String negotiationId, String resourceId, NegotiationEvent negotiationEvent) throws WrongRequestException, EntityNotFoundException {
        return sendEventToMachine(createMachineId(negotiationId, resourceId), negotiationEvent);
    }

    private NegotiationState sendEventToMachine(String machineId, NegotiationEvent negotiationEvent) {
        if (!jpaStateMachineRepository.existsById(machineId)) {
            throw new EntityNotFoundException("Negotiation " + machineId + "does not exist.");
        }
        if (!getPossibleEvents(machineId).contains(negotiationEvent)){
            throw new WrongRequestException("Unsupported event");
        }
        StateMachine<NegotiationState, NegotiationEvent> stateMachine = this.springNegotiationStateMachineService.acquireStateMachine(machineId, true);
        StateMachineEventResult<NegotiationState, NegotiationEvent> newState = stateMachine.sendEvent(Mono.just(MessageBuilder.withPayload(negotiationEvent).build())).blockFirst();
        if (newState != null) {
            log.info("Event was: " + newState.getResultType().toString());
        }
        return stateMachine.getState().getId();
    }

    public void removeStateMachine(String negotiationId){
        springNegotiationStateMachineService.releaseStateMachine(negotiationId);
    }

    private String createMachineId(String negotiationId, String resourceId){
        return negotiationId + SEPARATOR + resourceId;
    }
}
