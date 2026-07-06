package eu.bbmri_eric.negotiator.lifecycle.statemachine;

@FunctionalInterface
public interface TransitionAction<C extends TransitionContext> {

  void execute(C context);
}
