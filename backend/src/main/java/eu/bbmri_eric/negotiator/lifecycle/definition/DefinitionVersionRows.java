package eu.bbmri_eric.negotiator.lifecycle.definition;

import java.util.List;

/**
 * Every row of one Definition Version, already loaded. The input to {@link DefinitionCompiler}.
 *
 * <p>It exists so that loading and compiling are two steps rather than one. ADR 0001 wants "loading
 * the definition graph [to be] an explicit, testable step", and a compiler taking six repositories
 * instead of this record would make it neither: the load would be spread through the compile, and
 * the only way to test compilation would be against a database.
 *
 * <p>{@link DefinitionVersionLoader} is what populates it, in one transaction, and that transaction
 * is part of this record's contract rather than an implementation detail of the loader: a compile
 * compares owning versions by reference and groups Wiring by {@link Transition} identity, so rows
 * gathered from more than one persistence context are rows no compiler can accept. Anything else
 * building one — a test, or one day a definition file — owes the same guarantee, which for a
 * builder that never went near a database is free.
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

  /** A version whose Transitions carry no Actions, which is 5 of the 8 Negotiation edges today. */
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
