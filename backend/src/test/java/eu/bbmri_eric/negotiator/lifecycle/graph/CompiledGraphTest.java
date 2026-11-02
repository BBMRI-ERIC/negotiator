package eu.bbmri_eric.negotiator.lifecycle.graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Set;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The Compiled Graph contract, tested without Spring, a database or the definition package. A graph
 * is built from names and flags, so a test can build the exact shape it needs.
 *
 * <p>The fixture is a small copy of the v1 Negotiation graph with its two unusual structures: {@code
 * DRAFT}, which Transitions leave but none enters, and {@code APPROVED}, a Legacy State no
 * Transition touches.
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
        .transition("DRAFT", "SUBMIT", "SUBMITTED", Set.of(RequiredAuthority.IS_CREATOR))
        .transition("SUBMITTED", "APPROVE", "IN_PROGRESS", Set.of(RequiredAuthority.IS_ADMIN))
        .transition("SUBMITTED", "DECLINE", "ABANDONED", Set.of(RequiredAuthority.IS_ADMIN))
        .transition(
            "IN_PROGRESS",
            "ABANDON",
            "ABANDONED",
            Set.of(RequiredAuthority.IS_ADMIN, RequiredAuthority.IS_CREATOR))
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
                "SUBMITTED", "APPROVE", "IN_PROGRESS", Set.of(RequiredAuthority.IS_ADMIN)));
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
   * Events without a Transition are ordinary. Both v1 graphs have them, and the Override Event is
   * one.
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
   * For terminality, a Guard that read an unknown State as "still running" would leave a
   * Negotiation in progress for ever. For the other two lookups, an empty answer looks like a
   * terminal State, so a broken Definition Version Pin would reach a requester as "nothing is
   * available", which an empty listing must never mean.
   *
   * <p>Each refusal names the State asked about, the Definition Version, and a State the version
   * does declare, so a log line is enough to act on.
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

  /** The initial and terminal flags are independent columns. */
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
            .transition("NOWHERE", "SUBMIT", "SUBMITTED", Set.of(RequiredAuthority.IS_CREATOR));

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
            .transition("SUBMITTED", "APPROVE", "NOWHERE", Set.of(RequiredAuthority.IS_ADMIN));

    assertThatThrownBy(builder::build)
        .isInstanceOf(InvalidGraphException.class)
        .hasMessageContaining("NOWHERE");
  }

  /**
   * The schema enforces this too, in {@code uq_transition_definition_source_event}, but a graph can
   * be built without the database. Branching that a Guard selects is modelled as two distinct Events
   * instead.
   */
  @Test
  @DisplayName("two Transitions leaving one State on one Event are refused")
  void build_whenTwoTransitionsShareASourceAndEvent_isRefused() {
    CompiledGraph.Builder builder =
        CompiledGraph.builder(VERSION_ID)
            .initialState("SUBMITTED")
            .state("IN_PROGRESS")
            .state("ABANDONED")
            .transition("SUBMITTED", "APPROVE", "IN_PROGRESS", Set.of(RequiredAuthority.IS_ADMIN))
            .transition("SUBMITTED", "APPROVE", "ABANDONED", Set.of(RequiredAuthority.IS_ADMIN));

    assertThatThrownBy(builder::build)
        .isInstanceOf(InvalidGraphException.class)
        .hasMessageContaining("SUBMITTED")
        .hasMessageContaining("APPROVE")
        .hasMessageContaining("IN_PROGRESS")
        .hasMessageContaining("ABANDONED");
  }

  @Test
  @DisplayName("one Event may leave two different States, which is the ordinary case")
  void build_whenOneEventLeavesTwoDifferentStates_isAccepted() {
    CompiledGraph graph =
        CompiledGraph.builder(VERSION_ID)
            .initialState("IN_PROGRESS")
            .state("PAUSED")
            .terminalState("ABANDONED")
            .transition(
                "IN_PROGRESS",
                "ABANDON",
                "ABANDONED",
                Set.of(RequiredAuthority.IS_ADMIN, RequiredAuthority.IS_CREATOR))
            .transition(
                "PAUSED",
                "ABANDON",
                "ABANDONED",
                Set.of(RequiredAuthority.IS_ADMIN, RequiredAuthority.IS_CREATOR))
            .build();

    assertThat(graph.transition("IN_PROGRESS", "ABANDON")).isPresent();
    assertThat(graph.transition("PAUSED", "ABANDON")).isPresent();
  }

  /**
   * {@code RETURN_FOR_RESUBMISSION} in the v1 Negotiation graph returns to an earlier State, so any
   * acyclicity check in {@code build()} would refuse production data.
   */
  @Test
  @DisplayName("a graph whose Transitions form a cycle builds, and resolves both ways round")
  void build_whenTheTransitionsFormACycle_isAccepted() {
    CompiledGraph graph =
        CompiledGraph.builder(VERSION_ID)
            .initialState("SUBMITTED")
            .state("IN_PROGRESS")
            .transition("SUBMITTED", "APPROVE", "IN_PROGRESS", Set.of(RequiredAuthority.IS_ADMIN))
            .transition(
                "IN_PROGRESS",
                "RETURN_FOR_RESUBMISSION",
                "SUBMITTED",
                Set.of(RequiredAuthority.IS_ADMIN))
            .build();

    assertThat(graph.transition("SUBMITTED", "APPROVE"))
        .contains(
            new CompiledTransition(
                "SUBMITTED", "APPROVE", "IN_PROGRESS", Set.of(RequiredAuthority.IS_ADMIN)));
    assertThat(graph.transition("IN_PROGRESS", "RETURN_FOR_RESUBMISSION"))
        .contains(
            new CompiledTransition(
                "IN_PROGRESS",
                "RETURN_FOR_RESUBMISSION",
                "SUBMITTED",
                Set.of(RequiredAuthority.IS_ADMIN)));
  }

  /**
   * A Legacy State stays declared so that historical rows still resolve, and both v1 graphs have
   * one. A Negotiation still in it must get answers from {@code isTerminal} and {@code
   * transitionsFrom}, so {@code build()} must not check reachability.
   */
  @Test
  @DisplayName("a graph with a State no Transition reaches builds, and still declares the State")
  void build_whenNoTransitionReachesAState_isAcceptedAndTheStateStaysDeclared() {
    CompiledGraph graph =
        CompiledGraph.builder(VERSION_ID)
            .initialState("SUBMITTED")
            .terminalState("ABANDONED")
            .state("APPROVED")
            .transition("SUBMITTED", "DECLINE", "ABANDONED", Set.of(RequiredAuthority.IS_ADMIN))
            .build();

    assertThat(graph.states()).contains("APPROVED");
    assertThat(graph.declaresState("APPROVED")).isTrue();
    assertThat(graph.isTerminal("APPROVED")).isFalse();
    assertThat(graph.transitionsFrom("APPROVED")).isEmpty();
  }

  /**
   * A version being authored may have no terminal State yet. A Lifecycle needs an initial State to
   * start, so {@code build()} requires exactly one, but it needs no terminal State to run.
   */
  @Test
  @DisplayName("a graph that declares no terminal State builds, and calls no State finished")
  void build_whenNoStateIsTerminal_isAccepted() {
    CompiledGraph graph =
        CompiledGraph.builder(VERSION_ID)
            .initialState("DRAFT")
            .state("SUBMITTED")
            .transition("DRAFT", "SUBMIT", "SUBMITTED", Set.of(RequiredAuthority.IS_CREATOR))
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

  @Test
  @DisplayName("an empty Required Authority set builds, and the edge keeps it empty")
  void build_whenAnEdgeRequiresNoAuthority_keepsTheEmptySet() {
    CompiledGraph graph =
        CompiledGraph.builder(VERSION_ID)
            .initialState("OPEN")
            .state("MOVED")
            .transition("OPEN", "GO", "MOVED", Set.of())
            .build();

    assertThat(graph.transition("OPEN", "GO"))
        .hasValueSatisfying(edge -> assertThat(edge.requiredAuthority()).isEmpty());
  }

  /**
   * {@code SYSTEM} beside a person value would make an Event both system-fired and offered to a
   * person. The Transition itself refuses it, so no graph can hold one however it was built.
   */
  @ParameterizedTest
  @EnumSource(
      value = RequiredAuthority.class,
      names = {"SYSTEM"},
      mode = EnumSource.Mode.EXCLUDE)
  @DisplayName("SYSTEM beside any other value is refused at construction")
  void transition_whenSystemSharesASet_isRefusedAtConstruction(RequiredAuthority other) {
    assertThatThrownBy(
            () -> new CompiledTransition("A", "E", "B", Set.of(RequiredAuthority.SYSTEM, other)))
        .isInstanceOf(InvalidGraphException.class)
        .hasMessageContaining("SYSTEM must stand alone");
  }

  @Test
  @DisplayName("a set of person authorities builds, and the edge carries the whole set")
  void build_whenAnEdgeRequiresSeveralPersonAuthorities_keepsTheSet() {
    CompiledGraph graph =
        CompiledGraph.builder(VERSION_ID)
            .initialState("DRAFT")
            .state("SUBMITTED")
            .transition(
                "DRAFT",
                "SUBMIT",
                "SUBMITTED",
                Set.of(RequiredAuthority.IS_ADMIN, RequiredAuthority.IS_CREATOR))
            .build();

    assertThat(graph.transition("DRAFT", "SUBMIT"))
        .get()
        .extracting(CompiledTransition::requiredAuthority)
        .isEqualTo(Set.of(RequiredAuthority.IS_ADMIN, RequiredAuthority.IS_CREATOR));
  }
}
