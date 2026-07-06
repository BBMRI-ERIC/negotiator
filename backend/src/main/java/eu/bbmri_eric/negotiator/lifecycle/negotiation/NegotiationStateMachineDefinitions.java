package eu.bbmri_eric.negotiator.lifecycle.negotiation;

import eu.bbmri_eric.negotiator.lifecycle.statemachine.StateMachineDefinition;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.TransitionDescriptor;
import eu.bbmri_eric.negotiator.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.NegotiationState;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class NegotiationStateMachineDefinitions {

  private NegotiationStateMachineDefinitions() {}

  public static StateMachineDefinition definition() {
    Set<String> states =
        Arrays.stream(NegotiationState.values()).map(Enum::name).collect(Collectors.toSet());

    List<TransitionDescriptor> transitions =
        List.of(
            new TransitionDescriptor(
                NegotiationState.DRAFT.name(),
                NegotiationState.SUBMITTED.name(),
                NegotiationEvent.SUBMIT.name(),
                "lifecycleEnablePublicPostsAction"),
            TransitionDescriptor.withSecurity(
                NegotiationState.SUBMITTED.name(),
                NegotiationState.IN_PROGRESS.name(),
                NegotiationEvent.APPROVE.name(),
                Set.of("ROLE_ADMIN"),
                "lifecycleEnablePrivatePostsAction"),
            TransitionDescriptor.withSecurity(
                NegotiationState.SUBMITTED.name(),
                NegotiationState.DECLINED.name(),
                NegotiationEvent.DECLINE.name(),
                Set.of("ROLE_ADMIN"),
                null),
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
                "lifecycleDisablePostsAction"),
            new TransitionDescriptor(
                NegotiationState.IN_PROGRESS.name(),
                NegotiationState.CONCLUDED.name(),
                NegotiationEvent.CONCLUDE.name()));

    return new StateMachineDefinition(states, transitions);
  }
}
