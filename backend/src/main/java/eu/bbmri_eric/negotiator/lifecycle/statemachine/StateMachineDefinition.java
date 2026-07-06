package eu.bbmri_eric.negotiator.lifecycle.statemachine;

import java.util.List;
import java.util.Set;

public record StateMachineDefinition(Set<String> states, List<TransitionDescriptor> transitions) {

  public StateMachineDefinition {
    states = Set.copyOf(states);
    transitions = List.copyOf(transitions);
  }
}
