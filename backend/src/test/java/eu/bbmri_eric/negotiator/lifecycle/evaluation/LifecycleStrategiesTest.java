package eu.bbmri_eric.negotiator.lifecycle.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.lifecycle.graph.ActionContext;
import eu.bbmri_eric.negotiator.lifecycle.graph.ActionStep;
import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledGraph;
import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledTransition;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext.Caller;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext.SiblingResource;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext.Subject;
import eu.bbmri_eric.negotiator.lifecycle.graph.RequiredAuthority;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The four ported strategies. Each is exercised through its own {@code check} or {@code run} rather
 * than through the evaluator, because what is under test is the predicate or the effect; that they
 * compose into a pipeline is {@link TransitionEvaluatorTest}'s subject.
 *
 * <p>No Spring, no database and — for the two Guards — no test double at all, because neither reads
 * anything that is not on the context. That is the observable form of the claim that putting the
 * parent's State and the siblings' graphs on the context removes the last reason for the evaluator
 * to hold a repository.
 */
class LifecycleStrategiesTest {

  private static final long CREATOR = 101L;

  // --- NEGOTIATION_APPROVED ---------------------------------------------------------------------

  private final NegotiationApprovedGuard negotiationApproved = new NegotiationApprovedGuard();

  private static EvaluationContext resourceUnder(String parentState) {
    return EvaluationContext.forResource(
        Caller.person(CREATOR, Set.of()),
        Subject.resource("negotiation-1", "biobank:1:collection:1", "SUBMITTED", CREATOR, Set.of()),
        parentState);
  }

  @Test
  void negotiationApproved_whenTheParentIsInProgress_passes() {
    assertThat(negotiationApproved.check(resourceUnder("IN_PROGRESS"), NoParams.INSTANCE).passed())
        .isTrue();
  }

  /**
   * Walked over every State the Negotiation graph declares, so "the parent must be in progress" is
   * a claim about the rule rather than about one State — and the passing row is asserted so the
   * rule cannot pass by refusing everywhere.
   */
  @ParameterizedTest
  @CsvSource({
    "DRAFT,false",
    "SUBMITTED,false",
    "APPROVED,false",
    "IN_PROGRESS,true",
    "PAUSED,false",
    "DECLINED,false",
    "ABANDONED,false",
    "CONCLUDED,false"
  })
  void negotiationApproved_permitsResourceWorkOnlyWhileTheParentIsInProgress(
      String parentState, boolean expected) {
    assertThat(negotiationApproved.check(resourceUnder(parentState), NoParams.INSTANCE).passed())
        .isEqualTo(expected);
  }

  @Test
  void negotiationApproved_whenRefusing_saysWhatTheParentStateWasAndWhatItNeeded() {
    assertThat(negotiationApproved.check(resourceUnder("PAUSED"), NoParams.INSTANCE))
        .satisfies(
            verdict -> {
              assertThat(verdict.reasonCode()).isEqualTo("PARENT_NEGOTIATION_NOT_IN_PROGRESS");
              assertThat(verdict.details())
                  .containsEntry("parentNegotiationState", "PAUSED")
                  .containsEntry("requiredParentState", "IN_PROGRESS");
            });
  }

  // --- TERMINAL_AGGREGATION --------------------------------------------------------------------

  private final TerminalAggregationGuard terminalAggregation = new TerminalAggregationGuard();

  /** Two Resource definitions that disagree about which State means finished. */
  private static CompiledGraph strictFlow() {
    return CompiledGraph.builder(10L)
        .initialState("SUBMITTED")
        .terminalState("RESOURCE_MADE_AVAILABLE")
        .state("RESOURCE_NOT_MADE_AVAILABLE")
        .build();
  }

  private static CompiledGraph generousFlow() {
    return CompiledGraph.builder(20L)
        .initialState("SUBMITTED")
        .terminalState("RESOURCE_MADE_AVAILABLE")
        .terminalState("RESOURCE_NOT_MADE_AVAILABLE")
        .build();
  }

  private static EvaluationContext negotiationOver(SiblingResource... resources) {
    return EvaluationContext.forNegotiation(
        Caller.system(),
        Subject.negotiation("negotiation-1", "IN_PROGRESS", CREATOR),
        List.of(resources));
  }

  @Test
  void terminalAggregation_whenEveryResourceIsInATerminalState_passes() {
    EvaluationContext context =
        negotiationOver(
            new SiblingResource(1L, "RESOURCE_MADE_AVAILABLE", strictFlow()),
            new SiblingResource(2L, "RESOURCE_MADE_AVAILABLE", generousFlow()));

    assertThat(terminalAggregation.check(context, NoParams.INSTANCE).passed()).isTrue();
  }

  @Test
  void terminalAggregation_whenOneResourceIsStillRunning_refusesAndNamesIt() {
    EvaluationContext context =
        negotiationOver(
            new SiblingResource(1L, "RESOURCE_MADE_AVAILABLE", strictFlow()),
            new SiblingResource(2L, "SUBMITTED", strictFlow()));

    assertThat(terminalAggregation.check(context, NoParams.INSTANCE))
        .satisfies(
            verdict -> {
              assertThat(verdict.passed()).isFalse();
              assertThat(verdict.reasonCode()).isEqualTo("RESOURCES_STILL_RUNNING");
              assertThat(verdict.details())
                  .containsEntry("unfinishedResourceIds", List.of(2L))
                  .containsEntry("unfinishedCount", 1)
                  .containsEntry("resourceCount", 2);
            });
  }

