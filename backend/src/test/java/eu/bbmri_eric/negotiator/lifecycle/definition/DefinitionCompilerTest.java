package eu.bbmri_eric.negotiator.lifecycle.definition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.bbmri_eric.negotiator.lifecycle.graph.ActionCatalogue;
import eu.bbmri_eric.negotiator.lifecycle.graph.ActionContext;
import eu.bbmri_eric.negotiator.lifecycle.graph.ActionStep;
import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledGraph;
import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledTransition;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardCatalogue;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardStep;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardVerdict;
import eu.bbmri_eric.negotiator.lifecycle.graph.RequiredAuthority;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Compilation, with no database and no Spring. That this test can exist is the argument for handing
 * the compiler {@link DefinitionVersionRows} rather than six repositories: the rows are built here
 * in memory, and none of them is ever persisted or even given a row id.
 *
 * <p>The Guard catalogue is a local fake rather than the real registry, because what is under test
 * is the fold — which rows become which chain, in which order — and not what a strategy does. The
 * fake records the keys it was asked for, so the order of the effective chain is observable without
 * running a single Guard.
 *
 * <p>The builders here are local rather than in {@link DefinitionFixtures} on that class's own
 * stated criterion: the repository tests hold the Required Authority and the params column fixed
 * while varying the table under test, and this test does the opposite. Sharing a builder that
 * varies both would make each caller's subject less clear, which is exactly the argument that keeps
 * {@code versionBuilder} out of there.
 */
class DefinitionCompilerTest {

  private static final long VERSION_ID = 42L;

  private final RecordingCatalogue catalogue = new RecordingCatalogue();
  private final RecordingActionCatalogue actionCatalogue = new RecordingActionCatalogue();
  private final DefinitionCompiler compiler = new DefinitionCompiler(catalogue, actionCatalogue);

  private final LifecycleDefinition version =
      LifecycleDefinition.builder()
          .id(VERSION_ID)
          .scope(DefinitionScope.RESOURCE)
          .familyKey("standard-resource-flow")
          .name("Standard flow")
          .version(1)
          .active(true)
          .build();

  private final State submitted =
      State.builder()
          .lifecycleDefinition(version)
          .name("SUBMITTED")
          .label("Submitted")
          .initial(true)
          .build();
  private final State available =
      State.builder()
          .lifecycleDefinition(version)
          .name("RESOURCE_MADE_AVAILABLE")
          .label("Made available")
          .terminal(true)
          .build();
  private final State legacy =
      State.builder()
          .lifecycleDefinition(version)
          .name("RETURNED_FOR_RESUBMISSION")
          .label("Returned")
          .build();

  private final Event deliver = eventNamed("DELIVER");
  private final Event override = eventNamed("OVERRIDE");

  private final Transition deliverTransition =
      transition(submitted, deliver, available, RequiredAuthority.IS_REPRESENTATIVE);

  private Event eventNamed(String name) {
    return Event.builder().lifecycleDefinition(version).name(name).build();
  }

  private Transition transition(
      State from, Event event, State to, RequiredAuthority requiredAuthority) {
    return Transition.builder()
        .lifecycleDefinition(version)
        .fromState(from)
        .event(event)
        .toState(to)
        .requiredAuthority(requiredAuthority)
        .build();
  }

  private GuardWiring definitionWide(String typeKey, int sortOrder, String params) {
    return GuardWiring.builder()
        .lifecycleDefinition(version)
        .transition(null)
        .typeKey(typeKey)
        .params(params)
        .sortOrder(sortOrder)
        .build();
  }

  private GuardWiring onTransition(
      Transition transition, String typeKey, int sortOrder, String params) {
    return GuardWiring.builder()
        .lifecycleDefinition(version)
        .transition(transition)
        .typeKey(typeKey)
        .params(params)
        .sortOrder(sortOrder)
        .build();
  }

  private ActionWiring actionOn(
      Transition transition, String typeKey, int sortOrder, String params) {
    return ActionWiring.builder()
        .transition(transition)
        .typeKey(typeKey)
        .params(params)
        .sortOrder(sortOrder)
        .build();
  }

  private DefinitionVersionRows rows(List<Transition> transitions, List<GuardWiring> wirings) {
    return rows(transitions, wirings, List.of());
  }

