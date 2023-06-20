package eu.bbmri.eric.csit.service.negotiator.configuration.state_machine;

import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationResourceEvent;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationResourceState;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.persist.StateMachineRuntimePersister;


@Configuration
@EnableStateMachineFactory(name = "negotiationResourceStateMachineFactory")
@Log
public class NegotiationResourceStateMachineConfig extends
    EnumStateMachineConfigurerAdapter<NegotiationResourceState, NegotiationResourceEvent> {

  @Autowired
  @Qualifier("negotiationResourceStateMachineRuntimePersister")
  private StateMachineRuntimePersister<NegotiationResourceState, NegotiationResourceEvent, String> negotiationResourceStateMachineRuntimePersister;

  @Override
  public void configure(
      StateMachineConfigurationConfigurer<NegotiationResourceState, NegotiationResourceEvent> config)
      throws Exception {
    config.withPersistence().runtimePersister(negotiationResourceStateMachineRuntimePersister).and()
        .withConfiguration()
        .autoStartup(true);
  }
}
