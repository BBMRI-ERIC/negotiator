package eu.bbmri_eric.negotiator.lifecycle.statemachine;

/**
 * A named permission adapter evaluated by the executor for both listing and firing: "may
 * <em>this caller</em> fire <em>this transition</em>?" Return {@code false} to deny. A denied
 * transition is excluded from {@code permittedEvents} and raises {@link
 * TransitionDeniedException} on {@code fire}.
 */
public interface Guard<C extends TransitionContext> {
  boolean evaluate(C context);
}
