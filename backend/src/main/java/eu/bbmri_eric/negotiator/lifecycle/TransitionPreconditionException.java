package eu.bbmri_eric.negotiator.lifecycle;

/**
 * Thrown when a lifecycle transition's business-rule precondition (e.g. an unmet information
 * requirement) is not satisfied. Replaces {@code
 * org.springframework.statemachine.StateMachineException} at the lifecycle-service seam.
 */
public class TransitionPreconditionException extends RuntimeException {

  public TransitionPreconditionException(String message) {
    super(message);
  }
}
