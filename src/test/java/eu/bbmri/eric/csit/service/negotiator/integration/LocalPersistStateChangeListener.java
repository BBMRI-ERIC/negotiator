package eu.bbmri.eric.csit.service.negotiator.integration;

import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.recipes.persist.PersistStateMachineHandler;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Service;

@CommonsLog
@Service("testListener")
public class LocalPersistStateChangeListener implements PersistStateMachineHandler.PersistStateChangeListener {


    @Autowired
    NegotiationRepository negotiationRepository;


    @Override
    public void onPersist(State<String, String> state, Message<String> message, Transition<String, String> transition, StateMachine<String, String> stateMachine) {
        log.error("got here");
    }
}
