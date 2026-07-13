package eu.bbmri_eric.negotiator.lifecycle.statemachine;

/**
 * Thrown when a guard (machine or transition) denies the caller permission to fire a transition.
 * Carries the state and event so the message is generic and built from their names. Maps to HTTP
 * 403.
 */
public class TransitionDeniedException extends RuntimeException {

  public TransitionDeniedException(String currentState, String event) {
    super(message(currentState, event));
  }

  private static String message(String currentState, String event) {
    return "Transition '%s' from state '%s' is not permitted for the current caller."
        .formatted(event, currentState);
  }
}
