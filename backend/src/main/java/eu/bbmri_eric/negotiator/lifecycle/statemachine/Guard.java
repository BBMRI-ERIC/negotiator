package eu.bbmri_eric.negotiator.lifecycle.statemachine;

@FunctionalInterface
public interface Guard<C extends TransitionContext> {

  boolean isSatisfied(C context);
}
