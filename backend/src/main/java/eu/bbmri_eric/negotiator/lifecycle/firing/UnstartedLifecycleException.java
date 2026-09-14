package eu.bbmri_eric.negotiator.lifecycle.firing;

/**
 * A Lifecycle was asked to move before it started: it has no Definition Version Pin, no State, or
 * neither.
 *
 * <p>This is not a refusal. With no graph and no State the Evaluation Pipeline cannot run, so
 * there is no Failure Category to report, and {@code NO_TRANSITION} would blame the caller's Event
 * for a problem in the row.
 *
 * <p>Spawn writes the pin and the State together, so either one missing means the Lifecycle never
 * started. The message says which is missing.
 */
public class UnstartedLifecycleException extends RuntimeException {

  UnstartedLifecycleException(String message) {
    super(message);
  }
}