  private DefinitionVersionRows rows(
      List<Transition> transitions, List<GuardWiring> wirings, List<ActionWiring> actions) {
    return new DefinitionVersionRows(
        version,
        List.of(submitted, available, legacy),
        List.of(deliver, override),
        transitions,
        wirings,
        actions);
  }

  @Test
  void compile_takesTheDefinitionVersionsRowIdAsTheGraphsIdentity() {
    CompiledGraph graph = compiler.compile(rows(List.of(deliverTransition), List.of()));

    assertThat(graph.definitionVersionId()).isEqualTo(VERSION_ID);
  }

  @Test
  void compile_carriesTheStateFlagsAcrossAsTheyAreDeclared() {
    CompiledGraph graph = compiler.compile(rows(List.of(deliverTransition), List.of()));

    assertThat(graph.initialState()).isEqualTo("SUBMITTED");
    assertThat(graph.isTerminal("RESOURCE_MADE_AVAILABLE")).isTrue();
    assertThat(graph.isTerminal("SUBMITTED")).isFalse();
  }

  /**
   * A Legacy State is a row like any other, and must survive compilation to keep old data valid.
   */
  @Test
  void compile_keepsAStateNoTransitionTouches() {
    CompiledGraph graph = compiler.compile(rows(List.of(deliverTransition), List.of()));

    assertThat(graph.declaresState("RETURNED_FOR_RESUBMISSION")).isTrue();
    assertThat(graph.transitionsFrom("RETURNED_FOR_RESUBMISSION")).isEmpty();
  }

  @Test
  void compile_keepsAnEventNoTransitionUses() {
    CompiledGraph graph = compiler.compile(rows(List.of(deliverTransition), List.of()));

    assertThat(graph.declaresEvent("OVERRIDE")).isTrue();
    assertThat(graph.transition("SUBMITTED", "OVERRIDE")).isEmpty();
  }

  @Test
  void compile_carriesTheRequiredAuthorityOntoTheEdge() {
    CompiledGraph graph = compiler.compile(rows(List.of(deliverTransition), List.of()));

    assertThat(graph.transition("SUBMITTED", "DELIVER"))
        .get()
        .extracting(CompiledTransition::requiredAuthority)
        .isEqualTo(RequiredAuthority.IS_REPRESENTATIVE);
  }

  /**
   * ADR 0005's order, and the reason the fold happens here: the evaluator gets one chain and never
   * learns that Guards have two scopes.
   */
  @Test
  void compile_putsDefinitionWideGuardsAheadOfTheTransitionsOwn() {
    CompiledGraph graph =
        compiler.compile(
            rows(
                List.of(deliverTransition),
                List.of(
                    onTransition(deliverTransition, "TRANSITION_SECOND", 2, null),
                    definitionWide("DEFINITION_FIRST", 1, null),
                    onTransition(deliverTransition, "TRANSITION_FIRST", 1, null),
                    definitionWide("DEFINITION_SECOND", 2, null))));

    assertThat(chainOf(graph, "SUBMITTED", "DELIVER"))
        .containsExactly(
            "DEFINITION_FIRST", "DEFINITION_SECOND", "TRANSITION_FIRST", "TRANSITION_SECOND");
  }

  @Test
  void compile_ordersEachScopeByItsOwnSortOrderRatherThanByRowOrder() {
    CompiledGraph graph =
        compiler.compile(
            rows(
                List.of(deliverTransition),
                List.of(
                    definitionWide("THIRD", 30, null),
                    definitionWide("FIRST", 10, null),
                    definitionWide("SECOND", 20, null))));

    assertThat(chainOf(graph, "SUBMITTED", "DELIVER")).containsExactly("FIRST", "SECOND", "THIRD");
  }

  /**
   * The two scopes have independent {@code sort_order} sequences — two partial unique indexes, not
   * one — so the same number on both sides is a definition the schema accepts, and the fold must
   * not interleave them.
   */
  @Test
  void compile_whenBothScopesUseTheSameSortOrder_stillPutsTheDefinitionWideOneFirst() {
    CompiledGraph graph =
        compiler.compile(
            rows(
                List.of(deliverTransition),
                List.of(
                    onTransition(deliverTransition, "TRANSITION_SCOPED", 1, null),
                    definitionWide("DEFINITION_WIDE", 1, null))));

    assertThat(chainOf(graph, "SUBMITTED", "DELIVER"))
        .containsExactly("DEFINITION_WIDE", "TRANSITION_SCOPED");
  }

