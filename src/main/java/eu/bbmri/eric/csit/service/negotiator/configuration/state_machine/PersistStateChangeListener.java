package eu.bbmri.eric.csit.service.negotiator.configuration.state_machine;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.recipes.persist.PersistStateMachineHandler;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Service;

@Service("persistListener")
@CommonsLog
public class PersistStateChangeListener
    implements PersistStateMachineHandler.PersistStateChangeListener {
  public PersistStateChangeListener() {
    log.warn("heree");
  }

  @Override
  public void onPersist(
      State<String, String> state,
      Message<String> message,
      Transition<String, String> transition,
      StateMachine<String, String> stateMachine) {
    log.warn("got here");
  }
}
