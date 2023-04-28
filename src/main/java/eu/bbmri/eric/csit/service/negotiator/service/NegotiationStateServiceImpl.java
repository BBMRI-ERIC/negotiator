package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationEvent;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationState;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Spring State Machine implementation of the NegotiationStateService
 */
@Service
public class NegotiationStateServiceImpl implements NegotiationStateService{

    @Autowired
    private NegotiationRepository negotiationRepository;

    @Autowired
    @Qualifier("negotiationStateMachineService")
    private StateMachineService<NegotiationState, NegotiationEvent> springStateMachineService;

    @Override
    public void createStateMachineForNegotiation(String negotiationId) {
        springStateMachineService.acquireStateMachine(negotiationId);
    }

    @Override
    public NegotiationState getNegotiationState(String negotiationId) {
        return this.springStateMachineService.acquireStateMachine(negotiationId).getState().getId();
    }

    @Override
    public List<NegotiationEvent> getPossibleEvents(String negotiationId) {
        StateMachine<NegotiationState, NegotiationEvent> stateMachine = this.springStateMachineService.acquireStateMachine(negotiationId);
        return stateMachine.getTransitions().stream()
                .filter(transition -> transition.getSource().getId().equals(stateMachine.getState().getId()))
                .map(transition -> transition.getTrigger().getEvent())
                .collect(Collectors.toList());
    }

    @Override
    public NegotiationState sendEvent(String negotiationId, NegotiationEvent negotiationEvent) {
        StateMachine<NegotiationState, NegotiationEvent> stateMachine = this.springStateMachineService.acquireStateMachine(negotiationId, true);
        stateMachine.sendEvent(negotiationEvent);
        return stateMachine.getState().getId();
    }
}
