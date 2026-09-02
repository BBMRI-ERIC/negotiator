package eu.bbmri_eric.negotiator.lifecycle.graph;

/**
 * Why a move was refused, in the order the Evaluation Pipeline checks. The cheapest and most common
 * refusal comes first, and the category a caller sees does not depend on where a Guard is wired.
 *
 * <p>The HTTP layer maps a category to a status, so this package stays free of web concerns.
 */
public enum FailureCategory {

  /**
   * The graph has no Transition for that Event from that State. It comes before the other
   * categories because it is a fact about the definition, not about the caller or the domain. It
   * stays separate from {@code DOMAIN_STATE_CONFLICT} so the HTTP layer can answer it differently.
   */
  NO_TRANSITION,

  /** The caller holds none of the Transition's Required Authority. */
  AUTHORIZATION,

  /** An Information Requirement on the fired Event is not satisfied. */
  UNMET_REQUIREMENT,

  /** A Guard does not permit the move in the current domain state. */
  DOMAIN_STATE_CONFLICT
}