  /** There is no uniqueness on {@code type_key}: one definition may wire a key twice. */
  @Test
  void compile_whenOneTypeKeyIsWiredTwiceAtDifferentSortOrders_keepsBoth() {
    CompiledGraph graph =
        compiler.compile(
            rows(
                List.of(deliverTransition),
                List.of(definitionWide("TWICE", 1, null), definitionWide("TWICE", 2, null))));

    assertThat(chainOf(graph, "SUBMITTED", "DELIVER")).containsExactly("TWICE", "TWICE");
  }

  /**
   * The definition-wide Guard applies to every Transition of the version. This is what removes the
   * copy-drift risk of re-attaching it to each Transition a later version adds — the second edge
   * gets it without anybody wiring it.
   */
  @Test
  void compile_appliesADefinitionWideGuardToEveryTransition() {
    Transition second = transition(submitted, override, legacy, RequiredAuthority.IS_ADMIN);
    CompiledGraph graph =
        compiler.compile(
            rows(
                List.of(deliverTransition, second),
                List.of(definitionWide("NEGOTIATION_APPROVED", 1, null))));

    assertThat(chainOf(graph, "SUBMITTED", "DELIVER")).containsExactly("NEGOTIATION_APPROVED");
    assertThat(chainOf(graph, "SUBMITTED", "OVERRIDE")).containsExactly("NEGOTIATION_APPROVED");
  }

  @Test
  void compile_keepsATransitionScopedGuardOffEveryOtherTransition() {
    Transition second = transition(submitted, override, legacy, RequiredAuthority.IS_ADMIN);
    CompiledGraph graph =
        compiler.compile(
            rows(
                List.of(deliverTransition, second),
                List.of(onTransition(deliverTransition, "ONLY_HERE", 1, null))));

    assertThat(chainOf(graph, "SUBMITTED", "DELIVER")).containsExactly("ONLY_HERE");
    assertThat(chainOf(graph, "SUBMITTED", "OVERRIDE")).isEmpty();
  }

  @Test
  void compile_handsEachWiringsRawParamsToTheCatalogueOnce() {
    compiler.compile(
        rows(
            List.of(deliverTransition),
            List.of(definitionWide("SET_POST_VISIBILITY", 1, "{\"scope\":\"BOTH\"}"))));

    assertThat(catalogue.bound)
        .containsExactly(new Bound("SET_POST_VISIBILITY", "{\"scope\":\"BOTH\"}"));
  }

  /**
   * Bound once and shared, not once per Transition. A step closes over configuration and nothing
   * else, so the same instance is safe on every edge — and parsing one row's jsonb N times has no
   * reader.
   */
  @Test
  void compile_bindsADefinitionWideWiringOnceHoweverManyTransitionsCarryIt() {
    Transition second = transition(submitted, override, legacy, RequiredAuthority.IS_ADMIN);

    compiler.compile(
        rows(
            List.of(deliverTransition, second),
            List.of(definitionWide("NEGOTIATION_APPROVED", 1, null))));

    assertThat(catalogue.bound).hasSize(1);
  }

