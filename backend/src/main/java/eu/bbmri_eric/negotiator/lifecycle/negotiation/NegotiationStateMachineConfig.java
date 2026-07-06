package eu.bbmri_eric.negotiator.lifecycle.negotiation;

import eu.bbmri_eric.negotiator.lifecycle.statemachine.BeanResolver;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.StateMachineDefinition;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.StateMachineFactory;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.TransitionDescriptor;
import eu.bbmri_eric.negotiator.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.NegotiationState;
import java.util.List;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Definition of the Negotiation lifecycle state machine, and the factory built from it. */
@Configuration
public class NegotiationStateMachineConfig {

  @Bean
  public StateMachineDefinition negotiationStateMachineDefinition() {
    return new StateMachineDefinition(
        NegotiationState.DRAFT.name(),
        List.of(
            new TransitionDescriptor(
                NegotiationState.DRAFT.name(),
                NegotiationState.SUBMITTED.name(),
                NegotiationEvent.SUBMIT.name(),
                "enablePublicPosts",
                null,
                Set.of()),
            new TransitionDescriptor(
                NegotiationState.SUBMITTED.name(),
                NegotiationState.IN_PROGRESS.name(),
                NegotiationEvent.APPROVE.name(),
                "enablePrivatePosts",
                null,
                Set.of("ROLE_ADMIN")),
            new TransitionDescriptor(
                NegotiationState.SUBMITTED.name(),
                NegotiationState.DECLINED.name(),
                NegotiationEvent.DECLINE.name(),
                null,
                null,
                Set.of("ROLE_ADMIN")),
            new TransitionDescriptor(
                NegotiationState.IN_PROGRESS.name(),
                NegotiationState.PAUSED.name(),
                NegotiationEvent.PAUSE.name()),
            new TransitionDescriptor(
                NegotiationState.PAUSED.name(),
                NegotiationState.IN_PROGRESS.name(),
                NegotiationEvent.UNPAUSE.name()),
            new TransitionDescriptor(
                NegotiationState.PAUSED.name(),
                NegotiationState.ABANDONED.name(),
                NegotiationEvent.ABANDON.name()),
            new TransitionDescriptor(
                NegotiationState.IN_PROGRESS.name(),
                NegotiationState.ABANDONED.name(),
                NegotiationEvent.ABANDON.name(),
                "disablePosts",
                null,
                Set.of()),
            new TransitionDescriptor(
                NegotiationState.IN_PROGRESS.name(),
                NegotiationState.CONCLUDED.name(),
                NegotiationEvent.CONCLUDE.name())));
  }

  @Bean
  public StateMachineFactory<NegotiationTransitionContext> negotiationStateMachineFactory(
      StateMachineDefinition negotiationStateMachineDefinition,
      BeanResolver beanResolver,
      NegotiationPersistTransitionListener negotiationPersistTransitionListener) {
    return new StateMachineFactory<>(
        negotiationStateMachineDefinition, beanResolver, negotiationPersistTransitionListener);
  }
}
