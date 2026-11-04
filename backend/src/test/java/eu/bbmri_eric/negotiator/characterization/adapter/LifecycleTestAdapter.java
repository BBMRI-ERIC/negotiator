package eu.bbmri_eric.negotiator.characterization.adapter;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The only way the characterization suite reaches the Lifecycle services.
 *
 * <p>Every parameter and return value names States and Events as plain strings, so no assertion
 * depends on a Java type a redesign might delete; only the implementation behind this interface has
 * to change.
 *
 * <p>The adapter converts strings, delegates, and converts back, and hides no behaviour of the
 * services. Both services refuse by throwing, {@code ForbiddenRequestException} or {@code
 * LifecycleConflictException}, and the adapter passes the exception through.
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
   * @throws eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException when the caller
   *     may not fire the Event, passed through from the Negotiation service
   * @throws eu.bbmri_eric.negotiator.common.exceptions.LifecycleConflictException when no
   *     Transition leaves the current State on the Event, passed through likewise
   * @throws IllegalArgumentException when {@code event} is not a known Event name
   */
  String sendNegotiationEvent(String negotiationId, String event);

  /**
   * Fires an Event at a Negotiation together with a message.
   *
   * @param message the reason given, which becomes a post
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
   * <p>Like {@link #sendNegotiationEvent(String, String)}, an Event that is not offered throws.
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
   * Writes a State straight onto the link rows of the given Resources, through the Resource
   * governance service rather than the Lifecycle.
   *
   * <p>This is an Override, not a Transition: it checks no graph, no Required Authority and no
   * parent State. It belongs on the adapter because it publishes the same application event as a
   * Transition, with Event {@code "OVERRIDE"}, and so reaches the same listeners and notification
   * handlers.
   *
   * @param resourceRowIds the Resources' database row ids, unlike every Lifecycle call, which takes
   *     the Resource's source id
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
