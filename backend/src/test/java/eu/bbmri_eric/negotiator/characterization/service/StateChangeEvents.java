package eu.bbmri_eric.negotiator.characterization.service;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationStateChangeEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.ResourceStateChangeEvent;
import java.util.List;
import org.springframework.test.context.event.ApplicationEvents;

/**
 * The suite's single reader of the two state change application events - the seam every
 * notification handler, the conclusion listener and the webhook subsystem watch.
 *
 * <p><b>Why one reader.</b> Both events carried their origin State, destination State and
 * triggering Event as enum constants when this file was written, and ADR 0002 deletes those enums.
 * An assertion that named the enum could not be re-run after the cutover, so every assertion
 * downstream of this file speaks only of names. The seam now deals in names itself, so the reading
 * is a straight copy - and not one assertion downstream had to change when it stopped being a
 * conversion. It is the same argument, and the same one cutover point, that {@link
 * eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapter} makes for the services
 * and {@link LifecycleHistory} makes for the two Record tables.
 *
 * <p><b>What a recorded event is evidence of.</b> A Negotiation state change is always the trace of
 * a Negotiation Transition: one producer, the persist listener. A Resource state change is
 * <em>not</em> - it has two producers, and the second writes an arbitrary State onto the link row
 * with no Transition behind it and stamps the payload {@code "OVERRIDE"}. Every assertion here
 * therefore says which producer it is about; see {@link
 * eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapter#overrideResourceStates}.
 *
 * <p><b>Recording, not delivery.</b> Handlers are never observed through SMTP anywhere in this
 * suite. Their trigger is read here, off the recorded application events, and their effect is read
 * off the rows they write - see {@link HandlerNotifications}.
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
