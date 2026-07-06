package eu.bbmri_eric.negotiator.lifecycle.statemachine;

import java.util.List;

public record StateMachineDefinition(String initialState, List<TransitionDescriptor> transitions) {

  public List<TransitionDescriptor> transitionsFrom(String state) {
    return transitions.stream().filter(t -> t.sourceState().equals(state)).toList();
  }
}
