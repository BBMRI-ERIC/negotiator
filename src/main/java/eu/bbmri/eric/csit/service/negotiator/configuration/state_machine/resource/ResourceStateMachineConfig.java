package eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.resource;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.guard.Guard;
import org.springframework.statemachine.security.SecurityRule;

/** Configuration for the Resource State Machine. */
@Configuration
@EnableStateMachine(name = "resourceStateMachine")
public class ResourceStateMachineConfig extends StateMachineConfigurerAdapter<String, String> {

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
        .secured("ROLE_ADMIN", SecurityRule.ComparisonType.ALL)
        .and()
        .withExternal()
        .source(NegotiationResourceState.SUBMITTED.name())
        .event(NegotiationResourceEvent.MARK_AS_UNREACHABLE.name())
        .target(NegotiationResourceState.REPRESENTATIVE_UNREACHABLE.name())
        .secured("ROLE_ADMIN", SecurityRule.ComparisonType.ALL)
        .and()
        .withExternal()
        .source(NegotiationResourceState.REPRESENTATIVE_UNREACHABLE.name())
        .event(NegotiationResourceEvent.CONTACT.name())
        .target(NegotiationResourceState.REPRESENTATIVE_CONTACTED.name())
        .secured("isRepresentative")
        .and()
        .withExternal()
        .source(NegotiationResourceState.REPRESENTATIVE_CONTACTED.name())
        .event(NegotiationResourceEvent.MARK_AS_CHECKING_AVAILABILITY.name())
        .target(NegotiationResourceState.CHECKING_AVAILABILITY.name())
        .secured("isRepresentative")
        .and()
        .withExternal()
        .source(NegotiationResourceState.REPRESENTATIVE_CONTACTED.name())
        .event(NegotiationResourceEvent.STEP_AWAY.name())
        .target(NegotiationResourceState.RESOURCE_UNAVAILABLE.name())
        .secured("isRepresentative")
        .and()
        .withExternal()
        .source(NegotiationResourceState.CHECKING_AVAILABILITY.name())
        .event(NegotiationResourceEvent.MARK_AS_UNAVAILABLE.name())
        .target(NegotiationResourceState.RESOURCE_UNAVAILABLE.name())
        .secured("isRepresentative")
        .and()
        .withExternal()
        .source(NegotiationResourceState.CHECKING_AVAILABILITY.name())
        .event(NegotiationResourceEvent.MARK_AS_CURRENTLY_UNAVAILABLE_BUT_WILLING_TO_COLLECT.name())
        .target(NegotiationResourceState.RESOURCE_UNAVAILABLE_WILLING_TO_COLLECT.name())
        .secured("isRepresentative")
        .and()
        .withExternal()
        .source(NegotiationResourceState.CHECKING_AVAILABILITY.name())
        .event(NegotiationResourceEvent.MARK_AS_AVAILABLE.name())
        .target(NegotiationResourceState.RESOURCE_AVAILABLE.name())
        .secured("isRepresentative")
        .and()
        .withExternal()
        .source(NegotiationResourceState.RESOURCE_AVAILABLE.name())
        .event(NegotiationResourceEvent.INDICATE_ACCESS_CONDITIONS.name())
        .target(NegotiationResourceState.ACCESS_CONDITIONS_INDICATED.name())
        .secured("isRepresentative")
        .and()
        .withExternal()
        .source(NegotiationResourceState.RESOURCE_UNAVAILABLE_WILLING_TO_COLLECT.name())
        .event(NegotiationResourceEvent.INDICATE_ACCESS_CONDITIONS.name())
        .target(NegotiationResourceState.ACCESS_CONDITIONS_INDICATED.name())
        .secured("isRepresentative")
        .and()
        .withExternal()
        .source(NegotiationResourceState.ACCESS_CONDITIONS_INDICATED.name())
        .event(NegotiationResourceEvent.DECLINE_ACCESS_CONDITIONS.name())
        .target(NegotiationResourceState.RESOURCE_NOT_MADE_AVAILABLE.name())
        .secured("isCreator")
        .and()
        .withExternal()
        .source(NegotiationResourceState.ACCESS_CONDITIONS_INDICATED.name())
        .event(NegotiationResourceEvent.ACCEPT_ACCESS_CONDITIONS.name())
        .target(NegotiationResourceState.ACCESS_CONDITIONS_MET.name())
        .secured("isCreator")
        .and()
        .withExternal()
        .source(NegotiationResourceState.ACCESS_CONDITIONS_MET.name())
        .event(NegotiationResourceEvent.GRANT_ACCESS_TO_RESOURCE.name())
        .target(NegotiationResourceState.RESOURCE_MADE_AVAILABLE.name())
        .secured("isRepresentative");

    transitions.withExternal().guard(negotiationIsApproved());
  }

  @Bean
  public Guard<String, String> negotiationIsApproved() {
    return new NegotiationIsApprovedGuard();
  }
}
