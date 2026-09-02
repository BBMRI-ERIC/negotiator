package eu.bbmri_eric.negotiator.lifecycle.graph;

/**
 * Why a move was refused, in the order the Evaluation Pipeline can produce them. ADR 0005 fixes the
 * order by expected failure likelihood so that the cheapest and most common rejection comes first,
 * and the categories stay monotonic — the category a caller sees never flip-flops depending on
 * where a Guard happens to be wired.
 *
 * <p>The HTTP shapes ADR 0005 names — 403, 422, 409 — are deliberately <em>not</em> here. Mapping a
 * category to a status is the controller's, and putting it on this enum would make the graph
 * package know about a web layer it otherwise never mentions.
 */
public enum FailureCategory {

  /**
   * The graph offers no Transition for that Event from that State, so there was nothing to gate.
   * This is not one of ADR 0005's three; it sits ahead of all of them, because it is a fact about
   * the definition rather than about the caller or the domain.
   *
   * <p>Its outward shape is an open question the cutover slab owns. Today the two services
   * disagree: the Negotiation one raises {@code ForbiddenRequestException} and the Resource one
   * returns the unchanged State and raises nothing, so an unfireable Event is indistinguishable
   * from one that fired and led back where it started. Keeping this separate from {@code
   * DOMAIN_STATE_CONFLICT} is what leaves that decision open instead of taking it here.
   */
  NO_TRANSITION,

  /** The caller does not hold the Transition's Required Authority. ADR 0005's first stage; 403. */
  AUTHORIZATION,

  /** An Information Requirement on the firing Event is unsatisfied. The second stage; 422. */
  UNMET_REQUIREMENT,

  /** A Guard says the domain does not currently permit the move. The third stage; 409. */
  DOMAIN_STATE_CONFLICT
}
