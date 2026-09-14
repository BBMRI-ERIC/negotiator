package eu.bbmri_eric.negotiator.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.lifecycle.evaluation.EvaluationOutcome;
import eu.bbmri_eric.negotiator.lifecycle.firing.EventFiring;
import eu.bbmri_eric.negotiator.lifecycle.firing.LifecycleRef;
import eu.bbmri_eric.negotiator.lifecycle.firing.UnstartedLifecycleException;
import eu.bbmri_eric.negotiator.lifecycle.firing.UnsupportedScopeException;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext;
import eu.bbmri_eric.negotiator.lifecycle.graph.FailureCategory;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

/**
 * An Event fired end to end: State A, an Event, State B, with a Guard chain and an Action chain
 * actually running, against rows this test wrote and a Lifecycle the seed supplies.
 *
 * <p><b>No such test existed anywhere before this one</b>, in either subsystem. The
 * characterization suite drives Events through Spring Statemachine and asserts what came out, but
 * never sees a Guard or an Action as a configured chain; {@code TransitionEvaluatorTest} evaluates
 * 26 contexts and every one of them is hand-built in the test, so a context assembled wrongly from
 * real rows was not reachable by any assertion in the suite. That gap is what this class closes:
 * everything below starts from two ids and a Caller.
 *
 * <p><b>It is in this package rather than in {@code lifecycle.firing}</b>, for the reason the
 * Compiled Graph resolution test is one package out from {@code lifecycle.definition}: from here
 * the module's edge is the only thing nameable. {@code EventFiringImpl}, the assembler, the
 * assembled pair and the Information Requirement lookup are all package private and stay so — only
 * {@link EventFiring}, {@link LifecycleRef}, the two exceptions and the {@code graph} vocabulary
 * cross the line. That this class compiles is a real assertion about the module's surface.
 *
 * <p><b>The definition rows are written as SQL</b>, like that test's, because the six entities are
 * unreachable from out here and because that is how ADR 0009's seed arrives anyway. The Definition
 * Version Pin is written the same way, and deliberately: the column is {@code updatable = false}
 * and the link row already exists, so <em>how production writes a pin</em> is an open question the
 * effort map files with the coupling slab. A test writing its own pin does not answer it, and must
 * not be read as having answered it.
 *
 * <p><b>The subject is the seeded Negotiation</b> {@code negotiation-1} and its Resource {@code
 * biobank:1:collection:1} — the only seeded pair whose Negotiation is IN_PROGRESS and whose
 * representatives (109, 103), creator (108) and admin (101) are three different people, which is
 * what makes the Required Authority rules separable. Those constants are repeated here rather than
 * imported from the characterization suite: that suite is frozen against the old subsystem and is
 * deleted at cutover, and a dependency on it would take this class with it.
 *
 * <p><b>{@code @DirtiesContext} after every method</b> because these tests fire Events and write
 * Information Requirements. The Flyway strategy is clean-and-migrate on every context build, so
 * dirtying restores both the seed and the empty definition tables for whoever runs next — the
 * Information Requirement lookup is global, so one leaked row would block an Event for the rest of
 * the run, and a Negotiation left concluded is a different corpus than the next class expects.
 */
