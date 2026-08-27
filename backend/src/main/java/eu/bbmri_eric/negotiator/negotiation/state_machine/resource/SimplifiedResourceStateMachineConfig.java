package eu.bbmri_eric.negotiator.negotiation.state_machine.resource;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.guard.Guard;
import org.springframework.statemachine.security.SecurityRule;

/** Simplified resource state machine active under the minimal-workflow Spring profile. */
@Configuration
@Profile("minimal-workflow")
@EnableStateMachine(name = "resourceStateMachine")
public class SimplifiedResourceStateMachineConfig
    extends StateMachineConfigurerAdapter<String, String> {

  @Override
  public void configure(StateMachineConfigurationConfigurer<String, String> config)
      throws Exception {
    config.withConfiguration().autoStartup(true);
  }

  @Override
  public void configure(StateMachineStateConfigurer<String, String> states) throws Exception {
    Set<String> stringStates = new HashSet<>();
    EnumSet.allOf(NegotiationResourceState.class)
        .forEach(entity -> stringStates.add(entity.name()));
    states.withStates().initial(NegotiationResourceState.SUBMITTED.name()).states(stringStates);
  }

  @Override
  public void configure(StateMachineTransitionConfigurer<String, String> transitions)
      throws Exception {
    transitions
        .withExternal()
        .source(NegotiationResourceState.SUBMITTED.name())
        .event(NegotiationResourceEvent.CONTACT.name())
        .target(NegotiationResourceState.REPRESENTATIVE_CONTACTED.name())
        .secured("isAdmin", SecurityRule.ComparisonType.ALL)
        .and()
        .withExternal()
        .source(NegotiationResourceState.SUBMITTED.name())
        .event(NegotiationResourceEvent.MARK_AS_UNREACHABLE.name())
        .target(NegotiationResourceState.REPRESENTATIVE_UNREACHABLE.name())
        .secured("isAdmin", SecurityRule.ComparisonType.ALL)
        .and()
        .withExternal()
        .source(NegotiationResourceState.REPRESENTATIVE_UNREACHABLE.name())
        .event(NegotiationResourceEvent.CONTACT.name())
        .target(NegotiationResourceState.REPRESENTATIVE_CONTACTED.name())
        .secured("isAdmin", SecurityRule.ComparisonType.ALL)
        .and()
        .withExternal()
        .source(NegotiationResourceState.REPRESENTATIVE_CONTACTED.name())
        .event(NegotiationResourceEvent.GRANT_ACCESS_TO_RESOURCE.name())
        .target(NegotiationResourceState.RESOURCE_MADE_AVAILABLE.name())
        .secured("isRepresentative", SecurityRule.ComparisonType.ALL)
        .and()
        .withExternal()
        .source(NegotiationResourceState.REPRESENTATIVE_CONTACTED.name())
        .event(NegotiationResourceEvent.STEP_AWAY.name())
        .target(NegotiationResourceState.RESOURCE_UNAVAILABLE.name())
        .secured("isRepresentative", SecurityRule.ComparisonType.ALL);

    transitions.withExternal().guard(negotiationIsApproved());
  }

  @Bean
  public Guard<String, String> negotiationIsApproved() {
    return new NegotiationIsApprovedGuard();
  }
}