  /**
   * The whole reason terminality is a question rather than a list. Both Resources are in the same
   * State; one definition calls it finished and the other does not, so the answer depends on which
   * version is pinned to which Resource and on nothing else.
   */
  @Test
  void terminalAggregation_asksEachResourcesOwnPinnedVersion() {
    SiblingResource underStrict =
        new SiblingResource(1L, "RESOURCE_NOT_MADE_AVAILABLE", strictFlow());
    SiblingResource underGenerous =
        new SiblingResource(2L, "RESOURCE_NOT_MADE_AVAILABLE", generousFlow());

    assertThat(underStrict.isFinished()).isFalse();
    assertThat(underGenerous.isFinished()).isTrue();
    assertThat(
            terminalAggregation.check(negotiationOver(underGenerous), NoParams.INSTANCE).passed())
        .isTrue();
    assertThat(terminalAggregation.check(negotiationOver(underStrict), NoParams.INSTANCE).passed())
        .isFalse();
  }

  /** The vacuous reading of "every", left vacuous deliberately. */
  @Test
  void terminalAggregation_whenTheNegotiationHasNoResources_passes() {
    assertThat(terminalAggregation.check(negotiationOver(), NoParams.INSTANCE).passed()).isTrue();
  }

  // --- SET_POST_VISIBILITY ----------------------------------------------------------------------

  private final RecordingPostVisibility posts = new RecordingPostVisibility();

  private static ActionContext committing(String event) {
    return new ActionContext(
        EvaluationContext.forNegotiation(
            Caller.person(CREATOR, Set.of()),
            Subject.negotiation("negotiation-1", "DRAFT", CREATOR),
            List.of()),
        new CompiledTransition("DRAFT", event, "SUBMITTED", RequiredAuthority.IS_CREATOR));
  }

  /**
   * The three dump Actions, one Wiring row each. {@code DisablePostsAction} touches both flags,
   * which is why {@code BOTH} exists: with only {@code PUBLIC} and {@code PRIVATE} the abandon
   * Transition would need two rows and three Actions would become four.
   */
  @ParameterizedTest
  @CsvSource({
    "PUBLIC,true,'public=true'",
    "PRIVATE,true,'private=true'",
    "BOTH,false,'public=false,private=false'"
  })
  void setPostVisibility_reproducesEachDumpActionFromOneWiringRow(
      String scope, boolean enabled, String expected) {
    SetPostVisibilityAction action = new SetPostVisibilityAction(posts);

    action.run(
        committing("SUBMIT"),
        new SetPostVisibilityAction.Params(SetPostVisibilityAction.Scope.valueOf(scope), enabled));

    assertThat(String.join(",", posts.calls)).isEqualTo(expected);
  }

  @Test
  void setPostVisibility_readsItsScopeAndFlagOutOfTheWiringRowsJson() throws Exception {
    ActionRegistry registry =
        new ActionRegistry(new ObjectMapper(), List.of(new SetPostVisibilityAction(posts)));

    ActionStep step =
        registry.bind(SetPostVisibilityAction.TYPE_KEY, "{\"scope\":\"BOTH\",\"enabled\":false}");
    step.run(committing("ABANDON"));

    assertThat(step.typeKey()).isEqualTo("SET_POST_VISIBILITY");
    assertThat(posts.calls).containsExactly("public=false", "private=false");
  }

  /**
   * One bean, three rows. Three beans would all declare this key and collide, which is why the
   * {@code DefaultWebhookMappingStrategy} pattern — one class configured N times — does not
   * transfer here: there, each configured bean declares a different key.
   */
  @Test
  void setPostVisibility_configuredAsThreeBeans_wouldFailTheBoot() {
    ObjectMapper mapper = new ObjectMapper();
    List<Action<?>> threeBeans =
        List.of(
            new SetPostVisibilityAction(posts),
            new SetPostVisibilityAction(posts),
            new SetPostVisibilityAction(posts));

    assertThatThrownBy(() -> new ActionRegistry(mapper, threeBeans))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("SET_POST_VISIBILITY");
  }

  // --- SPAWN_RESOURCE_LIFECYCLES ---------------------------------------------------------------

  @Test
  void spawnResourceLifecycles_isRegisteredSoAWiringRowCanNameIt() {
    ActionRegistry registry =
        new ActionRegistry(new ObjectMapper(), List.of(new SpawnResourceLifecyclesAction()));

    assertThat(registry.typeKeys()).containsExactly("SPAWN_RESOURCE_LIFECYCLES");
    assertThat(registry.bind("SPAWN_RESOURCE_LIFECYCLES", null).typeKey())
        .isEqualTo("SPAWN_RESOURCE_LIFECYCLES");
  }

  /** It writes, and this slab excludes everything that writes. Refusing beats doing nothing. */
  @Test
  void spawnResourceLifecycles_refusesToRun() {
    assertThatThrownBy(
            () -> new SpawnResourceLifecyclesAction().run(committing("APPROVE"), NoParams.INSTANCE))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessageContaining("not built yet")
        .hasMessageContaining("negotiation-1");
  }

  private static final class RecordingPostVisibility implements PostVisibility {

    private final List<String> calls = new ArrayList<>();

    @Override
    public void setPublicPostsEnabled(String negotiationId, boolean enabled) {
      calls.add("public=" + enabled);
    }

    @Override
    public void setPrivatePostsEnabled(String negotiationId, boolean enabled) {
      calls.add("private=" + enabled);
    }
  }
}
