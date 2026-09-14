package eu.bbmri_eric.negotiator.lifecycle.firing;

import lombok.NonNull;

/**
 * Which Lifecycle an Event is fired at, as the ids a caller already holds. Event Firing reads the
 * State, creator and representatives from the rows these ids name, so no caller assembles the
 * evaluation context.
 *
 * <p>There is one case per Definition Scope, so the assembly switches over a closed set and the
 * compiler rejects a forgotten case.
 *
 * <p>A Resource is named by its {@code source_id}, never its row id, because every Lifecycle lookup
 * keys on it. A row id finds nothing and is reported as an unknown Resource.
 */
public sealed interface LifecycleRef {

  /** The Negotiation whose Lifecycle this is, or whose Resource's Lifecycle this is. */
  String negotiationId();

  /** A Negotiation's own Lifecycle. */
  record NegotiationLifecycle(@NonNull String negotiationId) implements LifecycleRef {}

  /**
   * One Resource's Lifecycle within a Negotiation.
   *
   * @param resourceId the Resource's {@code source_id}, not its row id
   */
  record ResourceLifecycle(@NonNull String negotiationId, @NonNull String resourceId)
      implements LifecycleRef {}

  static LifecycleRef negotiation(String negotiationId) {
    return new NegotiationLifecycle(negotiationId);
  }

  /**
   * @param resourceId the Resource's {@code source_id}, not its row id
   */
  static LifecycleRef resource(String negotiationId, String resourceId) {
    return new ResourceLifecycle(negotiationId, resourceId);
  }
}
