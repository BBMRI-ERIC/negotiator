package eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.recipes.persist.PersistStateMachineHandler;

/** Configuration of the Persist internal for SSM Negotiation Lifecycle. */
@Configuration
public class PersistHandlerConfig {

  @Autowired
  @Qualifier("negotiationStateMachine")
  private StateMachine<String, String> stateMachine;

  @Autowired
  @Qualifier("persistListener")
  private PersistStateChangeListener persistStateChangeListener;

  @Bean(name = "persistHandler")
  public PersistStateMachineHandler persistStateMachineHandler() {
    PersistStateMachineHandler handler = new PersistStateMachineHandler(stateMachine);
    handler.addPersistStateChangeListener(persistStateChangeListener);
    return handler;
  }
}
