package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationEvent;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationState;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring State Machine implementation of the NegotiationStateService
 */
@Service
public class NegotiationStateServiceImpl implements NegotiationStateService{

    @Autowired
    @Qualifier("negotiationStateMachineService")
    private StateMachineService<NegotiationState, NegotiationEvent> springStateMachineService;

    @Override
    public NegotiationState getNegotiationState(String negotiationId) {
        return this.springStateMachineService.acquireStateMachine(negotiationId).getState().getId();
    }

    @Override
    public List<NegotiationEvent> getPossibleEvents(String negotiationId) {
        return null;
    }

    @Override
    public NegotiationState sendEvent(String negotiationId, NegotiationEvent negotiationEvent) {
        return null;
    }
}
