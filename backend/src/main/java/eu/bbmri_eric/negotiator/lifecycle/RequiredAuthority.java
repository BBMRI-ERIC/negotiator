package eu.bbmri_eric.negotiator.lifecycle;

/**
 * Who may fire a Transition. Deliberately a field of the Transition rather than a Guard: asking
 * <em>who is firing</em> and asking <em>whether the move is currently legal</em> fail differently,
 * so they must not be expressible as the same kind of row.
 *
 * <p>Single-valued. Six of the eight Negotiation Transitions are behaviourally {@code IS_ADMIN OR
 * IS_CREATOR} and no value here reproduces that; resolving that is a decision of its own, and
 * inventing a disjunction to get ahead of it is not this schema's call.
 *
 * <p>It lives here beside the Well-known name holders for the reason {@link DefinitionScope} gives,
 * and needs it more: whoever decides whether a caller may fire a move has to be able to name these
 * five answers, and must not thereby be able to name a table.
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
