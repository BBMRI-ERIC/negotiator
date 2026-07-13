package eu.bbmri_eric.negotiator.lifecycle.statemachine;

/**
 * Thrown when an event is undefined from the current state in the Transition Table. Maps to HTTP
 * 403. The caller's UI is stale or the wrong state was targeted.
 */
public class InvalidTransitionException extends RuntimeException {

  public InvalidTransitionException(String currentState, String event) {
    super(message(currentState, event));
  }

  private static String message(String currentState, String event) {
    return "Event '%s' is not a valid transition from state '%s'.".formatted(event, currentState);
  }
}
