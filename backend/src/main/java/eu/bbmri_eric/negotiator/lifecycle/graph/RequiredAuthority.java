package eu.bbmri_eric.negotiator.lifecycle.graph;

/**
 * Who may fire a Transition. Deliberately a field of the Transition rather than a Guard: asking
 * <em>who is firing</em> and asking <em>whether the move is currently legal</em> fail differently,
 * so they must not be expressible as the same kind of row.
 *
 * <p>Single-valued. Six of the eight Negotiation Transitions are behaviourally {@code IS_ADMIN OR
 * IS_CREATOR} and no value here reproduces that; resolving that is a decision of its own, and
 * inventing a disjunction to get ahead of it is not this vocabulary's call.
 *
 * <p>It lives in the graph package rather than beside the {@code transition} table's entity because
 * it is vocabulary rather than schema: the compiled graph an evaluator reads carries a Required
 * Authority on every edge and cannot name one without this type. The persistence detail — that it
 * is stored {@code @Enumerated(STRING)} against a {@code VARCHAR} with a mirroring CHECK constraint
 * — stays with the entity, which is the only thing that needs to know it.
 */
public enum RequiredAuthority {

  /** No authority is required of the caller. */
  NONE,

  /**
   * Collapses today's two spellings — {@code ROLE_ADMIN} on the Negotiation machine and {@code
   * isAdmin} on the Resource one — which check the same granted authority.
   */
  IS_ADMIN,

  IS_CREATOR,

  IS_REPRESENTATIVE,

  /** Marks a System Event, which no human caller can ever satisfy. */
  SYSTEM
}
