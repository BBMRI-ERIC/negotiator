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
