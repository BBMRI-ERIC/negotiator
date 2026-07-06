package eu.bbmri_eric.negotiator.lifecycle.resource;

import eu.bbmri_eric.negotiator.lifecycle.statemachine.StateMachineDefinition;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.TransitionDescriptor;
import eu.bbmri_eric.negotiator.negotiation.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.negotiation.NegotiationResourceState;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ResourceStateMachineDefinitions {

  private ResourceStateMachineDefinitions() {}

  public static StateMachineDefinition definition() {
    Set<String> states =
        Arrays.stream(NegotiationResourceState.values())
            .map(Enum::name)
            .collect(Collectors.toSet());

    List<TransitionDescriptor> transitions =
        List.of(
            withSecurity(
                NegotiationResourceState.SUBMITTED,
                NegotiationResourceEvent.CONTACT,
                NegotiationResourceState.REPRESENTATIVE_CONTACTED,
                "isAdmin"),
            withSecurity(
                NegotiationResourceState.SUBMITTED,
                NegotiationResourceEvent.MARK_AS_UNREACHABLE,
                NegotiationResourceState.REPRESENTATIVE_UNREACHABLE,
                "isAdmin"),
            withSecurity(
                NegotiationResourceState.REPRESENTATIVE_UNREACHABLE,
                NegotiationResourceEvent.CONTACT,
                NegotiationResourceState.REPRESENTATIVE_CONTACTED,
                "isAdmin"),
            withSecurity(
                NegotiationResourceState.REPRESENTATIVE_CONTACTED,
                NegotiationResourceEvent.MARK_AS_CHECKING_AVAILABILITY,
                NegotiationResourceState.CHECKING_AVAILABILITY,
                "isRepresentative"),
            withSecurity(
                NegotiationResourceState.REPRESENTATIVE_CONTACTED,
                NegotiationResourceEvent.STEP_AWAY,
                NegotiationResourceState.RESOURCE_UNAVAILABLE,
                "isRepresentative"),
            withSecurity(
                NegotiationResourceState.CHECKING_AVAILABILITY,
                NegotiationResourceEvent.MARK_AS_UNAVAILABLE,
                NegotiationResourceState.RESOURCE_UNAVAILABLE,
                "isRepresentative"),
            withSecurity(
                NegotiationResourceState.CHECKING_AVAILABILITY,
                NegotiationResourceEvent.MARK_AS_CURRENTLY_UNAVAILABLE_BUT_WILLING_TO_COLLECT,
                NegotiationResourceState.RESOURCE_UNAVAILABLE_WILLING_TO_COLLECT,
                "isRepresentative"),
            withSecurity(
                NegotiationResourceState.CHECKING_AVAILABILITY,
                NegotiationResourceEvent.MARK_AS_AVAILABLE,
                NegotiationResourceState.RESOURCE_AVAILABLE,
                "isRepresentative"),
            withSecurity(
                NegotiationResourceState.RESOURCE_AVAILABLE,
                NegotiationResourceEvent.INDICATE_ACCESS_CONDITIONS,
                NegotiationResourceState.ACCESS_CONDITIONS_INDICATED,
                "isRepresentative"),
            withSecurity(
                NegotiationResourceState.RESOURCE_UNAVAILABLE_WILLING_TO_COLLECT,
                NegotiationResourceEvent.INDICATE_ACCESS_CONDITIONS,
                NegotiationResourceState.ACCESS_CONDITIONS_INDICATED,
                "isRepresentative"),
            withSecurity(
                NegotiationResourceState.ACCESS_CONDITIONS_INDICATED,
                NegotiationResourceEvent.DECLINE_ACCESS_CONDITIONS,
                NegotiationResourceState.RESOURCE_NOT_MADE_AVAILABLE,
                "isCreator"),
            withSecurity(
                NegotiationResourceState.ACCESS_CONDITIONS_INDICATED,
                NegotiationResourceEvent.ACCEPT_ACCESS_CONDITIONS,
                NegotiationResourceState.ACCESS_CONDITIONS_MET,
                "isCreator"),
            withSecurity(
                NegotiationResourceState.ACCESS_CONDITIONS_MET,
                NegotiationResourceEvent.GRANT_ACCESS_TO_RESOURCE,
                NegotiationResourceState.RESOURCE_MADE_AVAILABLE,
                "isRepresentative"));

    return new StateMachineDefinition(states, transitions);
  }

  private static TransitionDescriptor withSecurity(
      NegotiationResourceState source,
      NegotiationResourceEvent event,
      NegotiationResourceState target,
      String securityAttribute) {
    return TransitionDescriptor.withSecurity(
        source.name(), target.name(), event.name(), Set.of(securityAttribute), null);
  }
}
