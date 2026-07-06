package eu.bbmri_eric.negotiator.lifecycle.statemachine;

@FunctionalInterface
public interface TransitionListener<C extends TransitionContext> {

  void onTransition(TransitionOutcome<C> outcome);
}