@IntegrationTest(loadTestData = true)
@Import(FiringTestStrategies.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EventFiringIntegrationTest {

  private static final String NEGOTIATION = "negotiation-1";

  /** The Resource's {@code source_id}, which is what every Lifecycle path keys on. */
  private static final String RESOURCE = "biobank:1:collection:1";

  private static final long RESOURCE_ROW_ID = 4L;
  private static final long ANOTHER_RESOURCE_ROW_ID = 5L;

  private static final long ADMIN = 101L;
  private static final long REPRESENTATIVE = 109L;
  private static final long CREATOR = 108L;

  /**
   * The three State names this test's graph uses, and they are <b>not</b> free choices.
   *
   * <p>The Lifecycle Record still stores its State as the legacy {@code NegotiationResourceState}
   * enum, and {@code NegotiationResourceLifecycleRecord.forStateNamed} resolves the name through
   * {@code valueOf} — "deliberately the loud kind", as its javadoc says. So a committed move can
   * only reach a State that enum still knows, however freely the definition tables name States. A
   * graph using invented names compiles, evaluates and is permitted, and then fails at the append.
   *
   * <p>That is a real constraint on the new subsystem and not an artefact of this test. It lifts
   * when ADR 0008's {@code state_id} FK conversion replaces the enum column, which is the cutover's
   * work; until then anything firing Events for real is confined to the legacy names.
   */
  private static final String INITIAL = "SUBMITTED";

  private static final String CONTACTED = "REPRESENTATIVE_CONTACTED";
  private static final String CHECKING = "CHECKING_AVAILABILITY";

  /** Permitted for anyone, with a Guard chain to pass and an Action chain to run. */
  private static final String CONTACT = "CONTACT";

  /** Permitted for an admin alone, so every other caller is refused on authority. */
  private static final String ADMIN_ONLY = "CHECK_AVAILABILITY";

  /** Reachable and authorised, and refused by its Guard every time. */
  private static final String GUARDED = "LOCKED";

  private static final String ROLE_ADMIN = "ROLE_ADMIN";

  @Autowired EventFiring firing;
  @Autowired JdbcTemplate jdbc;
  @Autowired FiringTestStrategies.Recorder recorder;

  private DefinitionRows rows;

  /**
   * The rows written are never cleaned up, and do not need to be: {@code @DirtiesContext} rebuilds
   * the context after every method and the Flyway strategy is clean-and-migrate on every build, so
   * the six definition tables are empty again before the next one runs. {@link
   * DefinitionRows#deleteEverythingWritten()} exists for the test that shares its context instead,
   * which this one does not.
   */
  @BeforeEach
  void startWithAnEmptyLog() {
    rows = new DefinitionRows(jdbc);
    recorder.clear();
  }

  // ---------------------------------------------------------------- Possible Events

  @Test
  @DisplayName("Possible Events for a real Resource, from ids and a Caller alone")
  void possibleEvents_fromIdsAndACallerAlone() {
    pinTheResourceTo(writeTheResourceFlow());

    Set<String> offered = firing.possibleEvents(subject(), person(REPRESENTATIVE));

    assertThat(offered)
        .as(
            "exactly what this caller may act on from %s: not the admin-only Event, and not the"
                + " one whose Guard refuses",
            INITIAL)
        .containsExactly(CONTACT);
  }

  @Test
  @DisplayName("an Event blocked by its Guard is left out of the listing rather than offered")
  void possibleEvents_omitsAnEventItsGuardRefuses() {
    pinTheResourceTo(writeTheResourceFlow());

    assertThat(firing.possibleEvents(subject(), person(REPRESENTATIVE)))
        .as("the refusing Guard's Event is reachable and authorised, and still must not appear")
        .doesNotContain(GUARDED);
  }

  @Test
  @DisplayName("an Event the caller has no Required Authority for is left out of the listing")
  void possibleEvents_omitsAnEventTheCallerMayNotFire() {
    pinTheResourceTo(writeTheResourceFlow());

    assertThat(firing.possibleEvents(subject(), person(REPRESENTATIVE))).doesNotContain(ADMIN_ONLY);
    assertThat(firing.possibleEvents(subject(), admin()))
        .as("and is offered to the caller who does hold it")
        .contains(ADMIN_ONLY);
  }

  @Test
  @DisplayName("Possible Events and the real gate agree, for the same subject and Caller")
  void possibleEvents_andTheGate_agree() {
    long version = writeTheResourceFlow();
    pinTheResourceTo(version);
    EvaluationContext.Caller caller = person(REPRESENTATIVE);

    Set<String> offered = firing.possibleEvents(subject(), caller);

    assertThat(offered).isNotEmpty();
    for (String event : everyEventTheGraphDeclares(version)) {
      boolean permitted = firing.fire(subject(), event, caller).permitted();
      assertThat(permitted)
          .as(
              "the listing %s '%s', so the gate must %s it",
              offered.contains(event) ? "offered" : "withheld",
              event,
              offered.contains(event) ? "permit" : "refuse")
          .isEqualTo(offered.contains(event));
      putTheResourceIn(INITIAL);
    }
  }

  // ---------------------------------------------------------------- Firing a permitted Event

  @Test
  @DisplayName("a permitted Event writes the new State, appends one record, and runs the chain")
  void fire_whenPermitted_commitsTheMoveAndRunsTheActions() {
    pinTheResourceTo(writeTheResourceFlow());
    int recordsBefore = lifecycleRecordsForTheResource();

    EvaluationOutcome outcome = firing.fire(subject(), CONTACT, person(REPRESENTATIVE));

    assertThat(outcome.permitted()).isTrue();
    assertThat(currentState()).as("State A, Event, State B").isEqualTo(CONTACTED);
    assertThat(lifecycleRecordsForTheResource())
        .as("exactly one Lifecycle Record, not none and not one per Action")
        .isEqualTo(recordsBefore + 1);
    assertThat(lastLifecycleRecordState()).isEqualTo(CONTACTED);
  }

  @Test
  @DisplayName("the Guard chain runs definition-wide entries first, then the Transition's")
  void fire_whenPermitted_runsTheGuardChainInTheCompiledOrder() {
    pinTheResourceTo(writeTheResourceFlow());

    firing.fire(subject(), CONTACT, person(REPRESENTATIVE));

    assertThat(recorder.ran())
        .as("the two Guard scopes arrive as one chain, definition-wide first")
        .startsWith(FiringTestStrategies.DEFINITION_WIDE, FiringTestStrategies.TRANSITION_SCOPED);
  }

  @Test
  @DisplayName("the Action chain runs after the commit, in configured order")
  void fire_whenPermitted_runsTheActionChainInOrderAfterTheGuards() {
    pinTheResourceTo(writeTheResourceFlow());

    firing.fire(subject(), CONTACT, person(REPRESENTATIVE));

    assertThat(recorder.ran())
        .as("Guards before Actions, and the two Action rows in their sort_order")
        .containsExactly(
            FiringTestStrategies.DEFINITION_WIDE,
            FiringTestStrategies.TRANSITION_SCOPED,
            "first-action",
            "second-action");
  }

  // ---------------------------------------------------------------- Firing a refused Event

  @Test
  @DisplayName("a Guard's refusal writes nothing and reaches the caller as 409 with its reason")
  void fire_whenAGuardRefuses_writesNothingAndCarriesTheReasonOut() {
    pinTheResourceTo(writeTheResourceFlow());
    int recordsBefore = lifecycleRecordsForTheResource();

    EvaluationOutcome outcome = firing.fire(subject(), GUARDED, person(REPRESENTATIVE));

    assertRefused(
        outcome, FailureCategory.DOMAIN_STATE_CONFLICT, FiringTestStrategies.REFUSAL_REASON);
    assertNothingMoved(recordsBefore);
    assertThat(recorder.ran())
        .as("the refusing Guard ran; no Action did")
        .containsExactly(FiringTestStrategies.DEFINITION_WIDE, FiringTestStrategies.REFUSING);
  }

  @Test
  @DisplayName("a caller without the Required Authority is refused before any Guard runs")
  void fire_whenTheCallerLacksAuthority_isRefusedFirstAndWritesNothing() {
    pinTheResourceTo(writeTheResourceFlow());
    int recordsBefore = lifecycleRecordsForTheResource();

    EvaluationOutcome outcome = firing.fire(subject(), ADMIN_ONLY, person(CREATOR));

    assertRefused(outcome, FailureCategory.AUTHORIZATION, "REQUIRED_AUTHORITY_NOT_HELD");
    assertNothingMoved(recordsBefore);
    assertThat(recorder.ran())
        .as("Required Authority is the first stage, so not even a definition-wide Guard runs")
        .isEmpty();
  }

  @Test
  @DisplayName("an Event with no Transition from this State is refused without moving anything")
  void fire_whenNoTransitionLeavesThisState_isRefused() {
    pinTheResourceTo(writeTheResourceFlow());
    putTheResourceIn(CHECKING);
    int recordsBefore = lifecycleRecordsForTheResource();

    EvaluationOutcome outcome = firing.fire(subject(), CONTACT, person(REPRESENTATIVE));

    assertRefused(outcome, FailureCategory.NO_TRANSITION, "NO_TRANSITION_FOR_EVENT");
    assertThat(currentState()).isEqualTo(CHECKING);
    assertThat(lifecycleRecordsForTheResource()).isEqualTo(recordsBefore);
  }

  @Test
  @DisplayName("an Event the graph does not declare at all is refused as an unknown Event")
  void fire_whenTheGraphDoesNotDeclareTheEvent_isRefused() {
    pinTheResourceTo(writeTheResourceFlow());

    EvaluationOutcome outcome = firing.fire(subject(), "NO_SUCH_EVENT", admin());

    assertRefused(outcome, FailureCategory.NO_TRANSITION, "UNKNOWN_EVENT");
    assertThat(currentState()).isEqualTo(INITIAL);
  }

  // ---------------------------------------------------------------- The Information Requirement
  // stage

  @Test
  @DisplayName("an Event with an unmet Requirement is refused as 422, and nothing moves")
  void fire_withAnUnmetRequirement_isRefusedAsUnmetRequirement() {
    pinTheResourceTo(writeTheResourceFlow());
    requireInformationFor(CONTACT);
    int recordsBefore = lifecycleRecordsForTheResource();

    EvaluationOutcome outcome = firing.fire(subject(), CONTACT, person(REPRESENTATIVE));

    assertRefused(
        outcome, FailureCategory.UNMET_REQUIREMENT, "INFORMATION_REQUIREMENT_NOT_SATISFIED");
    assertNothingMoved(recordsBefore);
  }

  @Test
  @DisplayName("a Submission for this Resource and Negotiation satisfies it")
  void fire_withARequirementAndASubmission_isPermitted() {
    pinTheResourceTo(writeTheResourceFlow());
    submitInformationFor(requireInformationFor(CONTACT), RESOURCE_ROW_ID);

    assertThat(firing.fire(subject(), CONTACT, person(REPRESENTATIVE)).permitted()).isTrue();
    assertThat(currentState()).isEqualTo(CONTACTED);
  }

  /**
   * Today's check is not requirement-scoped, and reproducing that rather than fixing it is
   * deliberate. The old path's own assertion of this is {@code
   * ResourceInformationRequirementGateTest#submissionAgainstADifferentRequirement_satisfiesTheGate};
   * ADR 0006's Qualifying Submission is the change that makes it false, with a migration story of
   * its own.
   */
  @Test
  @DisplayName("a Submission against a different Requirement satisfies it, as it does today")
  void fire_withASubmissionAgainstAnotherRequirement_isPermitted() {
    pinTheResourceTo(writeTheResourceFlow());
    requireInformationFor(CONTACT);
    submitInformationFor(requireInformationFor(ADMIN_ONLY), RESOURCE_ROW_ID);

    assertThat(firing.fire(subject(), CONTACT, person(REPRESENTATIVE)).permitted()).isTrue();
  }

  @Test
  @DisplayName("a Submission for a different Resource does not satisfy it, as it does not today")
  void fire_withASubmissionForAnotherResource_isRefused() {
    pinTheResourceTo(writeTheResourceFlow());
    submitInformationFor(requireInformationFor(CONTACT), ANOTHER_RESOURCE_ROW_ID);

    assertRefused(
        firing.fire(subject(), CONTACT, person(REPRESENTATIVE)),
        FailureCategory.UNMET_REQUIREMENT,
        "INFORMATION_REQUIREMENT_NOT_SATISFIED");
  }

  @Test
  @DisplayName("an Event no Requirement names is ungated")
  void fire_withARequirementOnAnotherEvent_isPermitted() {
    pinTheResourceTo(writeTheResourceFlow());
    requireInformationFor(ADMIN_ONLY);

    assertThat(firing.fire(subject(), CONTACT, person(REPRESENTATIVE)).permitted()).isTrue();
  }

  @Test
  @DisplayName("the Requirement stage gates the listing too, so the two still agree")
  void possibleEvents_withAnUnmetRequirement_omitsTheGatedEvent() {
    pinTheResourceTo(writeTheResourceFlow());
    requireInformationFor(CONTACT);

    assertThat(firing.possibleEvents(subject(), person(REPRESENTATIVE))).doesNotContain(CONTACT);
  }

  // ---------------------------------------------------------------- What cannot be answered

  @Test
  @DisplayName(
      "an unpinned Resource Lifecycle is refused by name, not judged against the active version")
  void fire_whenTheLifecycleIsUnpinned_isRefusedByName() {
    writeTheResourceFlow();

    assertThatThrownBy(() -> firing.possibleEvents(subject(), admin()))
        .isInstanceOf(UnstartedLifecycleException.class)
        .hasMessageContaining("no Definition Version Pin")
        .hasMessageContaining(RESOURCE);
  }

  @Test
  @DisplayName("a Resource in no State is refused by name")
  void fire_whenTheResourceIsInNoState_isRefusedByName() {
    pinTheResourceTo(writeTheResourceFlow());
    jdbc.update(
        "update negotiation_resource_link set current_state = null"
            + " where negotiation_id = ? and resource_id = ?",
        NEGOTIATION,
        RESOURCE_ROW_ID);

    assertThatThrownBy(() -> firing.possibleEvents(subject(), admin()))
        .isInstanceOf(UnstartedLifecycleException.class)
        .hasMessageContaining("in no State");
  }

  @Test
  @DisplayName("a Resource that is not part of the Negotiation is not found")
  void fire_whenTheResourceIsNotLinked_isNotFound() {
    pinTheResourceTo(writeTheResourceFlow());

    assertThatThrownBy(
            () ->
                firing.possibleEvents(
                    LifecycleRef.resource(NEGOTIATION, "biobank:1:collection:unlinked"), admin()))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  @DisplayName("a Negotiation-scope reference is refused by name rather than half-served")
  void fire_whenTheScopeIsNegotiation_isRefusedByName() {
    assertThatThrownBy(() -> firing.possibleEvents(LifecycleRef.negotiation(NEGOTIATION), admin()))
        .isInstanceOf(UnsupportedScopeException.class)
        .hasMessageContaining("TERMINAL_AGGREGATION");
  }

  // ---------------------------------------------------------------- Fixtures

  private LifecycleRef subject() {
    return LifecycleRef.resource(NEGOTIATION, RESOURCE);
  }

  private static EvaluationContext.Caller person(long personId) {
    return EvaluationContext.Caller.person(personId, Set.of());
  }

  private static EvaluationContext.Caller admin() {
    return EvaluationContext.Caller.person(ADMIN, Set.of(ROLE_ADMIN));
  }

  private void assertRefused(EvaluationOutcome outcome, FailureCategory category, String reason) {
    assertThat(outcome).isInstanceOf(EvaluationOutcome.Refused.class);
    EvaluationOutcome.Refused refused = (EvaluationOutcome.Refused) outcome;
    assertThat(refused.category()).isEqualTo(category);
    assertThat(refused.reasonCode()).isEqualTo(reason);
  }

  private void assertNothingMoved(int recordsBefore) {
    assertThat(currentState()).as("the State is unchanged").isEqualTo(INITIAL);
    assertThat(lifecycleRecordsForTheResource())
        .as("no Lifecycle Record was appended")
        .isEqualTo(recordsBefore);
    assertThat(recorder.ran()).as("no Action ran").doesNotContain("first-action", "second-action");
  }

  private String currentState() {
    return jdbc.queryForObject(
        "select current_state from negotiation_resource_link"
            + " where negotiation_id = ? and resource_id = ?",
        String.class,
        NEGOTIATION,
        RESOURCE_ROW_ID);
  }

  private void putTheResourceIn(String state) {
    jdbc.update(
        "update negotiation_resource_link set current_state = ?"
            + " where negotiation_id = ? and resource_id = ?",
        state,
        NEGOTIATION,
        RESOURCE_ROW_ID);
  }

  private int lifecycleRecordsForTheResource() {
    return jdbc.queryForObject(
        "select count(*) from negotiation_resource_lifecycle_record"
            + " where negotiation_id = ? and resource_id = ?",
        Integer.class,
        NEGOTIATION,
        RESOURCE_ROW_ID);
  }

  private String lastLifecycleRecordState() {
    return jdbc.queryForObject(
        "select changed_to from negotiation_resource_lifecycle_record"
            + " where negotiation_id = ? and resource_id = ? order by id desc limit 1",
        String.class,
        NEGOTIATION,
        RESOURCE_ROW_ID);
  }

  private List<String> everyEventTheGraphDeclares(long versionId) {
    return jdbc.queryForList(
        "select name from event where lifecycle_definition_id = ? order by name",
        String.class,
        versionId);
  }

  private long requireInformationFor(String event) {
    Long accessFormId = jdbc.queryForObject("select min(id) from access_form", Long.class);
    return jdbc.queryForObject(
        "insert into information_requirement (required_access_form_id, for_event)"
            + " values (?, ?) returning id",
        Long.class,
        accessFormId,
        event);
  }

  private void submitInformationFor(long requirementId, long resourceRowId) {
    jdbc.update(
        "insert into information_submission (requirement_id, resource_id, negotiation_id, payload)"
            + " values (?, ?, ?, cast(? as json))",
        requirementId,
        resourceRowId,
        NEGOTIATION,
        "{}");
  }

  /**
   * Writes the pin straight onto the link row, which is the only way a test can: the mapping
   * declares the column {@code updatable = false} and the row already exists. That constraint is
   * the open question the coupling slab owns, and this is not an answer to it.
   */
  private void pinTheResourceTo(long versionId) {
    jdbc.update(
        "update negotiation_resource_link set lifecycle_definition_id = ?"
            + " where negotiation_id = ? and resource_id = ?",
        versionId,
        NEGOTIATION,
        RESOURCE_ROW_ID);
  }

  /**
   * A small Resource-scope graph with one of everything the gate needs: a permitted Transition
   * carrying both Guard scopes and a two-row Action chain, a Transition only an admin may fire, and
   * a Transition whose Guard always refuses.
   *
   * <p><b>Neither active nor the Global Default</b>, and both deliberately. Nothing here needs
   * either flag — the Definition Version Pin is read straight off the link row and {@code graphFor}
   * never asks whether a version is active — and setting them would be a trap set for a later
   * session: {@code uq_lifecycle_definition_global_default} is a unique index over the whole table,
   * so a test claiming the Global Default flag starts colliding with ADR 0009's v1 seed the moment
   * that seed lands, and the failure would surface anywhere but here.
   */
  private long writeTheResourceFlow() {
    long version = rows.writeVersion("firing-test-flow", "RESOURCE", 1, false, false);
    long submitted = rows.writeState(version, INITIAL, true, false);
    long contacted = rows.writeState(version, CONTACTED, false, false);
    long checking = rows.writeState(version, CHECKING, false, false);

    long contact = rows.writeEvent(version, CONTACT);
    long adminOnly = rows.writeEvent(version, ADMIN_ONLY);
    long guarded = rows.writeEvent(version, GUARDED);

    long contacting = rows.writeTransition(version, submitted, contact, contacted, "NONE");
    rows.writeTransition(version, submitted, adminOnly, checking, "IS_ADMIN");
    long locking = rows.writeTransition(version, submitted, guarded, checking, "NONE");

    rows.writeGuardWiring(version, null, FiringTestStrategies.DEFINITION_WIDE, 1);
    rows.writeGuardWiring(version, contacting, FiringTestStrategies.TRANSITION_SCOPED, 1);
    rows.writeGuardWiring(version, locking, FiringTestStrategies.REFUSING, 1);

    String action = FiringTestStrategies.RECORDING_ACTION;
    rows.writeActionWiring(contacting, action, "{\"label\":\"first-action\"}", 1);
    rows.writeActionWiring(contacting, action, "{\"label\":\"second-action\"}", 2);
    return version;
  }
}
