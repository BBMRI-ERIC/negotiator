package eu.bbmri_eric.negotiator.lifecycle.statemachine;

public record TransitionDescriptor(
    String sourceState, String targetState, String event, String actionName, String guardName) {

  public TransitionDescriptor(String sourceState, String targetState, String event) {
    this(sourceState, targetState, event, null, null);
  }
}
