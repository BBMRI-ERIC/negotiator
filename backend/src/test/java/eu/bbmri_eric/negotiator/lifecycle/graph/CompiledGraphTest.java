package eu.bbmri_eric.negotiator.lifecycle.graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The compiled graph's whole contract, exercised without Spring, without a database and without the
 * definition package. That it can be is the point: a graph is assembled from names and flags, so a
 * test can build the exact shape it wants to ask about.
 *
 * <p>The fixture is a miniature of the v1 Negotiation graph, kept small enough to read and shaped
 * to carry the two structures the real one has that a naive graph would not: {@code DRAFT}, which
 * Transitions lead out of and none leads into, and {@code APPROVED}, a Legacy State no Transition
 * touches at all.
 */
class CompiledGraphTest {

  private static final long VERSION_ID = 7L;

  private static CompiledGraph graph() {
    return CompiledGraph.builder(VERSION_ID)
        .state("DRAFT")
        .initialState("SUBMITTED")
        .state("IN_PROGRESS")
        .terminalState("ABANDONED")
        .state("APPROVED")
        .transition("DRAFT", "SUBMIT", "SUBMITTED", RequiredAuthority.IS_CREATOR)
        .transition("SUBMITTED", "APPROVE", "IN_PROGRESS", RequiredAuthority.IS_ADMIN)
        .transition("SUBMITTED", "DECLINE", "ABANDONED", RequiredAuthority.IS_ADMIN)
        .transition("IN_PROGRESS", "ABANDON", "ABANDONED", RequiredAuthority.NONE)
        .eventWithoutTransition("START")
        .build();
  }

  @Test
  @DisplayName("a compiled graph is identified by its Definition Version's row id and nothing else")
  void definitionVersionId_isTheWholeIdentity() {
    assertThat(graph().definitionVersionId()).isEqualTo(VERSION_ID);
  }

  @Test
  @DisplayName("the initial State is the one State whose row carries the flag")
  void initialState_isTheOneStateCarryingTheFlag() {
    assertThat(graph().initialState()).isEqualTo("SUBMITTED");
  }

  @Test
  @DisplayName("the graph lists every declared State, not only the reachable ones")
  void states_includesEveryDeclaredStateAndNotOnlyTheReachableOnes() {
    assertThat(graph().states())
        .containsExactlyInAnyOrder("DRAFT", "SUBMITTED", "IN_PROGRESS", "ABANDONED", "APPROVED");
  }

  @Test
  @DisplayName("an Event that leaves the current State returns the edge it leads along")
  void transition_whenAnEdgeLeavesTheStateForTheEvent_returnsIt() {
    assertThat(graph().transition("SUBMITTED", "APPROVE"))
        .contains(
            new CompiledTransition(
                "SUBMITTED", "APPROVE", "IN_PROGRESS", RequiredAuthority.IS_ADMIN));
  }

  @Test
  @DisplayName("an Event that leads nowhere from that State returns no edge")
  void transition_whenTheEventLeadsNowhereFromThatState_returnsEmpty() {
    assertThat(graph().transition("IN_PROGRESS", "APPROVE")).isEmpty();
  }

  @Test
  @DisplayName("an Event carrying no Transition anywhere returns no edge")
  void transition_whenTheEventCarriesNoTransitionAnywhere_returnsEmpty() {
    assertThat(graph().transition("SUBMITTED", "START")).isEmpty();
  }

  /**
   * An Event universe wider than the Transitions is the ordinary case, not a defect: both v1 graphs
   * have one, and the Override Event is the reason the model keeps such names rather than dropping
   * them.
   */
  @Test
  @DisplayName("the Event universe is wider than the Transitions, as both v1 graphs are")
  void events_includesEventsThatCarryNoTransition() {
    assertThat(graph().events())
        .containsExactlyInAnyOrder("SUBMIT", "APPROVE", "DECLINE", "ABANDON", "START");
  }

