package eu.bbmri_eric.negotiator.lifecycle.statemachine;

/**
 * The persistence step of a transition. Invoked synchronously by the {@link TransitionExecutor}
 * after the guard and transition action, while the caller's transaction is still open: it persists
 * the state change (pending commit) and runs before {@code fire} returns its outcome. A failure
 * here propagates out of {@code fire} and rolls back the surrounding transaction.
 */
public interface TransitionListener<C extends TransitionContext> {
  void onTransition(TransitionOutcome<C> outcome);
}
