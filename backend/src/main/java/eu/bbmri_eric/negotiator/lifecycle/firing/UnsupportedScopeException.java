package eu.bbmri_eric.negotiator.lifecycle.firing;

/**
 * Event Firing was asked to move a Negotiation's own Lifecycle, which this module does not yet
 * assemble a context for.
 *
 * <p><b>Refusing is the point.</b> A Negotiation-scope context needs every sibling Resource paired
 * with <em>its own</em> Compiled Graph, because terminality has to be asked of each Resource's own
 * pinned Definition Version — {@code TERMINAL_AGGREGATION} is meaningless without it, and a context
 * assembled with an empty sibling list would let that Guard pass by counting nothing. Answering
 * "permitted" off a half-built context is the one failure mode worse than not answering.
 *
 * <p>The half that is missing was left out deliberately rather than forgotten. It forces three
 * decisions the effort map files with other slabs, and taking them here would mean taking them
 * without their tickets: how a Resource's pin is written at all, given the column is {@code
 * updatable = false} and the link row already exists at Spawn; what an unresolvable definition does
 * to a Negotiation approval; and whether the pin columns get indexes. Resource scope needs none of
 * the three and still exercises the whole path end to end, which is why it landed first.
 *
 * <p>Deleting this class is how the slab that builds Negotiation scope announces itself.
 */
public class UnsupportedScopeException extends RuntimeException {

  UnsupportedScopeException(String message) {
    super(message);
  }
}
