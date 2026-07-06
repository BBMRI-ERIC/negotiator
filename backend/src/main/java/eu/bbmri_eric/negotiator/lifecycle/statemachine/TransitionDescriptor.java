package eu.bbmri_eric.negotiator.lifecycle.statemachine;

import java.util.Set;

public record TransitionDescriptor(
    String sourceState,
    String targetState,
    String event,
    String guardName,
    String actionName,
    Set<String> securityAttributes) {

  public TransitionDescriptor {
    securityAttributes = securityAttributes == null ? Set.of() : Set.copyOf(securityAttributes);
  }

  public TransitionDescriptor(String sourceState, String targetState, String event) {
    this(sourceState, targetState, event, null, null, Set.of());
  }

  public TransitionDescriptor(
      String sourceState, String targetState, String event, String actionName) {
    this(sourceState, targetState, event, null, actionName, Set.of());
  }

  public TransitionDescriptor(
      String sourceState, String targetState, String event, String guardName, String actionName) {
    this(sourceState, targetState, event, guardName, actionName, Set.of());
  }

  public static TransitionDescriptor withSecurity(
      String sourceState,
      String targetState,
      String event,
      Set<String> securityAttributes,
      String actionName) {
    return new TransitionDescriptor(
        sourceState, targetState, event, null, actionName, securityAttributes);
  }
}
