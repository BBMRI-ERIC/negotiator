package eu.bbmri_eric.negotiator.lifecycle.statemachine;

import java.util.Set;

public record TransitionDescriptor(
    String sourceState,
    String targetState,
    String event,
    String actionName,
    String guardName,
    Set<String> securityAttributes) {

  public TransitionDescriptor(String sourceState, String targetState, String event) {
    this(sourceState, targetState, event, null, null, Set.of());
  }
}
