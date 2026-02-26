package eu.bbmri_eric.negotiator.negotiation.state_machine.resource;

import io.micrometer.common.lang.NonNull;
import java.util.Optional;
import lombok.Getter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.statemachine.trigger.Trigger;

@Getter
public enum NegotiationResourceEvent {
  CONTACT("Contact", "Contact representatives"),
  MARK_AS_UNREACHABLE("Mark as Unreachable", "Mark representatives as unreachable"),
  RETURN_FOR_RESUBMISSION("Return for Resubmission", "Ask for further clarification"),
  MARK_AS_CHECKING_AVAILABILITY("Mark as Checking Availability", "Mark as checking availability"),
  MARK_AS_AVAILABLE("Mark as Available", "Mark the resource as available for access"),
  MARK_AS_UNAVAILABLE("Mark as Unavailable", "Mark the resource as unavailable for access"),
  MARK_AS_CURRENTLY_UNAVAILABLE_BUT_WILLING_TO_COLLECT(
      "Mark as Currently Unavailable, But Willing to Collect",
      "Mark the resource as currently unavailable, but willing to collect"),
  STEP_AWAY("Step Away", "Step away. Not interested in collaboration"),
  INDICATE_ACCESS_CONDITIONS(
      "Indicate Access Conditions", "Indicate access conditions for the resource"),
  ACCEPT_ACCESS_CONDITIONS("Accept Access Conditions", "Accept access conditions for the resource"),
  DECLINE_ACCESS_CONDITIONS(
      "Decline Access Conditions", "Decline access conditions for the resource"),
  GRANT_ACCESS_TO_RESOURCE("Grant Access to Resource", "Grant access to the resource"),
  OVERRIDE("Override current state", "Override current state, ignoring state machine guards");

  private final String label;
  private final String description;

  NegotiationResourceEvent(String label, String description) {
    this.label = label;
    this.description = description;
  }

  public String getValue() {
    return this.name();
  }

  public static NegotiationResourceEvent fromTransition(
      @NonNull Transition<String, String> transition) {
    return fromTransitionOptional(transition)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Could not get event for transition from %s to %s"
                        .formatted(transition.getSource(), transition.getTarget())));
  }

  private static Optional<NegotiationResourceEvent> fromTransitionOptional(
      Transition<String, String> transition) {
    return Optional.ofNullable(transition)
        .map(Transition::getTrigger)
        .map(Trigger::getEvent)
        .map(NegotiationResourceEvent::valueOf);
  }
}
