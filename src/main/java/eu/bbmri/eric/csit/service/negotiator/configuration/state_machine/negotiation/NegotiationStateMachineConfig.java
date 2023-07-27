package eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.negotiation;

import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationEvent;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationState;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

/** Configuration of the SpringStateMachine(SSM) for the Negotiation Lifecycle. */
@Configuration
@EnableStateMachine(name = "negotiationStateMachine")
public class NegotiationStateMachineConfig extends StateMachineConfigurerAdapter<String, String> {

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
        .target(NegotiationState.ONGOING.name())
        .event(NegotiationEvent.APPROVE.name())
        .action(enablePosts())
        .action(initializeStateForResources())
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

  @Bean
  public Action<String, String> enablePosts() {
    return new EnablePostsAction();
  }

  @Bean
  public Action<String, String> initializeStateForResources() {
    return new InitializeStateForResourceAction();
  }
}
