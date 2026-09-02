package eu.bbmri_eric.negotiator.lifecycle.definition;

import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledGraph;
import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledTransition;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardCatalogue;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardStep;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns the rows of one Definition Version into the compiled graph an evaluator reads. This is the
 * only place the relational shape of a definition and the in-memory shape of a graph are both
 * known, and it is in the definition package because that is where the rows are.
 *
 * <p>Everything expensive or fallible happens here, once per Definition Version, rather than once
 * per evaluation: the {@code (fromState, event)} index is built, each Guard Wiring row's type key
 * is resolved against the catalogue, each row's jsonb params is read into the strategy's declared
 * type, and the two Guard scopes are folded into one effective chain per Transition. An unknown
 * type key or unreadable params therefore fails a compile, loudly and once — not a user's click, on
 * whichever Transition happens to be tried first.
 *
 * <p>Two things it deliberately does not do. It does not <em>load</em>: it is handed {@link
 * DefinitionVersionRows}, which is what keeps loading an explicit step and compilation
 * unit-testable. And it does not read {@link LifecycleDefinition#getScope()} at all — the compiled
 * graph carries no Definition Scope because the evaluator turns out never to need one.
 *
 * <p>It is package-private on purpose. Nothing outside this package may compile a graph yet, which
 * is what keeps the package inert while there is no loader to call it.
 */
class DefinitionCompiler {

  private final GuardCatalogue guardCatalogue;

  DefinitionCompiler(GuardCatalogue guardCatalogue) {
    this.guardCatalogue = guardCatalogue;
  }

  CompiledGraph compile(DefinitionVersionRows rows) {
    requireRowsBelongToTheVersion(rows);

    List<GuardStep> definitionWideChain = bindChain(definitionWideWirings(rows));
    Map<Transition, List<GuardStep>> perTransition = bindPerTransitionChains(rows);

    CompiledGraph.Builder graph = CompiledGraph.builder(rows.definition().getId());
    for (State state : rows.states()) {
      graph.state(state.getName());
      if (state.isInitial()) {
        graph.initialState(state.getName());
      }
      if (state.isTerminal()) {
        graph.terminalState(state.getName());
      }
    }
    for (Event event : rows.events()) {
      graph.eventWithoutTransition(event.getName());
    }
    for (Transition transition : rows.transitions()) {
      graph.transition(
          new CompiledTransition(
              transition.getFromState().getName(),
              transition.getEvent().getName(),
              transition.getToState().getName(),
              transition.getRequiredAuthority(),
              effectiveChain(definitionWideChain, perTransition.get(transition))));
    }
    return graph.build();
  }

  /**
   * ADR 0005: "definition-level entries before Transition entries, each in their configured order".
   * Folding the two here rather than at fire time is what lets the evaluator run one loop and never
   * learn that Guards have two scopes — and it is why definition-level wiring removes the
   * copy-drift risk of re-attaching a Guard to every Transition a later version adds.
   *
   * <p>The definition-wide steps are bound once and shared by every Transition. A {@link GuardStep}
   * closes over configuration and nothing else, so sharing one is safe, and binding the same row N
   * times would parse the same jsonb N times for no reader.
   */
  private static List<GuardStep> effectiveChain(
      List<GuardStep> definitionWide, List<GuardStep> transitionScoped) {
    if (transitionScoped == null || transitionScoped.isEmpty()) {
      return definitionWide;
    }
    List<GuardStep> chain = new ArrayList<>(definitionWide);
    chain.addAll(transitionScoped);
    return List.copyOf(chain);
  }

  private static List<GuardWiring> definitionWideWirings(DefinitionVersionRows rows) {
    return rows.guardWirings().stream()
        .filter(wiring -> wiring.getTransition() == null)
        .sorted(Comparator.comparing(GuardWiring::getSortOrder))
        .toList();
  }

  /**
   * Grouped by the {@link Transition} object rather than by its row id, so that compilation works
   * on rows that were never persisted and the compiler never needs an id at all. Neither entity
   * overrides {@code equals}, so this is identity — which is what is wanted either way: within one
   * persistence context a Transition is one instance, and in a test it is whatever the test built.
   */
  private Map<Transition, List<GuardStep>> bindPerTransitionChains(DefinitionVersionRows rows) {
    Map<Transition, List<GuardWiring>> grouped = new HashMap<>();
    for (GuardWiring wiring : rows.guardWirings()) {
      if (wiring.getTransition() != null) {
        grouped.computeIfAbsent(wiring.getTransition(), t -> new ArrayList<>()).add(wiring);
      }
    }
    Map<Transition, List<GuardStep>> bound = new HashMap<>();
    grouped.forEach(
        (transition, wirings) -> {
          List<GuardWiring> ordered =
              wirings.stream().sorted(Comparator.comparing(GuardWiring::getSortOrder)).toList();
          bound.put(transition, bindChain(ordered));
        });
    return bound;
  }

  private List<GuardStep> bindChain(List<GuardWiring> wirings) {
    return wirings.stream()
        .map(wiring -> guardCatalogue.bind(wiring.getTypeKey(), wiring.getParams()))
        .toList();
  }

  /**
   * Composite foreign keys against {@code UNIQUE (lifecycle_definition_id, id)} already make a
   * straddling row impossible in the database. Said again here because these rows may never have
   * been near a database, and because a loader that got its {@code WHERE} clause wrong would
   * otherwise compile a graph quietly mixing two versions.
   */
  private static void requireRowsBelongToTheVersion(DefinitionVersionRows rows) {
    LifecycleDefinition version = rows.definition();
    List<String> foreign = new ArrayList<>();
    for (State state : rows.states()) {
      if (state.getLifecycleDefinition() != version) {
        foreign.add("State " + state.getName());
      }
    }
    for (Event event : rows.events()) {
      if (event.getLifecycleDefinition() != version) {
        foreign.add("Event " + event.getName());
      }
    }
    for (Transition transition : rows.transitions()) {
      if (transition.getLifecycleDefinition() != version) {
        foreign.add("Transition on " + transition.getEvent().getName());
      }
    }
    for (GuardWiring wiring : rows.guardWirings()) {
      if (wiring.getLifecycleDefinition() != version) {
        foreign.add("GuardWiring " + wiring.getTypeKey());
      }
    }
    if (!foreign.isEmpty()) {
      throw new IllegalArgumentException(
          "These rows belong to a different Definition Version and cannot be compiled together: "
              + foreign);
    }
  }
}
