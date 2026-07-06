package eu.bbmri_eric.negotiator.lifecycle.statemachine;

/**
 * Evaluated before a guarded transition is permitted. Return {@code false} to silently deny; throw
 * {@link TransitionPreconditionException} to deny loudly with a user-facing message.
 */
public interface Guard<C extends TransitionContext> {
  boolean evaluate(C context);
}
