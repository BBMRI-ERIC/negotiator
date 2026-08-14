package eu.bbmri_eric.negotiator.characterization.adapter;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The single surface through which the characterization suite reaches the Lifecycle services.
 *
 * <p>States and Events are named as plain strings ({@code "SUBMITTED"}, {@code "APPROVE"}) on every
 * parameter and every return value. Nothing here names a Java type that the Lifecycle redesign
 * deletes, so the assertions written against this adapter can be re-run unchanged against the new
 * subsystem: only the implementation behind this interface is rewritten.
 *
 * <p>The adapter is deliberately thin. It converts strings, delegates, and converts back. It adds
 * no behaviour of its own and it hides no behaviour of the services it wraps - in particular the
 * two services refuse a forbidden Event differently (a Negotiation raises {@code
 * ForbiddenRequestException}, a Resource silently returns its unchanged current State) and both
 * refusals stay observable through here.
 */
public interface LifecycleTestAdapter {

  /**
   * The Events currently offered for a Negotiation, as State/Event names.
   *
   * @param negotiationId of the Negotiation
   * @return the offered Event names, empty when the caller may fire nothing
   * @throws eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException as the service does
   */
  Set<String> possibleNegotiationEvents(String negotiationId);

  /**
   * Fires an Event at a Negotiation.
   *
   * @param negotiationId of the Negotiation
   * @param event the Event name, e.g. {@code "APPROVE"}
   * @return the State name the service reports afterwards
   * @throws eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException when the Event is
   *     not offered - the refusal behaviour of the Negotiation service, passed through unchanged
   * @throws IllegalArgumentException when {@code event} is not a known Event name
   */
  String sendNegotiationEvent(String negotiationId, String event);

  /**
   * Fires an Event at a Negotiation together with a message.
   *
   * @param message the reason given, which today becomes a post
   * @return the State name the service reports afterwards
   * @see #sendNegotiationEvent(String, String)
   */
  String sendNegotiationEvent(String negotiationId, String event, String message);

  /**
   * The State a Negotiation is currently in.
   *
   * @return the State name
   * @throws eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException when no such
   *     Negotiation exists
   */
  String currentNegotiationState(String negotiationId);

  /**
   * The Events currently offered for one Resource of a Negotiation, as Event names.
   *
   * @return the offered Event names, empty when the caller may fire nothing
   */
  Set<String> possibleResourceEvents(String negotiationId, String resourceId);

  /**
   * Fires an Event at one Resource of a Negotiation.
   *
   * <p>Note the asymmetry with {@link #sendNegotiationEvent(String, String)}: when the Event is not
   * offered this call raises nothing and returns the unchanged current State.
   *
   * @param event the Event name, e.g. {@code "CONTACT"}
   * @return the State name the service reports afterwards
   * @throws IllegalArgumentException when {@code event} is not a known Event name
   */
  String sendResourceEvent(String negotiationId, String resourceId, String event);

  /**
   * The State one Resource of a Negotiation is currently in.
   *
   * @return the State name, or {@code null} when the Resource has no recorded State yet
   */
  String currentResourceState(String negotiationId, String resourceId);

  /**
   * Writes a State straight onto the link rows of nominated Resources, through the Resource
   * governance service rather than through the Lifecycle.
   *
   * <p><b>This is the second producer of a Resource state change, and it is not a Transition.</b>
   * It consults no Definition graph, no Required Authority rule of that graph and no parent-State
   * gate; it takes any State name and writes it. It is on the adapter all the same because it
   * publishes the same application event as a Transition does - stamped {@code "OVERRIDE"} - and
   * therefore reaches the same listeners and the same notification handlers. A suite that pinned
   * only the Lifecycle producer would leave half of that seam's traffic unrecorded.
   *
   * @param resourceRowIds the Resources' database row ids, which is the identifier this surface
   *     takes - unlike every Lifecycle call, which keys on the Resource's source id
   * @param state the State name to write, e.g. {@code "RESOURCE_MADE_AVAILABLE"}
   * @return every Resource of the Negotiation afterwards, its State name keyed by its source id
   * @throws eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException when the caller is
   *     neither an administrator nor a representative of every named Resource
   * @throws IllegalArgumentException when {@code state} is not a known State name
   */
  Map<String, String> overrideResourceStates(
      String negotiationId, List<Long> resourceRowIds, String state);

  /**
   * The nested Lifecycle diagram the Resource service derives from its Definition graph.
   *
   * @return the diagram exactly as the service builds it, keyed by State and Event names
   */
  Map<String, Object> resourceLifecycleDiagram();
}
