package eu.bbmri_eric.negotiator.lifecycle.graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
  void definitionVersionId_isTheWholeIdentity() {
    assertThat(graph().definitionVersionId()).isEqualTo(VERSION_ID);
  }

  @Test
  void initialState_isTheOneStateCarryingTheFlag() {
    assertThat(graph().initialState()).isEqualTo("SUBMITTED");
  }

  @Test
  void states_includesEveryDeclaredStateAndNotOnlyTheReachableOnes() {
    assertThat(graph().states())
        .containsExactlyInAnyOrder("DRAFT", "SUBMITTED", "IN_PROGRESS", "ABANDONED", "APPROVED");
  }

  @Test
  void transition_whenAnEdgeLeavesTheStateForTheEvent_returnsIt() {
    assertThat(graph().transition("SUBMITTED", "APPROVE"))
        .contains(
            new CompiledTransition(
                "SUBMITTED", "APPROVE", "IN_PROGRESS", RequiredAuthority.IS_ADMIN));
  }

  @Test
  void transition_whenTheEventLeadsNowhereFromThatState_returnsEmpty() {
    assertThat(graph().transition("IN_PROGRESS", "APPROVE")).isEmpty();
  }

  @Test
  void transition_whenTheEventCarriesNoTransitionAnywhere_returnsEmpty() {
    assertThat(graph().transition("SUBMITTED", "START")).isEmpty();
  }

  /**
   * An Event universe wider than the Transitions is the ordinary case, not a defect: both v1 graphs
   * have one, and the Override Event is the reason the model keeps such names rather than dropping
   * them.
   */
  @Test
  void events_includesEventsThatCarryNoTransition() {
    assertThat(graph().events())
        .containsExactlyInAnyOrder("SUBMIT", "APPROVE", "DECLINE", "ABANDON", "START");
  }

  @Test
  void declaresEvent_separatesAnEventThatLeadsNowhereFromOneThatDoesNotExist() {
    CompiledGraph graph = graph();

    assertThat(graph.declaresEvent("START")).isTrue();
    assertThat(graph.declaresEvent("NOT_AN_EVENT")).isFalse();
  }

  @Test
  void transition_declaresItsOwnEvent_soOnlyTransitionlessOnesNeedNaming() {
    assertThat(graph().declaresEvent("APPROVE")).isTrue();
  }

  @Test
  void transitionsFrom_returnsEveryEdgeLeavingTheState() {
    assertThat(graph().transitionsFrom("SUBMITTED"))
        .extracting(CompiledTransition::event)
        .containsExactlyInAnyOrder("APPROVE", "DECLINE");
  }

  @Test
  void transitionsFrom_whenTheStateIsTerminal_returnsEmpty() {
    assertThat(graph().transitionsFrom("ABANDONED")).isEmpty();
  }

  /** A Legacy State is not an error and not a special case — it simply offers nothing. */
  @Test
  void transitionsFrom_whenTheStateIsALegacyState_returnsEmpty() {
    assertThat(graph().transitionsFrom("APPROVED")).isEmpty();
  }

  @Test
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
  void isTerminal_whenTheVersionDoesNotDeclareTheState_throws() {
    assertThatThrownBy(() -> graph().isTerminal("RESOURCE_MADE_AVAILABLE"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("RESOURCE_MADE_AVAILABLE")
        .hasMessageContaining("declares no State");
  }

  @Test
  void declaresState_separatesAStateThisVersionHasFromOneItDoesNot() {
    CompiledGraph graph = graph();

    assertThat(graph.declaresState("APPROVED")).isTrue();
    assertThat(graph.declaresState("RESOURCE_MADE_AVAILABLE")).isFalse();
  }

  @Test
  void build_whenNoStateIsInitial_isRefused() {
    CompiledGraph.Builder builder = CompiledGraph.builder(VERSION_ID).state("SUBMITTED");

    assertThatThrownBy(builder::build)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("exactly one initial State")
        .hasMessageContaining("found 0");
  }

  @Test
  void build_whenTwoStatesAreInitial_isRefused() {
    CompiledGraph.Builder builder =
        CompiledGraph.builder(VERSION_ID).initialState("DRAFT").initialState("SUBMITTED");

    assertThatThrownBy(builder::build)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("exactly one initial State")
        .hasMessageContaining("found 2");
  }

  /** Independent columns, so a one-State Lifecycle is a graph the schema permits. */
  @Test
  void build_whenOneStateIsBothInitialAndTerminal_isAccepted() {
    CompiledGraph graph =
        CompiledGraph.builder(VERSION_ID).initialState("DONE").terminalState("DONE").build();

    assertThat(graph.initialState()).isEqualTo("DONE");
    assertThat(graph.isTerminal("DONE")).isTrue();
    assertThat(graph.states()).containsExactly("DONE");
  }

  @Test
  void build_whenATransitionNamesAnUndeclaredSourceState_isRefused() {
    CompiledGraph.Builder builder =
        CompiledGraph.builder(VERSION_ID)
            .initialState("SUBMITTED")
            .transition("NOWHERE", "SUBMIT", "SUBMITTED", RequiredAuthority.NONE);

    assertThatThrownBy(builder::build)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("undeclared States")
        .hasMessageContaining("NOWHERE");
  }

  @Test
  void build_whenATransitionNamesAnUndeclaredTargetState_isRefused() {
    CompiledGraph.Builder builder =
        CompiledGraph.builder(VERSION_ID)
            .initialState("SUBMITTED")
            .transition("SUBMITTED", "APPROVE", "NOWHERE", RequiredAuthority.NONE);

    assertThatThrownBy(builder::build)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("undeclared States")
        .hasMessageContaining("NOWHERE");
  }

  /**
   * The schema says this too, in {@code uq_transition_definition_source_event}. It is checked again
   * here because a graph can be assembled without going near the database, and because refusing is
   * a decision: the alternative is Guard-selected branching, which ADR 0007 spells as two distinct
   * Events instead.
   */
  @Test
  void build_whenTwoTransitionsShareASourceAndEvent_isRefused() {
    CompiledGraph.Builder builder =
        CompiledGraph.builder(VERSION_ID)
            .initialState("SUBMITTED")
            .state("IN_PROGRESS")
            .state("ABANDONED")
            .transition("SUBMITTED", "APPROVE", "IN_PROGRESS", RequiredAuthority.IS_ADMIN)
            .transition("SUBMITTED", "APPROVE", "ABANDONED", RequiredAuthority.IS_ADMIN);

    assertThatThrownBy(builder::build)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("two Transitions for State 'SUBMITTED' and Event 'APPROVE'")
        .hasMessageContaining("IN_PROGRESS")
        .hasMessageContaining("ABANDONED");
  }

  /** The same Event from two different States is the ordinary case, and must stay legal. */
  @Test
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
  void transition_whenGivenNullRequiredAuthority_isRefusedAtConstruction() {
    assertThatThrownBy(() -> new CompiledTransition("A", "E", "B", null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("RequiredAuthority.NONE");
  }
}
