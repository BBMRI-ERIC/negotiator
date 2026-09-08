package eu.bbmri_eric.negotiator.lifecycle.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * One Definition Version as an indexed, immutable graph of States, Events and Transitions, with no
 * persistence behind it. A Definition Version is immutable once active, so one Compiled Graph
 * serves it for ever.
 *
 * <p>Its only identity is the Definition Version's row id. It carries no Definition Scope, so the
 * evaluator cannot branch on scope and treats both Lifecycles the same way.
 *
 * <p>Every lookup that takes a State throws for a State this version does not declare. See {@link
 * #requireDeclaredState}.
 *
 * <p>Build one through {@link #builder(long)}. The builder enforces every invariant, so a graph
 * built from rows and one built in a test meet the same rules.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class CompiledGraph {

  private final long definitionVersionId;
  private final String initialState;
  private final Set<String> states;
  private final Set<String> events;
  private final Set<String> terminalStates;
  private final Map<SourceAndEvent, CompiledTransition> bySourceAndEvent;
  private final Map<String, List<CompiledTransition>> bySource;

  public static Builder builder(long definitionVersionId) {
    return new Builder(definitionVersionId);
  }

  /** The row id of the Definition Version this graph was compiled from. */
  public long definitionVersionId() {
    return definitionVersionId;
  }

  /** The State a Lifecycle running this version starts in. */
  public String initialState() {
    return initialState;
  }

  /**
   * Whether this version declares the named State. A Lifecycle in an undeclared State is running
   * against the wrong graph.
   */
  public boolean declaresState(String state) {
    return states.contains(state);
  }

  /** Every State this version declares, Legacy States included. */
  public Set<String> states() {
    return states;
  }

  /**
   * Throws for a State this version does not declare. Every lookup that takes a State calls this
   * first.
   *
   * <p>An undeclared current State means a broken Definition Version Pin. Answering "no Transition"
   * instead would make the broken pin look like a terminal State.
   *
   * <p>It is public because the Transition Evaluator must check the State before the Event.
   *
   * @throws InvalidGraphException naming the State, this Definition Version and the States it
   *     declares
   */
  public void requireDeclaredState(String state) {
    if (!declaresState(state)) {
      throw new InvalidGraphException(
          "Definition Version %d declares no State named '%s'. Its States are %s."
              .formatted(definitionVersionId, state, states));
    }
  }

  /**
   * Whether this version declares the named Event. This tells an Override Event, which has no
   * Transition, apart from a misspelled Event name.
   */
  public boolean declaresEvent(String event) {
    return events.contains(event);
  }

  /** Every Event this version declares, including those carrying no Transition. */
  public Set<String> events() {
    return events;
  }

  /**
   * Whether the named State is one this version calls finished.
   *
   * @throws InvalidGraphException if this version does not declare the State. Answering {@code
   *     false} would let an unknown State keep a Negotiation in progress for ever.
   */
  public boolean isTerminal(String state) {
    requireDeclaredState(state);
    return terminalStates.contains(state);
  }

  /**
   * The one Transition leaving {@code fromState} for {@code event}, if there is one. The schema
   * allows at most one, and a branch uses two distinct Events, so the graph picks a Transition
   * before any Guard runs.
   *
   * <p>Empty means no such Transition leaves that State. It never means the State was unknown.
   *
   * @throws InvalidGraphException if this version does not declare {@code fromState}
   */
  public Optional<CompiledTransition> transition(String fromState, String event) {
    requireDeclaredState(fromState);
    return Optional.ofNullable(bySourceAndEvent.get(new SourceAndEvent(fromState, event)));
  }

  /**
   * Every Transition leaving {@code fromState}, in no promised order. The list is empty for a
   * terminal State and for a Legacy State.
   *
   * @throws InvalidGraphException if this version does not declare {@code fromState}
   */
  public List<CompiledTransition> transitionsFrom(String fromState) {
    requireDeclaredState(fromState);
    return bySource.getOrDefault(fromState, List.of());
  }

  @Override
  public String toString() {
    return "CompiledGraph[definitionVersionId=%d, states=%d, transitions=%d, initial=%s]"
        .formatted(definitionVersionId, states.size(), bySourceAndEvent.size(), initialState);
  }

  private record SourceAndEvent(String fromState, String event) {}

  /**
   * Assembles a {@link CompiledGraph} and refuses a malformed one. The checks live here rather than
   * in the compiler that reads rows, so a graph built in a test meets the same rules.
   *
   * <p>Marking a State initial or terminal also declares it, and adding a Transition declares its
   * Event.
   *
   * <p>Every refusal is an {@link InvalidGraphException}, so a caller can catch a corrupt definition
   * and nothing else.
   */
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Builder {

    private final long definitionVersionId;
    private final Set<String> states = new LinkedHashSet<>();
    private final Set<String> events = new LinkedHashSet<>();
    private final Set<String> initialStates = new LinkedHashSet<>();
    private final Set<String> terminalStates = new LinkedHashSet<>();
    private final List<CompiledTransition> transitions = new ArrayList<>();

    /** Declares an ordinary State. Declaring one twice is harmless. */
    public Builder state(@NonNull String name) {
      states.add(name);
      return this;
    }

    /** Declares a State and marks it initial. */
    public Builder initialState(String name) {
      state(name);
      initialStates.add(name);
      return this;
    }

    /**
     * Declares a State and marks it terminal. A State may be both initial and terminal, because the
     * schema permits a single-State Lifecycle.
     */
    public Builder terminalState(String name) {
      state(name);
      terminalStates.add(name);
      return this;
    }

    /**
     * Declares an Event that carries no Transition, such as an Override Event. {@link #transition}
     * declares every other Event.
     */
    public Builder eventWithoutTransition(@NonNull String name) {
      events.add(name);
      return this;
    }

    public Builder transition(@NonNull CompiledTransition transition) {
      transitions.add(transition);
      events.add(transition.event());
      return this;
    }

    public Builder transition(
        String fromState, String event, String toState, RequiredAuthority requiredAuthority) {
      return transition(new CompiledTransition(fromState, event, toState, requiredAuthority));
    }

    public CompiledGraph build() {
      requireExactlyOneInitialState();
      requireEveryTransitionToNameDeclaredStates();
      return new CompiledGraph(
          definitionVersionId,
          initialStates.iterator().next(),
          Set.copyOf(states),
          Set.copyOf(events),
          Set.copyOf(terminalStates),
          indexBySourceAndEvent(),
          indexBySource());
    }

    /**
     * The schema enforces at most one, because a partial unique index cannot require a row and a
     * draft version may have none yet. A Compiled Graph without one cannot start a Lifecycle, so
     * the "at least one" half is enforced here.
     */
    private void requireExactlyOneInitialState() {
      if (initialStates.size() != 1) {
        throw new InvalidGraphException(
            "Definition Version %d must declare exactly one initial State, found %d: %s"
                .formatted(definitionVersionId, initialStates.size(), initialStates));
      }
    }

    /**
     * Composite foreign keys stop a Transition spanning two Definition Versions in the database.
     * This applies the same rule to a graph built without the database.
     *
     * <p>Reachability is not checked. Legacy States have no inbound Transition, and {@code DRAFT}
     * has outbound Transitions but no inbound one, so a reachability check would refuse the seed.
     */
    private void requireEveryTransitionToNameDeclaredStates() {
      List<String> undeclared = new ArrayList<>();
      for (CompiledTransition transition : transitions) {
        if (!states.contains(transition.fromState())) {
          undeclared.add(transition.fromState());
        }
        if (!states.contains(transition.toState())) {
          undeclared.add(transition.toState());
        }
      }
      if (!undeclared.isEmpty()) {
        throw new InvalidGraphException(
            "Definition Version %d has Transitions naming undeclared States %s. Its States are %s."
                .formatted(definitionVersionId, undeclared, states));
      }
    }

    private Map<SourceAndEvent, CompiledTransition> indexBySourceAndEvent() {
      Map<SourceAndEvent, CompiledTransition> index = new HashMap<>();
      for (CompiledTransition transition : transitions) {
        SourceAndEvent key = new SourceAndEvent(transition.fromState(), transition.event());
        CompiledTransition existing = index.putIfAbsent(key, transition);
        if (existing != null) {
          throw new InvalidGraphException(
              ("Definition Version %d has two Transitions for State '%s' and Event '%s', to '%s' "
                      + "and to '%s'. Which one fires would depend on load order, so a graph that "
                      + "could ask a Guard to pick is refused rather than resolved.")
                  .formatted(
                      definitionVersionId,
                      key.fromState(),
                      key.event(),
                      existing.toState(),
                      transition.toState()));
        }
      }
      return Map.copyOf(index);
    }

    private Map<String, List<CompiledTransition>> indexBySource() {
      Map<String, List<CompiledTransition>> index = new LinkedHashMap<>();
      Set<String> sources = new HashSet<>();
      for (CompiledTransition transition : transitions) {
        sources.add(transition.fromState());
      }
      for (String source : sources) {
        List<CompiledTransition> leaving =
            transitions.stream().filter(t -> t.fromState().equals(source)).toList();
        index.put(source, leaving);
      }
      return Map.copyOf(index);
    }
  }
}
