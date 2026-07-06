package eu.bbmri_eric.negotiator.lifecycle.statemachine;

/** Invoked synchronously after every transition, once the state change has been committed. */
public interface TransitionListener<C extends TransitionContext> {
  void onTransition(TransitionOutcome<C> outcome);
}
