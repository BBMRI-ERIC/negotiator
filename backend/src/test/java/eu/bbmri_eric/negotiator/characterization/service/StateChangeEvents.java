package eu.bbmri_eric.negotiator.characterization.service;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationStateChangeEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.ResourceStateChangeEvent;
import java.util.List;
import org.springframework.test.context.event.ApplicationEvents;

/**
 * The suite's single reader of the two state change application events - the seam every
 * notification handler, the conclusion listener and the webhook subsystem watch.
 *
 * <p><b>Why one reader.</b> Both events carry their origin State, destination State and triggering
 * Event as enum constants today, and ADR 0002 deletes those enums. An assertion that named the enum
 * could not be re-run after the cutover, so this file converts each payload into strings once and
 * every assertion downstream of it speaks only of names. It is the same argument, and the same one
 * cutover point, that {@link
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
                    nameOf(event.getFromState()),
                    nameOf(event.getToState()),
                    nameOf(event.getEvent())))
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
                    nameOf(event.getFromState()),
                    nameOf(event.getToState()),
                    nameOf(event.getEvent())))
        .toList();
  }

  /**
   * The name of a State or an Event as the payload carries it.
   *
   * <p>Declared over {@link Enum} rather than over the four Lifecycle types on purpose: it is what
   * lets this file read today's payloads without naming a type the redesign deletes. {@code null}
   * is a real value on both sides - a Resource with no recorded State yet has no origin, and a
   * Transition whose trigger cannot be resolved carries no Event - so it is preserved rather than
   * turned into a placeholder.
   */
  private static String nameOf(Enum<?> value) {
    return value == null ? null : value.name();
  }

  private StateChangeEvents() {}
}
