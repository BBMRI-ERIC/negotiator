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
 * <p>ADR 0008 converts the Record's State column into a foreign key to a State row, so what has to
 * survive the conversion is exactly what is asserted here - which is why nothing below names a
 * column. Every read goes through {@link LifecycleHistory}, the suite's one reader of these two
 * tables, and speaks of a recorded State as a string.
 *
 * <p><b>Two questions this class settles rather than assumes</b>, both because the migration turns
 * on them:
 *
 * <ul>
 *   <li>a Record captures the destination State and nothing else - not the origin, not the Event -
 *       so two Transitions into the same State from different States leave rows that cannot be told
 *       apart. Reconstructing a Transition from the trail therefore depends on row order, which the
 *       migration has to preserve;
 *   <li>a refused send writes nothing at all. The Negotiation service refuses by raising, so this
 *       is the easy half of the question; its Resource counterpart is the interesting one.
 * </ul>
 *
 * <p><b>The subject is {@code negotiation-2}</b>, described in {@link SeededNegotiationSubject}: it
 * has no Resources, so nothing it is driven through can spawn a Resource Lifecycle and write rows
 * this class did not ask for, and the seed gives it no Lifecycle Record of its own, so every row
 * found under it afterwards was written by the test. Its starting State is written straight onto
 * the row, which - unlike driving a path to reach it - writes no Record either.
 *
 * <p><b>Asynchrony.</b> Reads after a send poll through {@link LifecyclePersistence} under a
 * bounded timeout, and the target State is awaited first: the Record is written by the same persist
 * listener call that writes the State, so a State that has landed is a Record that has landed.
 * Where the claim is that no row was written, the read additionally settles, so that "nothing"
 * cannot be "not yet".
 *
 * <p>{@code @DirtiesContext} per method restores the seed, as the ordering rule requires of every
 * class that fires an Event.
 */
@IntegrationTest(loadTestData = true)
@Import(LifecycleTestAdapterConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class NegotiationHistoryRowsTest {

  /** Long enough for the asynchronous persist path to have written a row had it been going to. */
  private static final Duration SETTLE = Duration.ofSeconds(3);

  @Autowired LifecycleTestAdapter adapter;
  @Autowired JdbcTemplate jdbcTemplate;

  /**
   * Truncated to whole seconds because the database stores microseconds and the JVM offers
   * nanoseconds: an untruncated marker taken moments before a write can compare as later than the
   * write it precedes.
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
   * The core statement, made for all eight Transitions rather than for one: firing an Event leaves
   * exactly one row, it names the State the pinned graph says the Transition leads to, and it is
   * attached to the Negotiation the Event was fired at.
   *
   * <p>The expectation is computed from {@link NegotiationGraphV1}, which {@link
   * NegotiationGraphV1BindingTest} binds edge for edge to the committed mechanical dump, so this
   * covers the whole graph rather than the Transitions someone remembered to type.
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
   * The destination-only question, settled by firing rather than by reading the entity.
   *
   * <p>{@code ABANDON} reaches {@code ABANDONED} from two different States. Both routes are fired
   * at the same Negotiation, and the two rows they leave are equal once the two things that
   * necessarily differ - the identity and the timestamps - are taken out. Neither origin appears
   * anywhere in either row, and neither does the Event: the trail says where the Negotiation ended
   * up and nothing about how it got there.
   *
   * <p>The consequence for ADR 0008's migration is stated in {@link LifecycleHistory}: with no
   * origin on the row, a Transition is only reconstructible from the order of the rows.
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
        .as("the rows are told apart by insertion order, and by nothing else that is not a clock")
        .isGreaterThan(written.get(0).id());
  }

  /**
   * The accumulation the migration's backfill walks: several Transitions in sequence leave several
   * rows, in the order they happened.
   *
   * <p>The path deliberately passes through {@code IN_PROGRESS} twice, so the trail contains the
   * same State name twice. That is worth pinning on its own: the Records live in a {@code Set} on
   * the Negotiation and are kept apart only by object identity, so a Record type that ever grew an
   * {@code equals} would silently lose the second visit.
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
   * The precondition ADR 0009's backfill rests on: it resolves the whole table by State name, so a
   * row naming something the Definition does not declare would not resolve to a State row at all.
   *
   * <p>Asserted over the whole table - the seeded rows and the ones a driven path just added - and
   * against {@link NegotiationGraphV1#allStateNames()}, which {@link NegotiationGraphV1BindingTest}
   * equates to the committed dump's States and to the States the metadata endpoint publishes. Note
   * that the universe it has to be checked against is the declared one and not the reachable one:
   * the Legacy State belongs to it too, and dropping it would strand any row that named it.
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
        arguments("an Event this caller has no Required Authority for", CREATOR, "APPROVE"),
        arguments("an Event with no Transition from this State", ADMIN, "UNPAUSE"),
        arguments("an Event that sits on no Transition at all", ADMIN, "START"));
  }

  /** A refusal is a refusal all the way down: nothing moves, and nothing is recorded. */
  @ParameterizedTest(name = "{0}")
  @MethodSource("refusals")
  @DisplayName("a refused send writes no Record")
  void refusedSend_writesNoRecord(String shapeOfTheRefusal, long caller, String event) {
    putInState(jdbcTemplate, NO_RESOURCES, "SUBMITTED");
    authenticateAs(caller);
    int recordsBefore = allNegotiationRecords(jdbcTemplate).size();

    assertThatThrownBy(() -> adapter.sendNegotiationEvent(NO_RESOURCES, event))
        .as("the Negotiation service refuses by raising - %s", shapeOfTheRefusal)
        .isInstanceOf(ForbiddenRequestException.class);

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
   * The seed already carries Lifecycle Records, and ADR 0009's backfill joins on State names across
   * the whole table, so a test that could not tell its own rows from the seeded ones would be
   * pinning the seed rather than the Transition.
   *
   * <p>Three things separate them, and all three are asserted: the seeded rows survive the send
   * untouched, the new row is the only one dated inside this test's window, and it is the only one
   * attached to the subject.
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
   * Fires each Event in turn from wherever the Negotiation stands, and returns the States the
   * pinned graph says it passed through - which is what the trail is then compared against.
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
