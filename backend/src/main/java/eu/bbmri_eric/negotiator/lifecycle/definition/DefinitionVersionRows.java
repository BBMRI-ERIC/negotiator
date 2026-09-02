package eu.bbmri_eric.negotiator.lifecycle.definition;

import java.util.List;

/**
 * Every row of one Definition Version, already loaded. The input to {@link DefinitionCompiler}.
 *
 * <p>It keeps loading and compiling as separate steps, so compilation can be tested without a
 * database.
 *
 * <p>Nothing populates it yet. No repository in this package has a "load the whole graph by
 * definition id" query, deliberately — the slab that writes one is the slab that starts reading
 * these tables, and it is not this one.
 */
record DefinitionVersionRows(
    LifecycleDefinition definition,
    List<State> states,
    List<Event> events,
    List<Transition> transitions,
    List<GuardWiring> guardWirings) {

  DefinitionVersionRows {
    states = List.copyOf(states);
    events = List.copyOf(events);
    transitions = List.copyOf(transitions);
    guardWirings = List.copyOf(guardWirings);
  }
}
