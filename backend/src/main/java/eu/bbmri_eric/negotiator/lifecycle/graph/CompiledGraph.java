package eu.bbmri_eric.negotiator.lifecycle.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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
 * <p>Construct one through {@link #builder(long)}. The builder holds every invariant, so a graph
 * that exists is a graph that is well formed however it was assembled — by compiling rows, by a
 * test, or one day by whatever reads a definition file.
 */
public final class CompiledGraph {

  private final long definitionVersionId;
  private final String initialState;
  private final Set<String> states;
  private final Set<String> events;
  private final Set<String> terminalStates;
  private final Map<SourceAndEvent, CompiledTransition> bySourceAndEvent;
  private final Map<String, List<CompiledTransition>> bySource;

  private CompiledGraph(
      long definitionVersionId,
      String initialState,
      Set<String> states,
      Set<String> events,
      Set<String> terminalStates,
      Map<SourceAndEvent, CompiledTransition> bySourceAndEvent,
      Map<String, List<CompiledTransition>> bySource) {
    this.definitionVersionId = definitionVersionId;
    this.initialState = initialState;
    this.states = states;
    this.events = events;
    this.terminalStates = terminalStates;
    this.bySourceAndEvent = bySourceAndEvent;
    this.bySource = bySource;
  }

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
   * Whether this version declares the named State at all. A Lifecycle whose current State is not
   * declared here is running against the wrong graph, which the Definition Version Pin is what
   * prevents.
   */
  public boolean declaresState(String state) {
    return states.contains(state);
  }

  /** Every State this version declares, Legacy States included. */
  public Set<String> states() {
    return states;
  }

  /**
   * Whether this version declares the named Event at all.
   *
   * <p>This is the difference between an Override Event and a typo, and both v1 graphs have the
   * former: their Event universes are wider than their Transitions, and the three Events carrying
   * no Transition — {@code START}, {@code RETURN_FOR_RESUBMISSION}, {@code OVERRIDE} — are
   * deliberate names rather than leftovers. Without this the evaluator could only ever say "nothing
   * happens here", and a caller could not tell a real Event that leads nowhere from this State from
   * a name the deployment has never heard of.
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
   * @throws IllegalArgumentException if this version does not declare the State. Answering {@code
   *     false} for a State the graph has never heard of would be the more forgiving choice and the
   *     wrong one: a terminal-aggregation Guard that quietly reads an unknown State as "still
   *     running" leaves a Negotiation in progress for ever.
   */
  public boolean isTerminal(String state) {
    if (!states.contains(state)) {
      throw new IllegalArgumentException(
          "Definition Version %d declares no State named '%s'. Its States are %s."
              .formatted(definitionVersionId, state, states));
    }
    return terminalStates.contains(state);
  }

  /**
   * The one Transition leaving {@code fromState} for {@code event}, if there is one. At most one
   * can exist: {@code uq_transition_definition_source_event} says so in the schema, and ADR 0007
   * models a branch as two distinct Events rather than as two edges for one, so a graph is
   * deterministic before any Guard is consulted.
   */
  public Optional<CompiledTransition> transition(String fromState, String event) {
    return Optional.ofNullable(bySourceAndEvent.get(new SourceAndEvent(fromState, event)));
  }

  /**
   * Every Transition leaving {@code fromState}, in no promised order. This is the candidate set a
   * Possible Events listing evaluates one by one; an empty list is the ordinary answer for a
   * terminal State, and for a Legacy State no work can be in.
   */
  public List<CompiledTransition> transitionsFrom(String fromState) {
    return bySource.getOrDefault(fromState, List.of());
  }

  @Override
  public String toString() {
    return "CompiledGraph[definitionVersionId=%d, states=%d, transitions=%d, initial=%s]"
        .formatted(definitionVersionId, states.size(), bySourceAndEvent.size(), initialState);
  }

  private record SourceAndEvent(String fromState, String event) {}

  /**
   * Assembles a {@link CompiledGraph} and refuses to produce a malformed one. Every check lives
   * here rather than in the compiler that reads the rows, so that a graph built by a test or by a
   * seed is held to the same rules as one built from the database — the alternative puts the rules
   * where only one of three construction paths passes through them.
   */
  public static final class Builder {

    private final long definitionVersionId;
    private final Set<String> states = new LinkedHashSet<>();
    private final Set<String> events = new LinkedHashSet<>();
    private final Set<String> initialStates = new LinkedHashSet<>();
    private final Set<String> terminalStates = new LinkedHashSet<>();
    private final List<CompiledTransition> transitions = new ArrayList<>();

    private Builder(long definitionVersionId) {
      this.definitionVersionId = definitionVersionId;
    }

    /** Declares an ordinary State. Declaring one twice is harmless. */
    public Builder state(String name) {
      states.add(Objects.requireNonNull(name, "name"));
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
    public Builder eventWithoutTransition(String name) {
      events.add(Objects.requireNonNull(name, "name"));
      return this;
    }

    public Builder transition(CompiledTransition transition) {
      Objects.requireNonNull(transition, "transition");
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
        throw new IllegalStateException(
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
        throw new IllegalStateException(
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
          throw new IllegalStateException(
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
