package eu.bbmri_eric.negotiator.lifecycle;

/** Thrown when a transition's business-rule preconditions are not met. Maps to HTTP 400. */
public class TransitionPreconditionException extends RuntimeException {
  public TransitionPreconditionException(String message) {
    super(message);
  }
}
