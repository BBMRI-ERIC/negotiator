package eu.bbmri_eric.negotiator.characterization.service;

import static eu.bbmri_eric.negotiator.characterization.service.LifecycleHistory.allNegotiationRecords;
import static eu.bbmri_eric.negotiator.characterization.service.LifecycleHistory.negotiationRecordsOf;
import static eu.bbmri_eric.negotiator.characterization.service.LifecycleHistory.negotiationStatesRecordedFor;
import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.ADMIN;
import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.CREATOR;
import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.NO_RESOURCES;
import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.putInState;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.authenticateAs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapter;
import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapterConfig;
import eu.bbmri_eric.negotiator.characterization.service.LifecycleHistory.NegotiationRecord;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.common.exceptions.LifecycleConflictException;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.util.WithMockNegotiatorUser;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Pins the audit trail a Negotiation Transition writes: one Lifecycle Record per Transition, naming
 * the State it changed to, attached to the Negotiation it happened on, with its auditing columns
 * filled in.
 *
 * <p>No assertion names a column, so a change to how the State is stored does not touch them.
 * Every read goes through {@link LifecycleHistory} and treats a recorded State as a string.
 *
 * <ul>
 *   <li>A Record holds the destination State only, not the origin or the Event, so two Transitions
 *       into one State from different States leave identical rows. Rebuilding a Transition from
 *       the trail depends on row order, which any migration has to keep.
 *   <li>A refused send writes nothing.
 * </ul>
 *
 * <p>The subject is {@code negotiation-2} (see {@link SeededNegotiationSubject}). It has no
 * Resources, so firing at it cannot Spawn and write extra rows, and the seed gives it no Lifecycle
 * Record, so every row under it came from the test. Writing its starting State onto the row writes
 * no Record either.
 *
 * <p>Reads after a send poll through {@link LifecyclePersistence}, waiting for the target State
 * first: the same persist listener call writes the State and the Record. Checks that no row was
 * written wait a settle period first.
 *
 * <p>{@code @DirtiesContext} per method restores the seed, as every class that fires an Event
 * must.
 */
