package eu.bbmri_eric.negotiator.characterization.service;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationStateChangeEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.ResourceStateChangeEvent;
import java.util.List;
import org.springframework.test.context.event.ApplicationEvents;

/**
 * Reads the two state change application events, which every notification handler, the conclusion
 * listener and the webhook subsystem listen to.
 *
 * <p>Tests downstream of this class speak only of State and Event names, so a change in how the
 * events carry them changes this file and no assertion, as {@link
 * eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapter} does for the services and
 * {@link LifecycleHistory} for the two Record tables.
 *
 * <p>A Negotiation state change always comes from a Negotiation Transition, through the persist
 * listener. A Resource state change has a second producer, the Override, which writes any State
 * onto the link row with no Transition behind it and sets the Event to {@code "OVERRIDE"}. So every
 * assertion says which producer it is about; see {@link
 * eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapter#overrideResourceStates}.
 *
 * <p>No test observes handlers through SMTP. Their trigger is read here and their effect from the
 * rows they write; see {@link HandlerNotifications}.
 */
final class StateChangeEvents {

  /** One published Negotiation state change, in names rather than in constants. */
  record NegotiationStateChange(
      String negotiationId, String fromState, String toState, String event) {}

  /** One published Resource state change, in names rather than in constants. */
  record ResourceStateChange(
      String negotiationId, String resourceId, String fromState, String toState, String event) {}

  /** Every Negotiation state change recorded so far, in publication order. */
  static List<NegotiationStateChange> negotiationChanges(ApplicationEvents events) {
    return events.stream(NegotiationStateChangeEvent.class)
        .map(
            event ->
                new NegotiationStateChange(
                    event.getNegotiationId(),
                    event.getFromState(),
                    event.getToState(),
                    event.getEvent()))
        .toList();
  }

  /** Every Resource state change recorded so far, in publication order, from both producers. */
  static List<ResourceStateChange> resourceChanges(ApplicationEvents events) {
    return events.stream(ResourceStateChangeEvent.class)
        .map(
            event ->
                new ResourceStateChange(
                    event.getNegotiationId(),
                    event.getResourceId(),
                    event.getFromState(),
                    event.getToState(),
                    event.getEvent()))
        .toList();
  }

  private StateChangeEvents() {}
}
