package eu.bbmri_eric.negotiator.lifecycle.statemachine;

/** Executed as part of a transition, before the state change is committed. */
public interface TransitionAction<C extends TransitionContext> {
  void execute(C context);
}