@IntegrationTest(loadTestData = true)
@Import(LifecycleTestAdapterConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class NegotiationHistoryRowsTest {

  /** Long enough for the asynchronous persist path to have written a row, if it were going to. */
  private static final Duration SETTLE = Duration.ofSeconds(3);

  @Autowired LifecycleTestAdapter adapter;
  @Autowired JdbcTemplate jdbcTemplate;

  /**
   * Truncated to whole seconds because the database stores microseconds and the JVM gives
   * nanoseconds, so an untruncated marker taken just before a write can compare as later than it.
   */
  private LocalDateTime testStarted;

  @BeforeEach
  void markTheStartOfTheTest() {
    testStarted = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
  }

  @AfterEach
  void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }

  static Stream<Arguments> everyTransition() {
    return NegotiationGraphV1.TRANSITIONS.stream()
        .map(edge -> arguments(edge.source(), edge.event(), edge.target()));
  }

  /**
   * For all eight Transitions, firing an Event leaves exactly one row, naming the State the graph
   * says it leads to and attached to the Negotiation the Event was fired at.
   *
   * <p>The expectation comes from {@link NegotiationGraphV1}, which {@link
   * NegotiationGraphV1BindingTest} binds edge for edge to the committed dump.
   */
  @ParameterizedTest(name = "{0} --{1}--> {2}")
  @MethodSource("everyTransition")
  @DisplayName("every Transition writes exactly one Record, naming the State it changed to")
  @WithMockNegotiatorUser(id = ADMIN, authorities = "ROLE_ADMIN")
  void transition_writesExactlyOneRecordNamingItsTargetState(
      String source, String event, String target) {
    putInState(jdbcTemplate, NO_RESOURCES, source);
    assertThat(negotiationStatesRecordedFor(jdbcTemplate, NO_RESOURCES))
        .as("the subject starts with no trail of its own, or a row found later proves nothing")
        .isEmpty();

    adapter.sendNegotiationEvent(NO_RESOURCES, event);

    awaitState(target);
    LifecyclePersistence.awaitValue(
        List.of(target),
        () -> negotiationStatesRecordedFor(jdbcTemplate, NO_RESOURCES),
        "the trail a '%s' from '%s' left behind".formatted(event, source));
    assertThat(negotiationRecordsOf(jdbcTemplate, NO_RESOURCES))
        .singleElement()
        .extracting(NegotiationRecord::negotiationId)
        .as("the Record is attached to the Negotiation the Event was fired at")
        .isEqualTo(NO_RESOURCES);
  }

  /** The four auditing columns, filled in from the caller and the clock. */
  @Test
  @DisplayName("the Record carries the caller and the time in its auditing columns")
  @WithMockNegotiatorUser(id = ADMIN, authorities = "ROLE_ADMIN")
  void record_carriesItsAuditingColumns() {
    putInState(jdbcTemplate, NO_RESOURCES, "SUBMITTED");

    adapter.sendNegotiationEvent(NO_RESOURCES, "APPROVE");

    awaitState(NegotiationGraphV1.target("SUBMITTED", "APPROVE"));
    LifecyclePersistence.awaitValue(
        1, () -> negotiationRecordsOf(jdbcTemplate, NO_RESOURCES).size(), "one Record was written");
    NegotiationRecord written = negotiationRecordsOf(jdbcTemplate, NO_RESOURCES).get(0);

    assertThat(written.createdBy())
        .as("created by the caller who fired the Event")
        .isEqualTo(ADMIN);
    assertThat(written.modifiedBy()).as("modified by the same caller").isEqualTo(ADMIN);
    assertThat(written.creationDate())
        .as("created now")
        .isNotNull()
        .isAfterOrEqualTo(testStarted)
        .isBeforeOrEqualTo(LocalDateTime.now());
    assertThat(written.modifiedDate())
        .as("modified now, because a Record is written once and never touched again")
        .isNotNull()
        .isAfterOrEqualTo(testStarted)
        .isBeforeOrEqualTo(LocalDateTime.now());
  }

  /**
   * A Record holds the destination only.
   *
   * <p>{@code ABANDON} reaches {@code ABANDONED} from two States. The test fires both routes at one
   * Negotiation, and the two rows are equal once id and timestamps are removed. Neither row holds
   * the origin or the Event, so only row order can rebuild the Transition.
   */
  @Test
  @DisplayName(
      "a Record names the destination only: the same State reached from two different States"
          + " leaves indistinguishable rows")
  @WithMockNegotiatorUser(id = ADMIN, authorities = "ROLE_ADMIN")
  void recordsNameTheDestinationOnly_soTwoRoutesIntoOneStateAreIndistinguishable() {
    String target = NegotiationGraphV1.target("IN_PROGRESS", "ABANDON");
    assertEquals(
        target,
        NegotiationGraphV1.target("PAUSED", "ABANDON"),
        "the contrast is only meaningful because both routes end in the same State");

    putInState(jdbcTemplate, NO_RESOURCES, "IN_PROGRESS");
    adapter.sendNegotiationEvent(NO_RESOURCES, "ABANDON");
    awaitState(target);
    LifecyclePersistence.awaitValue(
        1, () -> negotiationRecordsOf(jdbcTemplate, NO_RESOURCES).size());

    putInState(jdbcTemplate, NO_RESOURCES, "PAUSED");
    adapter.sendNegotiationEvent(NO_RESOURCES, "ABANDON");
    LifecyclePersistence.awaitValue(
        2, () -> negotiationRecordsOf(jdbcTemplate, NO_RESOURCES).size());

    List<NegotiationRecord> written = negotiationRecordsOf(jdbcTemplate, NO_RESOURCES);
    assertThat(written.get(1).withoutIdentityAndTimestamps())
        .as("the row written from IN_PROGRESS and the row written from PAUSED say the same thing")
        .isEqualTo(written.get(0).withoutIdentityAndTimestamps());
    assertThat(written).extracting(NegotiationRecord::changedTo).containsExactly(target, target);
    assertThat(written.get(1).id())
        .as("only insertion order and the timestamps tell the rows apart")
        .isGreaterThan(written.get(0).id());
  }

  /**
   * Several Transitions in sequence leave one row each, in order.
   *
   * <p>The path passes through {@code IN_PROGRESS} twice. The Records live in a {@code Set} on the
   * Negotiation and differ only by object identity, so a Record type with an {@code equals} would
   * lose the second visit.
   */
  @Test
  @DisplayName("a multi-step path leaves one Record per step, in the order the steps happened")
  @WithMockNegotiatorUser(id = ADMIN, authorities = "ROLE_ADMIN")
  void multiStepPath_leavesOneRecordPerStepInOrder() {
    putInState(jdbcTemplate, NO_RESOURCES, "SUBMITTED");

    List<String> expected = fireAll("APPROVE", "PAUSE", "UNPAUSE", "ABANDON");

    assertThat(new HashSet<>(expected))
        .as("the path revisits a State, so the trail has to hold the same name twice")
        .hasSizeLessThan(expected.size());
    LifecyclePersistence.awaitValue(
        expected,
        () -> negotiationStatesRecordedFor(jdbcTemplate, NO_RESOURCES),
        "the trail the whole path left behind");

    List<NegotiationRecord> written = negotiationRecordsOf(jdbcTemplate, NO_RESOURCES);
    assertThat(written).extracting(NegotiationRecord::id).isSorted();
    assertThat(written).extracting(NegotiationRecord::creationDate).isSorted();
  }

  /**
   * Every row names a declared State, so a migration that maps the table by State name finds a State
   * row for each.
   *
   * <p>Checked over the whole table, seeded rows and new ones, against {@link
   * NegotiationGraphV1#allStateNames()}. That set includes the Legacy State, because dropping it
   * would strand any row that named it.
   */
  @Test
  @DisplayName("every State name in the whole trail is one the Definition declares")
  @WithMockNegotiatorUser(id = ADMIN, authorities = "ROLE_ADMIN")
  void everyRecordedStateName_isDeclaredByTheDefinition() {
    putInState(jdbcTemplate, NO_RESOURCES, "SUBMITTED");
    List<String> visited = fireAll("APPROVE", "PAUSE", "ABANDON");
    LifecyclePersistence.awaitValue(
        visited, () -> negotiationStatesRecordedFor(jdbcTemplate, NO_RESOURCES));

    List<NegotiationRecord> wholeTable = allNegotiationRecords(jdbcTemplate);

    assertThat(wholeTable)
        .as("seeded rows and written rows alike")
        .hasSizeGreaterThan(visited.size());
    assertThat(wholeTable)
        .extracting(NegotiationRecord::changedTo)
        .isSubsetOf(NegotiationGraphV1.allStateNames());
  }

  static Stream<Arguments> refusals() {
    return Stream.of(
        arguments(
            "an Event this caller has no Required Authority for",
            CREATOR,
            "APPROVE",
            ForbiddenRequestException.class),
        arguments(
            "an Event with no Transition from this State",
            ADMIN,
            "UNPAUSE",
            LifecycleConflictException.class),
        arguments(
            "an Event that sits on no Transition at all",
            ADMIN,
            "START",
            LifecycleConflictException.class));
  }

  /** A refused send moves nothing and records nothing. */
  @ParameterizedTest(name = "{0}")
  @MethodSource("refusals")
  @DisplayName("a refused send writes no Record")
  void refusedSend_writesNoRecord(
      String shapeOfTheRefusal,
      long caller,
      String event,
      Class<? extends RuntimeException> refusal) {
    putInState(jdbcTemplate, NO_RESOURCES, "SUBMITTED");
    authenticateAs(caller);
    int recordsBefore = allNegotiationRecords(jdbcTemplate).size();

    assertThatThrownBy(() -> adapter.sendNegotiationEvent(NO_RESOURCES, event))
        .as("the Negotiation service refuses by throwing: %s", shapeOfTheRefusal)
        .isInstanceOf(refusal);

    LifecyclePersistence.awaitValueAfterSettling(
        SETTLE,
        List.of(),
        () -> negotiationStatesRecordedFor(jdbcTemplate, NO_RESOURCES),
        "a refused Event leaves the subject's trail empty");
    assertThat(allNegotiationRecords(jdbcTemplate))
        .as("and leaves every other Negotiation's trail alone too")
        .hasSize(recordsBefore);
  }

  /**
   * The seed already has Lifecycle Records, so a test that could not tell its rows from the seeded
   * ones would pin the seed rather than the Transition.
   *
   * <p>The test checks three things: the seeded rows survive unchanged, the new row is the only one
   * dated inside the test's window, and it is the only one attached to the subject.
   */
  @Test
  @DisplayName("a Record written by a Transition is distinguishable from the seeded Records")
  @WithMockNegotiatorUser(id = ADMIN, authorities = "ROLE_ADMIN")
  void writtenRecords_areDistinguishableFromTheSeededOnes() {
    List<NegotiationRecord> seeded = allNegotiationRecords(jdbcTemplate);
    assertThat(seeded).as("the seed carries a trail of its own, or this pins nothing").isNotEmpty();
    assertThat(seeded)
        .as("every seeded Record predates this test")
        .allSatisfy(record -> assertThat(record.creationDate()).isBefore(testStarted));

    putInState(jdbcTemplate, NO_RESOURCES, "SUBMITTED");
    adapter.sendNegotiationEvent(NO_RESOURCES, "APPROVE");
    awaitState(NegotiationGraphV1.target("SUBMITTED", "APPROVE"));
    LifecyclePersistence.awaitValue(
        seeded.size() + 1, () -> allNegotiationRecords(jdbcTemplate).size());

    List<NegotiationRecord> after = allNegotiationRecords(jdbcTemplate);
    assertThat(after)
        .as("the seeded rows are still there, unchanged")
        .startsWith(seeded.toArray(new NegotiationRecord[0]));
    assertThat(after.subList(seeded.size(), after.size()))
        .singleElement()
        .satisfies(
            written -> {
              assertThat(written.creationDate()).isAfterOrEqualTo(testStarted);
              assertThat(written.negotiationId()).isEqualTo(NO_RESOURCES);
              assertThat(written.id())
                  .isGreaterThan(
                      seeded.stream().mapToLong(NegotiationRecord::id).max().orElseThrow());
            });
  }

  /**
   * Fires each Event in turn and returns the States the graph says the Negotiation passed through.
   */
  private List<String> fireAll(String... events) {
    List<String> visited = new ArrayList<>();
    for (String event : events) {
      String source = adapter.currentNegotiationState(NO_RESOURCES);
      String target = NegotiationGraphV1.target(source, event);
      adapter.sendNegotiationEvent(NO_RESOURCES, event);
      awaitState(target);
      visited.add(target);
    }
    return visited;
  }

  private void awaitState(String expected) {
    LifecyclePersistence.awaitState(expected, () -> adapter.currentNegotiationState(NO_RESOURCES));
  }
}
