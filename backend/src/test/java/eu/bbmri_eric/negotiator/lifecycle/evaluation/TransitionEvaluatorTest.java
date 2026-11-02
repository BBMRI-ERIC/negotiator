package eu.bbmri_eric.negotiator.lifecycle.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.bbmri_eric.negotiator.lifecycle.graph.ActionContext;
import eu.bbmri_eric.negotiator.lifecycle.graph.ActionStep;
import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledGraph;
import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledTransition;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext.Caller;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext.Subject;
import eu.bbmri_eric.negotiator.lifecycle.graph.FailureCategory;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardStep;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardVerdict;
import eu.bbmri_eric.negotiator.lifecycle.graph.InvalidGraphException;
import eu.bbmri_eric.negotiator.lifecycle.graph.RequiredAuthority;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The whole Evaluation Pipeline: Required Authority, the Information Requirement stage, then the
 * Guard chain. It runs without Spring or a database. The only stub is the Information Requirement
 * port, because it is the only part that reads data.
 *
 * <p>The graph puts all four Required Authority values and an empty set on Transitions leaving one
 * State, so {@link #possibleEvents_offersExactlyWhatEvaluateWouldPermit} can compare both entry
 * points over every Transition from one context.
 *
 * <p>Three tests cover the pipeline's order: a caller who fails two stages is told about the
 * earlier one, so the failure category a caller sees stays stable.
 */
class TransitionEvaluatorTest {

  private static final long VERSION_ID = 1L;
  private static final long CREATOR = 101L;
  private static final long REPRESENTATIVE = 108L;
  private static final long STRANGER = 999L;

  /** A State this graph does not declare, which stands in for a broken Definition Version Pin. */
  private static final String UNDECLARED_STATE = "RESOURCE_MADE_AVAILABLE";

  private final StubRequirements requirements = new StubRequirements();
  private final TransitionEvaluator evaluator = new TransitionEvaluator(requirements);

  private static CompiledGraph graph() {
    return CompiledGraph.builder(VERSION_ID)
        .initialState("OPEN")
        .state("BY_ANYONE")
        .state("BY_ADMIN")
        .state("BY_CREATOR")
        .state("BY_REPRESENTATIVE")
        .state("BY_SYSTEM")
        .transition("OPEN", "ANYONE", "BY_ANYONE", Set.of())
        .transition("OPEN", "ADMIN_ONLY", "BY_ADMIN", Set.of(RequiredAuthority.IS_ADMIN))
        .transition("OPEN", "CREATOR_ONLY", "BY_CREATOR", Set.of(RequiredAuthority.IS_CREATOR))
        .transition(
            "OPEN", "REP_ONLY", "BY_REPRESENTATIVE", Set.of(RequiredAuthority.IS_REPRESENTATIVE))
        .transition("OPEN", "CONCLUDE", "BY_SYSTEM", Set.of(RequiredAuthority.SYSTEM))
        .eventWithoutTransition("OVERRIDE")
        .build();
  }

  private static EvaluationContext contextFor(Caller caller) {
    return contextIn("OPEN", caller);
  }

  private static EvaluationContext contextIn(String currentState, Caller caller) {
    return EvaluationContext.forResource(
        caller,
        Subject.resource(
            "negotiation-1",
            "biobank:1:collection:1",
            currentState,
            CREATOR,
            Set.of(REPRESENTATIVE)),
        "IN_PROGRESS");
  }

  private static Caller admin() {
    return Caller.person(STRANGER, Set.of("ROLE_ADMIN"));
  }

  private static Caller creator() {
    return Caller.person(CREATOR, Set.of());
  }

  private static Caller representative() {
    return Caller.person(REPRESENTATIVE, Set.of());
  }

  private static Caller stranger() {
    return Caller.person(STRANGER, Set.of());
  }

  @Test
  @DisplayName("an Event with an empty Required Authority is permitted to any person")
  void evaluate_whenTheAuthorityIsEmpty_permitsAnyPerson() {
    assertThat(evaluator.evaluate(graph(), "ANYONE", contextFor(stranger())))
        .isEqualTo(
            new EvaluationOutcome.Permitted(
                new CompiledTransition("OPEN", "ANYONE", "BY_ANYONE", Set.of())));
  }

  @Test
  @DisplayName("IS_ADMIN reads the granted ROLE_ADMIN authority rather than who the caller is")
  void evaluate_whenTheAuthorityIsAdmin_readsTheGrantedAuthorityAndNotTheCallersIdentity() {
    assertThat(evaluator.evaluate(graph(), "ADMIN_ONLY", contextFor(admin())).permitted()).isTrue();
    assertThat(evaluator.evaluate(graph(), "ADMIN_ONLY", contextFor(creator())).permitted())
        .isFalse();
  }

  @Test
  @DisplayName("IS_CREATOR permits the Negotiation's creator and nobody else")
  void evaluate_whenTheAuthorityIsCreator_permitsOnlyTheNegotiationsCreator() {
    assertThat(evaluator.evaluate(graph(), "CREATOR_ONLY", contextFor(creator())).permitted())
        .isTrue();
    assertThat(
            evaluator.evaluate(graph(), "CREATOR_ONLY", contextFor(representative())).permitted())
        .isFalse();
  }

  @Test
  @DisplayName("IS_REPRESENTATIVE permits a representative of that Resource and nobody else")
  void evaluate_whenTheAuthorityIsRepresentative_permitsOnlyARepresentativeOfThatResource() {
    assertThat(evaluator.evaluate(graph(), "REP_ONLY", contextFor(representative())).permitted())
        .isTrue();
    assertThat(evaluator.evaluate(graph(), "REP_ONLY", contextFor(stranger())).permitted())
        .isFalse();
  }

  @Test
  @DisplayName("being an administrator does not make a caller a representative")
  void evaluate_whenTheAuthorityIsRepresentative_isNotSatisfiedByBeingAnAdmin() {
    assertThat(evaluator.evaluate(graph(), "REP_ONLY", contextFor(admin())).permitted()).isFalse();
  }

  @Test
  @DisplayName("a refusal on authority names the authority that was wanted")
  void evaluate_whenRefusedOnAuthority_saysWhichAuthorityWasWanted() {
    EvaluationOutcome outcome = evaluator.evaluate(graph(), "ADMIN_ONLY", contextFor(stranger()));

    assertThat(outcome)
        .isInstanceOf(EvaluationOutcome.Refused.class)
        .asInstanceOf(
            org.assertj.core.api.InstanceOfAssertFactories.type(EvaluationOutcome.Refused.class))
        .satisfies(
            refused -> {
              assertThat(refused.category()).isEqualTo(FailureCategory.AUTHORIZATION);
              assertThat(refused.reasonCode()).isEqualTo("REQUIRED_AUTHORITY_NOT_HELD");
              assertThat(refused.details()).containsEntry("requiredAuthority", List.of("IS_ADMIN"));
            });
  }

  /** The shape of the Negotiation Transitions that an admin or the creator may fire. */
  private static CompiledGraph adminOrCreatorGraph() {
    return CompiledGraph.builder(VERSION_ID)
        .initialState("OPEN")
        .state("MOVED")
        .transition(
            "OPEN", "GO", "MOVED", Set.of(RequiredAuthority.IS_ADMIN, RequiredAuthority.IS_CREATOR))
        .build();
  }

  @Test
  @DisplayName("a caller holding any one value of the Required Authority set is permitted")
  void evaluate_whenTheCallerHoldsAnyOneValueOfTheSet_permits() {
    assertThat(evaluator.evaluate(adminOrCreatorGraph(), "GO", contextFor(admin())).permitted())
        .isTrue();
    assertThat(evaluator.evaluate(adminOrCreatorGraph(), "GO", contextFor(creator())).permitted())
        .isTrue();
  }

  @Test
  @DisplayName("a caller holding no value of the Required Authority set is refused")
  void evaluate_whenTheCallerHoldsNoValueOfTheSet_refuses() {
    for (Caller caller : List.of(representative(), stranger(), Caller.system())) {
      assertThat(evaluator.evaluate(adminOrCreatorGraph(), "GO", contextFor(caller)).permitted())
          .isFalse();
    }
  }

  @Test
  @DisplayName("a refusal on authority names every value of the set, in declaration order")
  void evaluate_whenRefusedOnASetOfAuthorities_namesTheWholeSet() {
    EvaluationOutcome outcome =
        evaluator.evaluate(adminOrCreatorGraph(), "GO", contextFor(stranger()));

    assertThat(outcome)
        .asInstanceOf(
            org.assertj.core.api.InstanceOfAssertFactories.type(EvaluationOutcome.Refused.class))
        .extracting(EvaluationOutcome.Refused::details)
        .isEqualTo(Map.of("requiredAuthority", List.of("IS_ADMIN", "IS_CREATOR")));
  }

  /**
   * Only the system fires an Event whose authority is {@code SYSTEM}. The test tries every person
   * in the fixture, so it checks the rule and not one caller.
   */
  @Test
  @DisplayName("no person of any shape satisfies SYSTEM")
  void evaluate_whenTheAuthorityIsSystem_isRefusedForEveryPerson() {
    for (Caller person : List.of(admin(), creator(), representative(), stranger())) {
      assertThat(evaluator.evaluate(graph(), "CONCLUDE", contextFor(person)).permitted()).isFalse();
    }
  }

  @Test
  @DisplayName("the system caller satisfies SYSTEM")
  void evaluate_whenTheAuthorityIsSystem_permitsTheSystemCaller() {
    assertThat(evaluator.evaluate(graph(), "CONCLUDE", contextFor(Caller.system())).permitted())
        .isTrue();
  }

  /**
   * If an empty set admitted the system, the system could fire every Transition that requires no
   * authority. {@code SYSTEM} exists to prevent that.
   */
  @ParameterizedTest
  @MethodSource("personAuthorities")
  @DisplayName("the system caller satisfies no person authority and no empty set")
  void evaluate_whenTheCallerIsTheSystem_satisfiesNoPersonAuthority(
      Set<RequiredAuthority> required) {
    CompiledGraph graph =
        CompiledGraph.builder(1L)
            .initialState("OPEN")
            .state("MOVED")
            .transition("OPEN", "GO", "MOVED", required)
            .build();

    assertThat(evaluator.evaluate(graph, "GO", contextFor(Caller.system())).permitted()).isFalse();
  }

  private static List<Set<RequiredAuthority>> personAuthorities() {
    return List.of(
        Set.of(),
        Set.of(RequiredAuthority.IS_ADMIN),
        Set.of(RequiredAuthority.IS_CREATOR),
        Set.of(RequiredAuthority.IS_REPRESENTATIVE));
  }

  /**
   * The Override Event's shape: a declared Event with no Transition. It is refused before any
   * gating, with a different reason code from an Event the version does not declare.
   */
  @Test
  @DisplayName("a declared Event that leads nowhere is refused structurally, before any gating")
  void evaluate_whenTheEventIsDeclaredButCarriesNoTransition_refusesBeforeAnyGating() {
    EvaluationOutcome outcome = evaluator.evaluate(graph(), "OVERRIDE", contextFor(admin()));

    assertThat(outcome)
        .asInstanceOf(
            org.assertj.core.api.InstanceOfAssertFactories.type(EvaluationOutcome.Refused.class))
        .satisfies(
            refused -> {
              assertThat(refused.category()).isEqualTo(FailureCategory.NO_TRANSITION);
              assertThat(refused.reasonCode()).isEqualTo("NO_TRANSITION_FOR_EVENT");
              assertThat(refused.details())
                  .containsEntry("event", "OVERRIDE")
                  .containsEntry("fromState", "OPEN");
            });
  }

  @Test
  @DisplayName("an Event the version never declared is refused under its own reason code")
  void evaluate_whenTheVersionDeclaresNoSuchEventAtAll_saysSoDistinctly() {
    EvaluationOutcome outcome = evaluator.evaluate(graph(), "TYPO", contextFor(admin()));

    assertThat(outcome)
        .asInstanceOf(
            org.assertj.core.api.InstanceOfAssertFactories.type(EvaluationOutcome.Refused.class))
        .satisfies(
            refused -> {
              assertThat(refused.category()).isEqualTo(FailureCategory.NO_TRANSITION);
              assertThat(refused.reasonCode()).isEqualTo("UNKNOWN_EVENT");
              assertThat(refused.details()).containsEntry("event", "TYPO");
            });
  }

  /**
   * Not a refusal. A refusal answers a question about a move, and here the data behind the question
   * is broken. It throws, so the caller can map it to a 500 and a log line, never a 403, 422 or
   * 409.
   */
  @Test
  @DisplayName("a Lifecycle in a State the version does not declare is refused as graph corruption")
  void evaluate_whenTheVersionDoesNotDeclareTheCurrentState_throwsRatherThanRefusing() {
    assertThatThrownBy(
            () -> evaluator.evaluate(graph(), "ANYONE", contextIn(UNDECLARED_STATE, admin())))
        .isInstanceOf(InvalidGraphException.class)
        .hasMessageContaining("Definition Version " + VERSION_ID)
        .hasMessageContaining(UNDECLARED_STATE)
        .hasMessageContaining("OPEN");
  }

  /**
   * The State check runs before the Event check. In the other order a corrupt pin would come back
   * as {@code UNKNOWN_EVENT}, blaming the caller's input for a fault in the data.
   */
  @Test
  @DisplayName("a corrupt pin is reported as corruption even when the Event is a typo too")
  void evaluate_whenTheCurrentStateIsUndeclaredAndTheEventUnknown_blamesTheStateAndNotTheEvent() {
    assertThatThrownBy(
            () -> evaluator.evaluate(graph(), "TYPO", contextIn(UNDECLARED_STATE, admin())))
        .isInstanceOf(InvalidGraphException.class)
        .hasMessageContaining("Definition Version " + VERSION_ID)
        .hasMessageContaining(UNDECLARED_STATE)
        .hasMessageContaining("OPEN")
        .hasMessageNotContaining("TYPO");
  }

  @Test
  @DisplayName("the listing omits a blocked Event rather than offering it as unavailable")
  void possibleEvents_omitsBlockedEventsRatherThanListingThemAsUnavailable() {
    assertThat(evaluator.possibleEvents(graph(), contextFor(creator())))
        .containsExactlyInAnyOrder("ANYONE", "CREATOR_ONLY");
  }

  @Test
  @DisplayName("the listing offers an Event with an empty Required Authority to any person")
  void possibleEvents_whenTheAuthorityIsEmpty_offersItToAnyPerson() {
    assertThat(evaluator.possibleEvents(graph(), contextFor(stranger()))).containsExactly("ANYONE");
  }

  @Test
  @DisplayName("a Lifecycle in a terminal State is offered nothing")
  void possibleEvents_whenTheStateIsTerminal_isEmpty() {
    CompiledGraph graph =
        CompiledGraph.builder(1L).initialState("OPEN").terminalState("DONE").build();
    EvaluationContext atDone =
        EvaluationContext.forNegotiation(
            admin(), Subject.negotiation("negotiation-1", "DONE", CREATOR), List.of());

    assertThat(evaluator.possibleEvents(graph, atDone)).isEmpty();
  }

  /**
   * A terminal State's listing is empty, and a corrupt pin's listing throws. Returning empty for
   * both would tell a requester there is nothing to do on a Negotiation whose pinned version cannot
   * describe its State.
   */
  @Test
  @DisplayName(
      "listing the Possible Events over an undeclared State throws rather than saying none")
  void possibleEvents_whenTheVersionDoesNotDeclareTheCurrentState_throwsRatherThanReturningEmpty() {
    assertThatThrownBy(
            () -> evaluator.possibleEvents(graph(), contextIn(UNDECLARED_STATE, admin())))
        .isInstanceOf(InvalidGraphException.class)
        .hasMessageContaining("Definition Version " + VERSION_ID)
        .hasMessageContaining(UNDECLARED_STATE)
        .hasMessageContaining("OPEN");
  }

  /**
   * For every caller and every Event in the fixture, an Event is listed exactly when firing it
   * would be permitted. Both entry points share one path.
   */
  @Test
  @DisplayName(
      "the listing offers exactly the Events the gate would permit, for every caller shape")
  void possibleEvents_offersExactlyWhatEvaluateWouldPermit() {
    CompiledGraph graph = graph();
    List<String> everyEvent =
        List.of("ANYONE", "ADMIN_ONLY", "CREATOR_ONLY", "REP_ONLY", "CONCLUDE", "OVERRIDE", "TYPO");

    for (Caller caller :
        List.of(admin(), creator(), representative(), stranger(), Caller.system())) {
      EvaluationContext context = contextFor(caller);
      Set<String> listed = evaluator.possibleEvents(graph, context);

      for (String event : everyEvent) {
        assertThat(listed.contains(event))
            .as("caller %s, event %s", caller, event)
            .isEqualTo(evaluator.evaluate(graph, event, context).permitted());
      }
    }
  }

  // --- the Information Requirement stage, and the pipeline's order ---------------------------

  @Test
  @DisplayName(
      "an unmet Information Requirement refuses with the stage's category and the step's reason")
  void evaluate_whenARequirementIsUnmet_refusesWithTheStagesOwnCategoryAndTheStepsReason() {
    requirements.blocked.put(
        "ANYONE", GuardVerdict.fail("REQUIREMENT_NOT_MET", Map.of("missingForms", List.of("7"))));

    EvaluationOutcome outcome = evaluator.evaluate(graph(), "ANYONE", contextFor(stranger()));

    assertThat(outcome)
        .asInstanceOf(
            org.assertj.core.api.InstanceOfAssertFactories.type(EvaluationOutcome.Refused.class))
        .satisfies(
            refused -> {
              assertThat(refused.category()).isEqualTo(FailureCategory.UNMET_REQUIREMENT);
              assertThat(refused.reasonCode()).isEqualTo("REQUIREMENT_NOT_MET");
              assertThat(refused.details()).containsEntry("missingForms", List.of("7"));
            });
  }

  /**
   * The stage is built in, so no Wiring row can omit it. It runs on every Transition of every
   * version, including one an admin adds later.
   */
  @Test
  @DisplayName(
      "the Information Requirement stage runs on every Transition, with no Wiring row to omit it")
  void evaluate_runsTheRequirementStageOnEveryTransitionWithoutAnyWiring() {
    requirements.blockEverything();

    for (String event : List.of("ANYONE", "ADMIN_ONLY", "CREATOR_ONLY", "REP_ONLY")) {
      assertThat(evaluator.evaluate(graph(), event, contextFor(admin())).permitted())
          .as("event %s", event)
          .isFalse();
    }
  }

  /**
   * Required Authority is checked first, so a caller who fails both is told about the authority.
   * This also stops an unauthorised caller from learning that an Information Requirement exists.
   */
  @Test
  @DisplayName("a caller failing both authority and requirement is told about the authority")
  void evaluate_whenBothAuthorityAndRequirementFail_reportsTheAuthorityOne() {
    requirements.blockEverything();

    EvaluationOutcome outcome = evaluator.evaluate(graph(), "ADMIN_ONLY", contextFor(stranger()));

    assertThat(outcome)
        .asInstanceOf(
            org.assertj.core.api.InstanceOfAssertFactories.type(EvaluationOutcome.Refused.class))
        .extracting(EvaluationOutcome.Refused::category)
        .isEqualTo(FailureCategory.AUTHORIZATION);
  }

  @Test
  @DisplayName("a caller failing both requirement and Guard is told about the requirement")
  void evaluate_whenBothRequirementAndGuardFail_reportsTheRequirementOne() {
    requirements.blockEverything();
    CompiledGraph guarded =
        CompiledGraph.builder(1L)
            .initialState("OPEN")
            .state("MOVED")
            .transition(
                new CompiledTransition(
                    "OPEN", "GO", "MOVED", Set.of(), List.of(alwaysFailing("ALWAYS_NO"))))
            .build();

    EvaluationOutcome outcome = evaluator.evaluate(guarded, "GO", contextFor(stranger()));

    assertThat(outcome)
        .asInstanceOf(
            org.assertj.core.api.InstanceOfAssertFactories.type(EvaluationOutcome.Refused.class))
        .extracting(EvaluationOutcome.Refused::category)
        .isEqualTo(FailureCategory.UNMET_REQUIREMENT);
  }

  // --- the Guard stage ------------------------------------------------------------------------

  @Test
  @DisplayName("a Guard's refusal is a domain-state conflict that names the refusing Guard")
  void evaluate_whenAGuardRefuses_reportsADomainStateConflictNamingTheGuard() {
    CompiledGraph guarded =
        CompiledGraph.builder(1L)
            .initialState("OPEN")
            .state("MOVED")
            .transition(
                new CompiledTransition(
                    "OPEN",
                    "GO",
                    "MOVED",
                    Set.of(),
                    List.of(alwaysFailing("TERMINAL_AGGREGATION"))))
            .build();

    EvaluationOutcome outcome = evaluator.evaluate(guarded, "GO", contextFor(stranger()));

    assertThat(outcome)
        .asInstanceOf(
            org.assertj.core.api.InstanceOfAssertFactories.type(EvaluationOutcome.Refused.class))
        .satisfies(
            refused -> {
              assertThat(refused.category()).isEqualTo(FailureCategory.DOMAIN_STATE_CONFLICT);
              assertThat(refused.reasonCode()).isEqualTo("NOPE");
              assertThat(refused.details()).containsEntry("guard", "TERMINAL_AGGREGATION");
            });
  }

  @Test
  @DisplayName("a Guard chain stops at the first refusal and the rest never run")
  void evaluate_whenAGuardRefuses_doesNotRunTheRestOfTheChain() {
    List<String> ran = new java.util.ArrayList<>();
    CompiledGraph guarded =
        CompiledGraph.builder(1L)
            .initialState("OPEN")
            .state("MOVED")
            .transition(
                new CompiledTransition(
                    "OPEN",
                    "GO",
                    "MOVED",
                    Set.of(),
                    List.of(
                        recording("FIRST", ran, GuardVerdict.pass()),
                        recording("SECOND", ran, GuardVerdict.fail("NOPE")),
                        recording("THIRD", ran, GuardVerdict.pass()))))
            .build();

    evaluator.evaluate(guarded, "GO", contextFor(stranger()));

    assertThat(ran).containsExactly("FIRST", "SECOND");
  }

  // --- the Action chain, reported and not run --------------------------------------------------

  /**
   * A permitted outcome is a decision, not a commit. The services around the evaluator commit the
   * move and run its Actions, so the outcome carries the chain in run order and the evaluator runs
   * none of it. Otherwise, evaluating twenty Events to build a Possible Events listing would run
   * twenty Action chains.
   */
  @Test
  @DisplayName("a permitted outcome reports its ordered Action chain and runs none of it")
  void evaluate_whenPermitted_reportsTheOrderedActionChainAndRunsNoneOfIt() {
    List<String> ran = new java.util.ArrayList<>();
    CompiledGraph withActions =
        CompiledGraph.builder(1L)
            .initialState("OPEN")
            .state("MOVED")
            .transition(
                new CompiledTransition(
                    "OPEN",
                    "GO",
                    "MOVED",
                    Set.of(),
                    List.of(),
                    List.of(
                        recordingAction("FIRST", ran),
                        recordingAction("SECOND", ran),
                        recordingAction("THIRD", ran))))
            .build();

    EvaluationOutcome outcome = evaluator.evaluate(withActions, "GO", contextFor(stranger()));

    assertThat(outcome)
        .asInstanceOf(
            org.assertj.core.api.InstanceOfAssertFactories.type(EvaluationOutcome.Permitted.class))
        .satisfies(
            permitted ->
                assertThat(permitted.actions())
                    .extracting(ActionStep::typeKey)
                    .containsExactly("FIRST", "SECOND", "THIRD"));
    assertThat(ran).isEmpty();
  }

  @Test
  @DisplayName("listing the Possible Events runs no Action either")
  void possibleEvents_runsNoAction() {
    List<String> ran = new java.util.ArrayList<>();
    CompiledGraph withActions =
        CompiledGraph.builder(1L)
            .initialState("OPEN")
            .state("MOVED")
            .state("ALSO_MOVED")
            .transition(
                new CompiledTransition(
                    "OPEN",
                    "GO",
                    "MOVED",
                    Set.of(),
                    List.of(),
                    List.of(recordingAction("ON_GO", ran))))
            .transition(
                new CompiledTransition(
                    "OPEN",
                    "ALSO_GO",
                    "ALSO_MOVED",
                    Set.of(),
                    List.of(),
                    List.of(recordingAction("ON_ALSO_GO", ran))))
            .build();

    assertThat(evaluator.possibleEvents(withActions, contextFor(stranger())))
        .containsExactlyInAnyOrder("GO", "ALSO_GO");
    assertThat(ran).isEmpty();
  }

  /**
   * {@code CompiledGraphTest} shows that a graph with a cycle builds. This test shows that the
   * evaluator permits both directions and offers the outbound Event again after the return.
   */
  @Test
  @DisplayName("a Lifecycle that returns to an earlier State is offered its way out again")
  void evaluate_whenTheGraphHasACycle_permitsBothDirections() {
    CompiledGraph cyclic =
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

    assertThat(evaluator.evaluate(cyclic, "APPROVE", contextIn("SUBMITTED", admin())).permitted())
        .isTrue();
    assertThat(
            evaluator
                .evaluate(cyclic, "RETURN_FOR_RESUBMISSION", contextIn("IN_PROGRESS", admin()))
                .permitted())
        .isTrue();
    assertThat(evaluator.possibleEvents(cyclic, contextIn("SUBMITTED", admin())))
        .containsExactly("APPROVE");
  }

  private static ActionStep recordingAction(String typeKey, List<String> ran) {
    return new ActionStep() {
      @Override
      public String typeKey() {
        return typeKey;
      }

      @Override
      public void run(ActionContext context) {
        ran.add(typeKey);
      }
    };
  }

  private static GuardStep alwaysFailing(String typeKey) {
    return recording(typeKey, new java.util.ArrayList<>(), GuardVerdict.fail("NOPE"));
  }

  private static GuardStep recording(String typeKey, List<String> ran, GuardVerdict verdict) {
    return new GuardStep() {
      @Override
      public String typeKey() {
        return typeKey;
      }

      @Override
      public GuardVerdict check(EvaluationContext context) {
        ran.add(typeKey);
        return verdict;
      }
    };
  }

  /** Satisfied unless a test blocks an Event, because most Events have no Information Requirement. */
  private static final class StubRequirements implements InformationRequirementSatisfaction {

    private final Map<String, GuardVerdict> blocked = new java.util.HashMap<>();
    private GuardVerdict everything;

    void blockEverything() {
      everything = GuardVerdict.fail("REQUIREMENT_NOT_MET");
    }

    @Override
    public GuardVerdict check(String event, EvaluationContext context) {
      if (everything != null) {
        return everything;
      }
      return blocked.getOrDefault(event, GuardVerdict.pass());
    }
  }
}
