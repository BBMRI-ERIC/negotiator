package eu.bbmri_eric.negotiator.lifecycle.statemachine;

/**
 * Thrown when an event is not a valid transition from the current state: either the event is
 * undefined in the Transition Table for that state, or a guard denied the transition. Signals a
 * transition-validity failure, distinct from a business failure. When a guard denies the
 * transition, the underlying library exception is preserved as the cause.
 */
public class InvalidTransitionException extends RuntimeException {

  public InvalidTransitionException(String currentState, String event) {
    super(message(currentState, event));
  }

  public InvalidTransitionException(String currentState, String event, Throwable cause) {
    super(message(currentState, event), cause);
  }

  private static String message(String currentState, String event) {
    return "Event '%s' is not a valid transition from state '%s'.".formatted(event, currentState);
  }
}
