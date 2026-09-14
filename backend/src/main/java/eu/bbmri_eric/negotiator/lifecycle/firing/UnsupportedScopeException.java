package eu.bbmri_eric.negotiator.lifecycle.firing;

/**
 * Event Firing was asked to move a Negotiation's own Lifecycle, and it assembles no context for
 * one.
 *
 * <p>A Negotiation-scope context needs every sibling Resource paired with its own Compiled Graph.
 * With an empty sibling list, {@code TERMINAL_AGGREGATION} would pass by counting nothing, so
 * refusing is safer than judging a half-built context.
 */
public class UnsupportedScopeException extends RuntimeException {

  UnsupportedScopeException(String message) {
    super(message);
  }
}
