package eu.bbmri_eric.negotiator.lifecycle.definition;

import eu.bbmri_eric.negotiator.lifecycle.graph.ActionCatalogue;
import eu.bbmri_eric.negotiator.lifecycle.graph.ActionStep;
import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledGraph;
import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledTransition;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardCatalogue;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardStep;
import eu.bbmri_eric.negotiator.lifecycle.graph.InvalidGraphException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Definition Compilation: turns the rows of one Definition Version into a Compiled Graph.
 *
 * <p>The expensive and fallible work happens here, once per Definition Version: it binds each
 * Wiring row's type key and params through its catalogue and folds the two Guard scopes into one
 * chain per Transition. An unknown type key or unreadable params fails the compile, not a user's
 * first attempt at that Transition.
 *
 * <p>Two things it deliberately does not do. It does not <em>load</em>: it is handed {@link
 * DefinitionVersionRows}, which is what keeps loading an explicit step and compilation
 * unit-testable. And it does not read {@link LifecycleDefinition#getScope()} at all — the compiled
 * graph carries no Definition Scope because the evaluator turns out never to need one.
 *
 * <p>It is package-private on purpose. Nothing outside this package may compile a graph yet, which
 * is what keeps the package inert while there is no loader to call it.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class DefinitionCompiler {

  private final GuardCatalogue guardCatalogue;
  private final ActionCatalogue actionCatalogue;

  CompiledGraph compile(DefinitionVersionRows rows) {
    requireRowsBelongToTheVersion(rows);

    List<GuardStep> definitionWideChain = bindChain(definitionWideWirings(rows));
    Map<Transition, List<GuardStep>> perTransition = bindPerTransitionChains(rows);
    Map<Transition, List<ActionStep>> actionChains = bindActionChains(rows);

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
              effectiveChain(definitionWideChain, perTransition.get(transition)),
              actionChains.getOrDefault(transition, List.of())));
    }
    return graph.build();
  }

  private Map<Transition, List<ActionStep>> bindActionChains(DefinitionVersionRows rows) {
    Map<Transition, List<ActionWiring>> grouped = new HashMap<>();
    for (ActionWiring wiring : rows.actionWirings()) {
      grouped.computeIfAbsent(wiring.getTransition(), t -> new ArrayList<>()).add(wiring);
    }
    Map<Transition, List<ActionStep>> bound = new HashMap<>();
    grouped.forEach(
        (transition, wirings) ->
            bound.put(
                transition,
                wirings.stream()
                    .sorted(Comparator.comparing(ActionWiring::getSortOrder))
                    .map(wiring -> actionCatalogue.bind(wiring.getTypeKey(), wiring.getParams()))
                    .toList()));
    return bound;
  }

  /**
   * Definition-wide Guards come first, then the Transition's own, each in configured order. Folding
   * here means the Transition Evaluator runs one chain and never sees two scopes.
   *
   * <p>Every Transition shares the definition-wide steps. A {@link GuardStep} holds only its
   * configuration, so sharing is safe and saves parsing the same params once per Transition.
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
   * Groups by {@link Transition} instance rather than row id, so compilation works on rows that were
   * never persisted. Transition does not override {@code equals}, so this relies on one persistence
   * context holding one instance per row.
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
   * The composite foreign keys already stop a row from mixing versions in the database. This check
   * covers rows built without a database and a loader with a wrong {@code WHERE} clause.
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
    // ActionWiring has no definition reference of its own, so it is checked through its Transition.
    for (ActionWiring wiring : rows.actionWirings()) {
      if (wiring.getTransition().getLifecycleDefinition() != version) {
        foreign.add("ActionWiring " + wiring.getTypeKey());
      }
    }
    if (!foreign.isEmpty()) {
      throw new InvalidGraphException(
          "These rows belong to a different Definition Version and cannot be compiled together: "
              + foreign);
    }
  }
}
