package eu.bbmri_eric.negotiator.lifecycle.statemachine;

import java.util.List;

/**
 * The declarative Transition Table: the list of {@link TransitionDescriptor}s plus the machine-level
 * slots (optional {@code machineGuardName} and {@code preconditionName}), both wired by bean name
 * through {@link BeanResolver}.
 */
public record StateMachineDefinition(
    String initialState,
    List<TransitionDescriptor> transitions,
    String machineGuardName,
    String preconditionName) {

  public StateMachineDefinition(String initialState, List<TransitionDescriptor> transitions) {
    this(initialState, transitions, null, null);
  }

  public List<TransitionDescriptor> transitionsFrom(String state) {
    return transitions.stream().filter(t -> t.sourceState().equals(state)).toList();
  }
}
