package eu.bbmri_eric.negotiator.lifecycle.graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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

  /** A State of the <em>other</em> v1 graph, so nothing here can declare it by accident. */
  private static final String UNDECLARED_STATE = "RESOURCE_MADE_AVAILABLE";

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
   * All three questions that take a State, refusing one this version does not declare — one claim
   * rather than three, because it is one rule.
   *
   * <p>The unforgiving answer is deliberate, and the reason differs by lookup. For terminality, a
   * terminal-aggregation Guard that quietly read an unknown State as "still running" would leave a
   * Negotiation in progress for ever. For the other two, an empty answer is indistinguishable from
   * a legitimately terminal State, so a broken Definition Version Pin would reach a requester as
   * "nothing is available" — which user story 64 says an empty listing must never be able to mean.
   *
   * <p>Each refusal offers what the version <em>does</em> declare, which is what makes it
   * actionable from a log line, so each is asserted for the State asked about, the Definition
   * Version, and a State the version has.
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("everyLookupTakingAState")
  @DisplayName("every lookup taking a State refuses one this version does not declare")
  void everyLookupTakingAState_whenTheVersionDoesNotDeclareIt_throws(
      String lookup, ThrowingCallable askIt) {
    assertThatThrownBy(askIt)
        .isInstanceOf(InvalidGraphException.class)
        .hasMessageContaining("Definition Version " + VERSION_ID)
        .hasMessageContaining(UNDECLARED_STATE)
        .hasMessageContaining("SUBMITTED");
  }

  private static Stream<Arguments> everyLookupTakingAState() {
    return Stream.of(
        arguments("isTerminal", (ThrowingCallable) () -> graph().isTerminal(UNDECLARED_STATE)),
        arguments(
            "transition", (ThrowingCallable) () -> graph().transition(UNDECLARED_STATE, "APPROVE")),
        arguments(
            "transitionsFrom", (ThrowingCallable) () -> graph().transitionsFrom(UNDECLARED_STATE)));
  }

  @Test
  @DisplayName("a State this version declares is told apart from one it has never heard of")
  void declaresState_separatesAStateThisVersionHasFromOneItDoesNot() {
    CompiledGraph graph = graph();

    assertThat(graph.declaresState("APPROVED")).isTrue();
    assertThat(graph.declaresState(UNDECLARED_STATE)).isFalse();
  }

  @Test
  @DisplayName("a graph with no initial State cannot be built")
  void build_whenNoStateIsInitial_isRefused() {
    CompiledGraph.Builder builder = CompiledGraph.builder(VERSION_ID).state("SUBMITTED");

    assertThatThrownBy(builder::build)
        .isInstanceOf(InvalidGraphException.class)
        .hasMessageContaining("found 0");
  }

  @Test
  @DisplayName("a graph with two initial States cannot be built")
  void build_whenTwoStatesAreInitial_isRefused() {
    CompiledGraph.Builder builder =
        CompiledGraph.builder(VERSION_ID).initialState("DRAFT").initialState("SUBMITTED");

    assertThatThrownBy(builder::build)
        .isInstanceOf(InvalidGraphException.class)
        .hasMessageContaining("found 2")
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

  /**
   * The first of the three topologies this subsystem exists to run, and the reason {@code build()}
   * walks no graph looking for one. A Lifecycle that can return to an earlier State is ordinary:
   * {@code RETURN_FOR_RESUBMISSION} is exactly that edge in the v1 Negotiation graph, and any
   * acyclicity check - however it were spelled - would refuse production data.
   *
   * <p>Asserted as <em>acceptance</em>, so this fixture depends on no exception type and says
   * nothing about how a refusal would have been worded.
   */
  @Test
  @DisplayName("a graph whose Transitions form a cycle builds, and resolves both ways round")
  void build_whenTheTransitionsFormACycle_isAccepted() {
    CompiledGraph graph =
        CompiledGraph.builder(VERSION_ID)
            .initialState("SUBMITTED")
            .state("IN_PROGRESS")
            .transition("SUBMITTED", "APPROVE", "IN_PROGRESS", RequiredAuthority.IS_ADMIN)
            .transition(
                "IN_PROGRESS", "RETURN_FOR_RESUBMISSION", "SUBMITTED", RequiredAuthority.IS_ADMIN)
            .build();

    assertThat(graph.transition("SUBMITTED", "APPROVE"))
        .contains(
            new CompiledTransition(
                "SUBMITTED", "APPROVE", "IN_PROGRESS", RequiredAuthority.IS_ADMIN));
    assertThat(graph.transition("IN_PROGRESS", "RETURN_FOR_RESUBMISSION"))
        .contains(
            new CompiledTransition(
                "IN_PROGRESS", "RETURN_FOR_RESUBMISSION", "SUBMITTED", RequiredAuthority.IS_ADMIN));
  }

  /**
   * The second: a Legacy State, kept in the version only so that historical rows still resolve.
   * Both v1 graphs have one. What makes it useful is precisely that it is <em>declared</em> - a
   * Negotiation still sitting in it must have its State answered rather than refused, which is why
   * the assertions below are that {@code isTerminal} and {@code transitionsFrom} answer at all.
   *
   * <p>A reachability check would refuse this graph, and {@code
   * requireEveryTransitionToNameDeclaredStates} says in its own javadoc that it deliberately does
   * not make one. This is that sentence as a test.
   */
  @Test
  @DisplayName("a graph with a State no Transition reaches builds, and still declares the State")
  void build_whenNoTransitionReachesAState_isAcceptedAndTheStateStaysDeclared() {
    CompiledGraph graph =
        CompiledGraph.builder(VERSION_ID)
            .initialState("SUBMITTED")
            .terminalState("ABANDONED")
            .state("APPROVED")
            .transition("SUBMITTED", "DECLINE", "ABANDONED", RequiredAuthority.IS_ADMIN)
            .build();

    assertThat(graph.states()).contains("APPROVED");
    assertThat(graph.declaresState("APPROVED")).isTrue();
    assertThat(graph.isTerminal("APPROVED")).isFalse();
    assertThat(graph.transitionsFrom("APPROVED")).isEmpty();
  }

  /**
   * The third: a version nobody has yet given an end. Which States carry the terminal flag is seed
   * content and the migration slab's via ticket 12, so a version legitimately has none while it is
   * being authored - and the asymmetry with the initial flag is deliberate. An initial State is
   * what <em>starts</em> a Lifecycle, so {@code build()} requires exactly one; a terminal State is
   * not needed to run one, so it requires none.
   */
  @Test
  @DisplayName("a graph that declares no terminal State builds, and calls no State finished")
  void build_whenNoStateIsTerminal_isAccepted() {
    CompiledGraph graph =
        CompiledGraph.builder(VERSION_ID)
            .initialState("DRAFT")
            .state("SUBMITTED")
            .transition("DRAFT", "SUBMIT", "SUBMITTED", RequiredAuthority.IS_CREATOR)
            .build();

    assertThat(graph.states()).containsExactlyInAnyOrder("DRAFT", "SUBMITTED");
    assertThat(graph.isTerminal("DRAFT")).isFalse();
    assertThat(graph.isTerminal("SUBMITTED")).isFalse();
  }

  @Test
  @DisplayName("an edge with no Required Authority is refused at construction")
  void transition_whenGivenNullRequiredAuthority_isRefusedAtConstruction() {
    assertThatThrownBy(() -> new CompiledTransition("A", "E", "B", null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("requiredAuthority");
  }
}