  @Test
  void compile_whenTheCatalogueRefusesAKey_failsTheCompileRatherThanTheFiring() {
    assertThatThrownBy(
            () ->
                compiler.compile(
                    rows(List.of(deliverTransition), List.of(definitionWide("NO_SUCH", 1, null)))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("NO_SUCH");
  }

  @Test
  void compile_whenARowBelongsToAnotherDefinitionVersion_isRefused() {
    LifecycleDefinition other =
        LifecycleDefinition.builder()
            .id(99L)
            .scope(DefinitionScope.RESOURCE)
            .familyKey("other")
            .name("Other")
            .version(1)
            .build();
    State foreign =
        State.builder().lifecycleDefinition(other).name("ELSEWHERE").label("Elsewhere").build();

    DefinitionVersionRows mixed =
        new DefinitionVersionRows(
            version,
            List.of(submitted, foreign),
            List.of(deliver),
            List.of(deliverTransition),
            List.of(),
            List.of());

    assertThatThrownBy(() -> compiler.compile(mixed))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("different Definition Version")
        .hasMessageContaining("ELSEWHERE");
  }

  // --- the Action chain -------------------------------------------------------------------------

  @Test
  void compile_putsTheTransitionsActionsOnItsEdgeInSortOrder() {
    CompiledGraph graph =
        compiler.compile(
            rows(
                List.of(deliverTransition),
                List.of(),
                List.of(
                    actionOn(deliverTransition, "SECOND", 20, null),
                    actionOn(deliverTransition, "FIRST", 10, null))));

    assertThat(actionsOf(graph, "SUBMITTED", "DELIVER")).containsExactly("FIRST", "SECOND");
  }

  /** Action wiring is transition-scoped only; there is no definition-wide chain to fold in. */
  @Test
  void compile_keepsATransitionsActionsOffEveryOtherTransition() {
    Transition second = transition(submitted, override, legacy, RequiredAuthority.IS_ADMIN);
    CompiledGraph graph =
        compiler.compile(
            rows(
                List.of(deliverTransition, second),
                List.of(),
                List.of(actionOn(deliverTransition, "SET_POST_VISIBILITY", 1, null))));

    assertThat(actionsOf(graph, "SUBMITTED", "DELIVER")).containsExactly("SET_POST_VISIBILITY");
    assertThat(actionsOf(graph, "SUBMITTED", "OVERRIDE")).isEmpty();
  }

  @Test
  void compile_handsAnActionsRawParamsToItsOwnCatalogue() {
    compiler.compile(
        rows(
            List.of(deliverTransition),
            List.of(),
            List.of(
                actionOn(
                    deliverTransition,
                    "SET_POST_VISIBILITY",
                    1,
                    "{\"scope\":\"BOTH\",\"enabled\":false}"))));

    assertThat(actionCatalogue.bound)
        .containsExactly(
            new Bound("SET_POST_VISIBILITY", "{\"scope\":\"BOTH\",\"enabled\":false}"));
    assertThat(catalogue.bound).isEmpty();
  }

  /**
   * The two catalogues are separate key spaces. An Action key wired where a Guard belongs is
   * refused at compile time rather than dispatched to the wrong strategy.
   */
  @Test
  void compile_whenAnActionKeyIsWiredAsAGuard_isRefused() {
    assertThatThrownBy(
            () ->
                compiler.compile(
                    rows(List.of(deliverTransition), List.of(definitionWide("NO_SUCH", 1, null)))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("NO_SUCH");
  }

  private static List<String> actionsOf(CompiledGraph graph, String fromState, String event) {
    return graph.transition(fromState, event).orElseThrow().actions().stream()
        .map(ActionStep::typeKey)
        .toList();
  }

  private static List<String> chainOf(CompiledGraph graph, String fromState, String event) {
    return graph.transition(fromState, event).orElseThrow().guards().stream()
        .map(GuardStep::typeKey)
        .toList();
  }

  private record Bound(String typeKey, String paramsJson) {}

  /**
   * A catalogue that binds anything whose key is not {@code NO_SUCH}, and remembers what it was
   * asked. Standing in for the registry keeps this test about the fold rather than about Jackson.
   */
  private static final class RecordingCatalogue implements GuardCatalogue {

    private final List<Bound> bound = new ArrayList<>();

    @Override
    public GuardStep bind(String typeKey, String paramsJson) {
      if ("NO_SUCH".equals(typeKey)) {
        throw new IllegalArgumentException("No Guard strategy declares the type key 'NO_SUCH'.");
      }
      bound.add(new Bound(typeKey, paramsJson));
      return new GuardStep() {
        @Override
        public String typeKey() {
          return typeKey;
        }

        @Override
        public GuardVerdict check(EvaluationContext context) {
          return GuardVerdict.pass();
        }
      };
    }
  }

  /** The Action-side twin of {@link RecordingCatalogue}, over its own key space. */
  private static final class RecordingActionCatalogue implements ActionCatalogue {

    private final List<Bound> bound = new ArrayList<>();

    @Override
    public ActionStep bind(String typeKey, String paramsJson) {
      if ("NO_SUCH".equals(typeKey)) {
        throw new IllegalArgumentException("No Action strategy declares the type key 'NO_SUCH'.");
      }
      bound.add(new Bound(typeKey, paramsJson));
      return new ActionStep() {
        @Override
        public String typeKey() {
          return typeKey;
        }

        @Override
        public void run(ActionContext context) {
          throw new UnsupportedOperationException("the fake does not run");
        }
      };
    }
  }
}
