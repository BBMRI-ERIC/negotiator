package eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.resource;

import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationResourceEvent;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationResourceState;
import java.util.EnumSet;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.persist.StateMachineRuntimePersister;

@Configuration
@EnableStateMachineFactory(name = "negotiationResourceStateMachineFactory")
@Log
public class NegotiationResourceStateMachineConfig
    extends EnumStateMachineConfigurerAdapter<NegotiationResourceState, NegotiationResourceEvent> {

  @Autowired
  @Qualifier("negotiationResourceStateMachineRuntimePersister")
  private StateMachineRuntimePersister<NegotiationResourceState, NegotiationResourceEvent, String>
      negotiationResourceStateMachineRuntimePersister;

  @Override
  public void configure(
      StateMachineConfigurationConfigurer<NegotiationResourceState, NegotiationResourceEvent>
          config)
      throws Exception {
    config
        .withPersistence()
        .runtimePersister(negotiationResourceStateMachineRuntimePersister)
        .and()
        .withConfiguration()
        .autoStartup(true);
  }

  @Override
  public void configure(
      StateMachineStateConfigurer<NegotiationResourceState, NegotiationResourceEvent> states)
      throws Exception {
    states
        .withStates()
        .initial(NegotiationResourceState.SUBMITTED)
        .states(EnumSet.allOf(NegotiationResourceState.class));
  }

  @Override
  public void configure(
      StateMachineTransitionConfigurer<NegotiationResourceState, NegotiationResourceEvent>
          transitions)
      throws Exception {
    transitions
        .withExternal()
        .source(NegotiationResourceState.SUBMITTED)
        .event(NegotiationResourceEvent.CONTACT)
        .target(NegotiationResourceState.REPRESENTATIVE_CONTACTED)
        .and()
        .withExternal()
        .source(NegotiationResourceState.SUBMITTED)
        .event(NegotiationResourceEvent.MARK_AS_UNREACHABLE)
        .target(NegotiationResourceState.REPRESENTATIVE_UNREACHABLE)
        .and()
        .withExternal()
        .source(NegotiationResourceState.REPRESENTATIVE_CONTACTED)
        .event(NegotiationResourceEvent.MARK_AS_CHECKING_AVAILABILITY)
        .target(NegotiationResourceState.CHECKING_AVAILABILITY)
        .and()
        .withExternal()
        .source(NegotiationResourceState.REPRESENTATIVE_CONTACTED)
        .event(NegotiationResourceEvent.MARK_AS_UNAVAILABLE)
        .target(NegotiationResourceState.RESOURCE_UNAVAILABLE)
        .and()
        .withExternal()
        .source(NegotiationResourceState.CHECKING_AVAILABILITY)
        .event(NegotiationResourceEvent.MARK_AS_AVAILABLE)
        .target(NegotiationResourceState.RESOURCE_AVAILABLE)
        .and()
        .withExternal()
        .source(NegotiationResourceState.RESOURCE_AVAILABLE)
        .event(NegotiationResourceEvent.INDICATE_ACCESS_CONDITIONS)
        .target(NegotiationResourceState.ACCESS_CONDITIONS_INDICATED)
        .and()
        .withExternal()
        .source(NegotiationResourceState.ACCESS_CONDITIONS_INDICATED)
        .event(NegotiationResourceEvent.MARK_ACCESS_CONDITIONS_AS_MET)
        .target(NegotiationResourceState.ACCESS_CONDITIONS_MET)
        .and()
        .withExternal()
        .source(NegotiationResourceState.ACCESS_CONDITIONS_MET)
        .event(NegotiationResourceEvent.GRANT_ACCESS_TO_RESOURCE)
        .target(NegotiationResourceState.RESOURCE_MADE_AVAILABLE);
  }
}
