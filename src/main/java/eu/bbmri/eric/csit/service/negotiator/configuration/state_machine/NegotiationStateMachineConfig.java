package eu.bbmri.eric.csit.service.negotiator.configuration.state_machine;

import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationEvent;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationState;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

@Configuration
@EnableStateMachine
@Log
public class NegotiationStateMachineConfig extends StateMachineConfigurerAdapter<String, String> {

  @Autowired private NegotiationStateMachineActions actions;

  @Override
  public void configure(StateMachineConfigurationConfigurer<String, String> config)
      throws Exception {
    config.withConfiguration().autoStartup(true);
  }

  @Override
  public void configure(StateMachineStateConfigurer<String, String> states) throws Exception {
    Set<String> stringStates = new HashSet<>();
    EnumSet.allOf(NegotiationState.class).forEach(entity -> stringStates.add(entity.name()));
    states.withStates().initial(NegotiationState.SUBMITTED.name()).states(stringStates);
  }

  @Override
  public void configure(StateMachineTransitionConfigurer<String, String> transitions)
      throws Exception {
    transitions
        .withExternal()
        .source(NegotiationState.SUBMITTED.name())
        .target(NegotiationState.APPROVED.name())
        .target(NegotiationState.ONGOING.name())
        .event(NegotiationEvent.APPROVE.name())
        .action(actions.enablePosts())
        .and()
        .withExternal()
        .source(NegotiationState.SUBMITTED.name())
        .target(NegotiationState.DECLINED.name())
        .event(NegotiationEvent.DECLINE.name())
        .and()
        .withExternal()
        .source(NegotiationState.ONGOING.name())
        .target(NegotiationState.PAUSED.name())
        .event(NegotiationEvent.PAUSE.name())
        .and()
        .withExternal()
        .source(NegotiationState.PAUSED.name())
        .target(NegotiationState.ONGOING.name())
        .event(NegotiationEvent.UNPAUSE.name())
        .and()
        .withExternal()
        .source(NegotiationState.PAUSED.name())
        .target(NegotiationState.ABANDONED.name())
        .event(NegotiationEvent.ABANDON.name())
        .and()
        .withExternal()
        .source(NegotiationState.ONGOING.name())
        .target(NegotiationState.CONCLUDED.name())
        .event(NegotiationEvent.CONCLUDE.name());
  }
}
