package eu.bbmri_eric.negotiator.lifecycle.statemachine;

/**
 * Thrown when a transition's preconditions are not met. Maps to HTTP 400. The event remains listed
 * in {@code permittedEvents} so the caller knows the action is available once the step is completed.
 */
public class TransitionPreconditionException extends RuntimeException {
  public TransitionPreconditionException(String message) {
    super(message);
  }
}
