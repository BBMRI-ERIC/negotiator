package eu.bbmri_eric.negotiator.lifecycle.negotiation;

import eu.bbmri_eric.negotiator.lifecycle.statemachine.BeanResolver;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.StateMachineDefinition;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.TransitionDescriptor;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.TransitionExecutor;
import eu.bbmri_eric.negotiator.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.NegotiationState;
import java.util.List;
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
                null),
            new TransitionDescriptor(
                NegotiationState.SUBMITTED.name(),
                NegotiationState.IN_PROGRESS.name(),
                NegotiationEvent.APPROVE.name(),
                "enablePrivatePosts",
                "isAdmin"),
            new TransitionDescriptor(
                NegotiationState.SUBMITTED.name(),
                NegotiationState.DECLINED.name(),
                NegotiationEvent.DECLINE.name(),
                null,
                "isAdmin"),
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
                null),
            new TransitionDescriptor(
                NegotiationState.IN_PROGRESS.name(),
                NegotiationState.CONCLUDED.name(),
                NegotiationEvent.CONCLUDE.name())),
        "isCreatorOrAdmin",
        null);
  }

  @Bean
  public TransitionExecutor<NegotiationTransitionContext> negotiationTransitionExecutor(
      StateMachineDefinition negotiationStateMachineDefinition,
      BeanResolver beanResolver,
      NegotiationPersistTransitionListener negotiationPersistTransitionListener) {
    return new TransitionExecutor<>(
        negotiationStateMachineDefinition, beanResolver, negotiationPersistTransitionListener);
  }
}
