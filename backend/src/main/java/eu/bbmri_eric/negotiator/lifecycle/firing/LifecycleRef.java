package eu.bbmri_eric.negotiator.lifecycle.firing;

import lombok.NonNull;

/**
 * Which Lifecycle an Event is being fired at, as the ids a caller already holds.
 *
 * <p>This is the <em>reference</em>, not the Lifecycle: a Negotiation id, and for a Resource the
 * Resource as well. What State the Lifecycle is in, who created it and who represents it are read
 * from the rows these ids name — that reading is the whole of what this module adds, and a caller
 * that had to supply any of it would be assembling the evaluation context itself, which is the
 * duplication ADR 0001 exists to prevent.
 *
 * <p><b>Sealed, with one case per Definition Scope.</b> The two Scopes take different ids and build
 * different contexts — a Resource-scope evaluation has a parent Negotiation and no siblings, a
 * Negotiation-scope one has siblings and no parent — and naming them as two records rather than as
 * one record with a nullable field means the assembly switches over a closed set and the compiler
 * refuses a forgotten case.
 *
 * <p><b>The Resource is named by its {@code source_id}, never by its row id.</b> That is what every
 * Lifecycle path in this backend keys on — the State lookup, the representative check and the
 * persist listener alike — so a reference built from a row id finds nothing and is reported as an
 * unknown Resource rather than as the mistake it is.
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
