package eu.bbmri_eric.negotiator.lifecycle.resource;

import eu.bbmri_eric.negotiator.lifecycle.statemachine.BeanResolver;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.StateMachineDefinition;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.TransitionDescriptor;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.TransitionExecutor;
import eu.bbmri_eric.negotiator.negotiation.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.negotiation.NegotiationResourceState;
import java.util.List;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Definition of the Resource lifecycle state machine, and the factory built from it. */
@Configuration
public class ResourceStateMachineConfig {

  @Bean
  public StateMachineDefinition resourceStateMachineDefinition() {
    return new StateMachineDefinition(
        NegotiationResourceState.SUBMITTED.name(),
        List.of(
            transition(
                NegotiationResourceState.SUBMITTED,
                NegotiationResourceEvent.CONTACT,
                NegotiationResourceState.REPRESENTATIVE_CONTACTED,
                "isAdmin"),
            transition(
                NegotiationResourceState.SUBMITTED,
                NegotiationResourceEvent.MARK_AS_UNREACHABLE,
                NegotiationResourceState.REPRESENTATIVE_UNREACHABLE,
                "isAdmin"),
            transition(
                NegotiationResourceState.REPRESENTATIVE_UNREACHABLE,
                NegotiationResourceEvent.CONTACT,
                NegotiationResourceState.REPRESENTATIVE_CONTACTED,
                "isAdmin"),
            transition(
                NegotiationResourceState.REPRESENTATIVE_CONTACTED,
                NegotiationResourceEvent.MARK_AS_CHECKING_AVAILABILITY,
                NegotiationResourceState.CHECKING_AVAILABILITY,
                "isRepresentative"),
            transition(
                NegotiationResourceState.REPRESENTATIVE_CONTACTED,
                NegotiationResourceEvent.STEP_AWAY,
                NegotiationResourceState.RESOURCE_UNAVAILABLE,
                "isRepresentative"),
            transition(
                NegotiationResourceState.CHECKING_AVAILABILITY,
                NegotiationResourceEvent.MARK_AS_UNAVAILABLE,
                NegotiationResourceState.RESOURCE_UNAVAILABLE,
                "isRepresentative"),
            transition(
                NegotiationResourceState.CHECKING_AVAILABILITY,
                NegotiationResourceEvent.MARK_AS_CURRENTLY_UNAVAILABLE_BUT_WILLING_TO_COLLECT,
                NegotiationResourceState.RESOURCE_UNAVAILABLE_WILLING_TO_COLLECT,
                "isRepresentative"),
            transition(
                NegotiationResourceState.CHECKING_AVAILABILITY,
                NegotiationResourceEvent.MARK_AS_AVAILABLE,
                NegotiationResourceState.RESOURCE_AVAILABLE,
                "isRepresentative"),
            transition(
                NegotiationResourceState.RESOURCE_AVAILABLE,
                NegotiationResourceEvent.INDICATE_ACCESS_CONDITIONS,
                NegotiationResourceState.ACCESS_CONDITIONS_INDICATED,
                "isRepresentative"),
            transition(
                NegotiationResourceState.RESOURCE_UNAVAILABLE_WILLING_TO_COLLECT,
                NegotiationResourceEvent.INDICATE_ACCESS_CONDITIONS,
                NegotiationResourceState.ACCESS_CONDITIONS_INDICATED,
                "isRepresentative"),
            transition(
                NegotiationResourceState.ACCESS_CONDITIONS_INDICATED,
                NegotiationResourceEvent.DECLINE_ACCESS_CONDITIONS,
                NegotiationResourceState.RESOURCE_NOT_MADE_AVAILABLE,
                "isCreator"),
            transition(
                NegotiationResourceState.ACCESS_CONDITIONS_INDICATED,
                NegotiationResourceEvent.ACCEPT_ACCESS_CONDITIONS,
                NegotiationResourceState.ACCESS_CONDITIONS_MET,
                "isCreator"),
            transition(
                NegotiationResourceState.ACCESS_CONDITIONS_MET,
                NegotiationResourceEvent.GRANT_ACCESS_TO_RESOURCE,
                NegotiationResourceState.RESOURCE_MADE_AVAILABLE,
                "isRepresentative")));
  }

  private static TransitionDescriptor transition(
      NegotiationResourceState source,
      NegotiationResourceEvent event,
      NegotiationResourceState target,
      String securityAttribute) {
    return new TransitionDescriptor(
        source.name(), target.name(), event.name(), null, null, Set.of(securityAttribute));
  }

  @Bean
  public TransitionExecutor<ResourceTransitionContext> resourceTransitionExecutor(
      StateMachineDefinition resourceStateMachineDefinition,
      BeanResolver beanResolver,
      ResourcePersistTransitionListener resourcePersistTransitionListener) {
    return new TransitionExecutor<>(
        resourceStateMachineDefinition, beanResolver, resourcePersistTransitionListener);
  }
}
