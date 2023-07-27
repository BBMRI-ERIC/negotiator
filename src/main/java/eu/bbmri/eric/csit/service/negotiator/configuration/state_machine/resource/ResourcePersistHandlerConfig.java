package eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.recipes.persist.PersistStateMachineHandler;

/** Persist handler configuration for the Resource StateMachine. */
@Configuration
public class ResourcePersistHandlerConfig {
  @Autowired
  @Qualifier("resourceStateMachine")
  private StateMachine<String, String> stateMachine;

  @Autowired
  @Qualifier("resourcePersistListener")
  private ResourcePersistStateChangeListener persistStateChangeListener;

  @Bean(name = "resourcePersistHandler")
  public PersistStateMachineHandler persistStateMachineHandler() {
    PersistStateMachineHandler handler = new PersistStateMachineHandler(stateMachine);
    handler.addPersistStateChangeListener(persistStateChangeListener);
    return handler;
  }
}
