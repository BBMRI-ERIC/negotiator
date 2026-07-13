package eu.bbmri_eric.negotiator.lifecycle.statemachine;

/**
 * The loud, firing-only counterpart of a {@link Guard}: "not yet — do X first."
 *
 * <p>{@code check(context, event)} throws {@link TransitionPreconditionException} with a
 * user-actionable message to refuse the firing. Preconditions are evaluated only on {@code fire},
 * never in listing, so an event with an unmet precondition stays discoverable in {@code
 * permittedEvents}.
 */
public interface Precondition<C extends TransitionContext> {
  void check(C context, String event);
}