  @Test
  @DisplayName("an Event that leads nowhere is told apart from one the version never declared")
  void declaresEvent_separatesAnEventThatLeadsNowhereFromOneThatDoesNotExist() {
    CompiledGraph graph = graph();

    assertThat(graph.declaresEvent("START")).isTrue();
    assertThat(graph.declaresEvent("NOT_AN_EVENT")).isFalse();
  }

  @Test
  @DisplayName("a Transition declares its own Event, so only a Transitionless one needs naming")
  void transition_declaresItsOwnEvent_soOnlyTransitionlessOnesNeedNaming() {
    assertThat(graph().declaresEvent("APPROVE")).isTrue();
  }

  @Test
  @DisplayName("every Transition leaving a State is offered from it")
  void transitionsFrom_returnsEveryEdgeLeavingTheState() {
    assertThat(graph().transitionsFrom("SUBMITTED"))
        .extracting(CompiledTransition::event)
        .containsExactlyInAnyOrder("APPROVE", "DECLINE");
  }

  @Test
  @DisplayName("a terminal State offers no Transitions")
  void transitionsFrom_whenTheStateIsTerminal_returnsEmpty() {
    assertThat(graph().transitionsFrom("ABANDONED")).isEmpty();
  }

  /** A Legacy State is not an error and not a special case — it simply offers nothing. */
  @Test
  @DisplayName("a Legacy State offers no Transitions, and is not an error")
  void transitionsFrom_whenTheStateIsALegacyState_returnsEmpty() {
    assertThat(graph().transitionsFrom("APPROVED")).isEmpty();
  }

  @Test
  @DisplayName("terminality is the flag this version declares, State by State")
  void isTerminal_answersTheFlagTheVersionDeclares() {
    CompiledGraph graph = graph();

    assertThat(graph.isTerminal("ABANDONED")).isTrue();
    assertThat(graph.isTerminal("IN_PROGRESS")).isFalse();
    assertThat(graph.isTerminal("APPROVED")).isFalse();
  }

  /**
   * The unforgiving answer, on purpose. A terminal-aggregation Guard asks each Resource's own
   * pinned version this question, so a State the version has never heard of means the pin is wrong
   * — and reading that as "still running" is how a Negotiation stays in progress for ever.
   */
  @Test
  @DisplayName(
      "asking whether an undeclared State is terminal is refused rather than answered false")
  void isTerminal_whenTheVersionDoesNotDeclareTheState_throws() {
    assertThatThrownBy(() -> graph().isTerminal("RESOURCE_MADE_AVAILABLE"))
        .isInstanceOf(InvalidGraphException.class)
        .hasMessageContaining("RESOURCE_MADE_AVAILABLE");
  }

  @Test
  @DisplayName("a State this version declares is told apart from one it has never heard of")
  void declaresState_separatesAStateThisVersionHasFromOneItDoesNot() {
    CompiledGraph graph = graph();

    assertThat(graph.declaresState("APPROVED")).isTrue();
    assertThat(graph.declaresState("RESOURCE_MADE_AVAILABLE")).isFalse();
  }

  @Test
  @DisplayName("a graph with no initial State cannot be built")
  void build_whenNoStateIsInitial_isRefused() {
    CompiledGraph.Builder builder = CompiledGraph.builder(VERSION_ID).state("SUBMITTED");

    assertThatThrownBy(builder::build).isInstanceOf(InvalidGraphException.class);
  }

  @Test
  @DisplayName("a graph with two initial States cannot be built")
  void build_whenTwoStatesAreInitial_isRefused() {
    CompiledGraph.Builder builder =
        CompiledGraph.builder(VERSION_ID).initialState("DRAFT").initialState("SUBMITTED");

    assertThatThrownBy(builder::build)
        .isInstanceOf(InvalidGraphException.class)
        .hasMessageContaining("DRAFT")
        .hasMessageContaining("SUBMITTED");
  }

