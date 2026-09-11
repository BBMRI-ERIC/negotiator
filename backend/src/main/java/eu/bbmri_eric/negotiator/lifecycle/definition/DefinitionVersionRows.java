package eu.bbmri_eric.negotiator.lifecycle.definition;

import java.util.List;

/**
 * Every row of one Definition Version, already loaded. The input to {@link DefinitionCompiler}.
 *
 * <p>It keeps loading and compiling as separate steps, so compilation can be tested without a
 * database.
 *
 * <p>All rows must come from one persistence context. The compiler compares owning versions by
 * reference and groups Wiring by {@link Transition} instance, so it refuses rows gathered across
 * several. {@link DefinitionVersionLoader} uses one transaction for this.
 */
record DefinitionVersionRows(
    LifecycleDefinition definition,
    List<State> states,
    List<Event> events,
    List<Transition> transitions,
    List<GuardWiring> guardWirings,
    List<ActionWiring> actionWirings) {

  DefinitionVersionRows {
    states = List.copyOf(states);
    events = List.copyOf(events);
    transitions = List.copyOf(transitions);
    guardWirings = List.copyOf(guardWirings);
    actionWirings = List.copyOf(actionWirings);
  }

  /** A version whose Transitions carry no Actions. */
  static DefinitionVersionRows withoutActions(
      LifecycleDefinition definition,
      List<State> states,
      List<Event> events,
      List<Transition> transitions,
      List<GuardWiring> guardWirings) {
    return new DefinitionVersionRows(
        definition, states, events, transitions, guardWirings, List.of());
  }
}
