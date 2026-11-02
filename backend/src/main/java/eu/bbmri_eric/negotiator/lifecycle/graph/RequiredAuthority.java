package eu.bbmri_eric.negotiator.lifecycle.graph;

/**
 * Who may fire a Transition. It is a field of the Transition and not a Guard, because a wrong
 * caller and a move the domain forbids fail in different categories.
 *
 * <p>A Transition carries a set of these, and a caller holding any one value may fire it. An empty
 * set admits any person. {@code SYSTEM} stands alone, so a System Event is one whose set is {@code
 * {SYSTEM}}.
 */
public enum RequiredAuthority {

  /** The caller holds the admin authority. */
  IS_ADMIN,

  IS_CREATOR,

  IS_REPRESENTATIVE,

  /** The Orchestration Trigger's authority. No person holds it. */
  SYSTEM
}