  /** Independent columns, so a one-State Lifecycle is a graph the schema permits. */
  @Test
  @DisplayName("one State may be both initial and terminal, so a one-State Lifecycle is legal")
  void build_whenOneStateIsBothInitialAndTerminal_isAccepted() {
    CompiledGraph graph =
        CompiledGraph.builder(VERSION_ID).initialState("DONE").terminalState("DONE").build();

    assertThat(graph.initialState()).isEqualTo("DONE");
    assertThat(graph.isTerminal("DONE")).isTrue();
    assertThat(graph.states()).containsExactly("DONE");
  }

  @Test
  @DisplayName("a Transition out of a State the version never declared is refused")
  void build_whenATransitionNamesAnUndeclaredSourceState_isRefused() {
    CompiledGraph.Builder builder =
        CompiledGraph.builder(VERSION_ID)
            .initialState("SUBMITTED")
            .transition("NOWHERE", "SUBMIT", "SUBMITTED", RequiredAuthority.NONE);

    assertThatThrownBy(builder::build)
        .isInstanceOf(InvalidGraphException.class)
        .hasMessageContaining("NOWHERE");
  }

  @Test
  @DisplayName("a Transition into a State the version never declared is refused")
  void build_whenATransitionNamesAnUndeclaredTargetState_isRefused() {
    CompiledGraph.Builder builder =
        CompiledGraph.builder(VERSION_ID)
            .initialState("SUBMITTED")
            .transition("SUBMITTED", "APPROVE", "NOWHERE", RequiredAuthority.NONE);

    assertThatThrownBy(builder::build)
        .isInstanceOf(InvalidGraphException.class)
        .hasMessageContaining("NOWHERE");
  }

  /**
   * The schema says this too, in {@code uq_transition_definition_source_event}. It is checked again
   * here because a graph can be assembled without going near the database, and because refusing is
   * a decision: the alternative is Guard-selected branching, which ADR 0007 spells as two distinct
   * Events instead.
   */
  @Test
  @DisplayName("two Transitions leaving one State on one Event are refused")
  void build_whenTwoTransitionsShareASourceAndEvent_isRefused() {
    CompiledGraph.Builder builder =
        CompiledGraph.builder(VERSION_ID)
            .initialState("SUBMITTED")
            .state("IN_PROGRESS")
            .state("ABANDONED")
            .transition("SUBMITTED", "APPROVE", "IN_PROGRESS", RequiredAuthority.IS_ADMIN)
            .transition("SUBMITTED", "APPROVE", "ABANDONED", RequiredAuthority.IS_ADMIN);

    assertThatThrownBy(builder::build)
        .isInstanceOf(InvalidGraphException.class)
        .hasMessageContaining("SUBMITTED")
        .hasMessageContaining("APPROVE")
        .hasMessageContaining("IN_PROGRESS")
        .hasMessageContaining("ABANDONED");
  }

  /** The same Event from two different States is the ordinary case, and must stay legal. */
  @Test
  @DisplayName("one Event may leave two different States, which is the ordinary case")
  void build_whenOneEventLeavesTwoDifferentStates_isAccepted() {
    CompiledGraph graph =
        CompiledGraph.builder(VERSION_ID)
            .initialState("IN_PROGRESS")
            .state("PAUSED")
            .terminalState("ABANDONED")
            .transition("IN_PROGRESS", "ABANDON", "ABANDONED", RequiredAuthority.NONE)
            .transition("PAUSED", "ABANDON", "ABANDONED", RequiredAuthority.NONE)
            .build();

    assertThat(graph.transition("IN_PROGRESS", "ABANDON")).isPresent();
    assertThat(graph.transition("PAUSED", "ABANDON")).isPresent();
  }

  @Test
  @DisplayName("an edge with no Required Authority is refused at construction")
  void transition_whenGivenNullRequiredAuthority_isRefusedAtConstruction() {
    assertThatThrownBy(() -> new CompiledTransition("A", "E", "B", null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("requiredAuthority");
  }
}
