package eu.bbmri_eric.negotiator.characterization.service;

import static eu.bbmri_eric.negotiator.characterization.service.LifecycleHistory.allResourceRecords;
import static eu.bbmri_eric.negotiator.characterization.service.LifecycleHistory.resourceRecordsOf;
import static eu.bbmri_eric.negotiator.characterization.service.LifecycleHistory.resourceStatesRecordedFor;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.ADMIN;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.CREATOR;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.NEGOTIATION;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.REPRESENTATIVE;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.RESOURCE;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.authenticateAs;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.putResourceInState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapter;
import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapterConfig;
import eu.bbmri_eric.negotiator.characterization.service.LifecycleHistory.ResourceRecord;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.common.exceptions.LifecycleConflictException;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
 * The Resource half of {@link NegotiationHistoryRowsTest}: one Lifecycle Record per Resource
 * Transition, naming the State it changed to, attached to the Negotiation <em>and</em> to the
 * Resource it concerns, with its auditing columns filled in.
 *
 * <p>As there, no column is named, every read goes through {@link LifecycleHistory}, and a recorded
 * State is a string. The Resource appears as its {@code source_id}, not the row id the Record
 * stores; {@link SeededResourceSubject} explains why.
 *
 * <p>A refusal throws, but the exception alone does not prove that nothing was written. The test
 * asserts "a refusal writes no Record" directly, after a settle period, for all three kinds of
 * refusal {@link ResourceTransitionParityTest} pins.
 *
 * <p>The subject is {@link SeededResourceSubject}: {@code negotiation-1}, its one linked Resource,
 * and the three seeded callers for the three Required Authority values. It starts with a seeded
 * Record, so every expectation is the trail before the send plus what the send added.
 *
 * <p>Reads poll through {@link LifecyclePersistence}, waiting for the target State first; checks
 * that no row was written wait a settle period first.
 *
 * <p>{@code @DirtiesContext} per method restores the seed. This class drives a shared Resource to
 * terminal States, which concludes {@code negotiation-1}.
 */
@IntegrationTest(loadTestData = true)
@Import(LifecycleTestAdapterConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ResourceHistoryRowsTest {

  /** Which seeded caller satisfies which of the graph's three Required Authority rules. */
  private static final Map<String, Long> CALLER_SATISFYING =
      Map.of(
          ResourceGraphV1.IS_ADMIN, ADMIN,
          ResourceGraphV1.IS_REPRESENTATIVE, REPRESENTATIVE,
          ResourceGraphV1.IS_CREATOR, CREATOR);

  /** Long enough for the asynchronous persist path to have written a row, if it were going to. */
  private static final Duration SETTLE = Duration.ofSeconds(3);

  @Autowired LifecycleTestAdapter adapter;
  @Autowired JdbcTemplate jdbcTemplate;

  /** Truncated to whole seconds, for the reason {@link NegotiationHistoryRowsTest} explains. */
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
    return ResourceGraphV1.TRANSITIONS.stream()
        .map(
            edge ->
                arguments(
                    edge.source(),
                    edge.event(),
                    edge.target(),
                    CALLER_SATISFYING.get(edge.requiredAuthority())));
  }

  /**
   * For all thirteen Transitions, firing an Event appends exactly one row, naming the State the graph
   * says it leads to and the Negotiation and Resource the Event was fired at.
   *
   * <p>The expectation comes from {@link ResourceGraphV1}, which {@link ResourceGraphV1BindingTest}
   * binds edge for edge to the committed dump.
   */
  @ParameterizedTest(name = "{0} --{1}--> {2}")
  @MethodSource("everyTransition")
  @DisplayName("every Transition appends exactly one Record, naming the State it changed to")
  void transition_appendsExactlyOneRecordNamingItsTargetState(
      String source, String event, String target, long caller) {
    putResourceInState(jdbcTemplate, source);
    authenticateAs(caller);
    List<String> before = resourceStatesRecordedFor(jdbcTemplate, NEGOTIATION);

    adapter.sendResourceEvent(NEGOTIATION, RESOURCE, event);

    awaitState(target);
    LifecyclePersistence.awaitValue(
        append(before, target),
        () -> resourceStatesRecordedFor(jdbcTemplate, NEGOTIATION),
        "the trail a '%s' from '%s' left behind".formatted(event, source));
    assertThat(lastRecord())
        .as("the Record names the Negotiation and the Resource the Event was fired at")
        .satisfies(
            written -> {
              assertThat(written.negotiationId()).isEqualTo(NEGOTIATION);
              assertThat(written.resourceSourceId()).isEqualTo(RESOURCE);
              assertThat(written.changedTo()).isEqualTo(target);
            });
  }

  /** The four auditing columns, filled in from the caller and the clock. */
  @Test
  @DisplayName("the Record carries the caller and the time in its auditing columns")
  void record_carriesItsAuditingColumns() {
    authenticateAs(ADMIN);
    int before = resourceRecordsOf(jdbcTemplate, NEGOTIATION).size();

    adapter.sendResourceEvent(NEGOTIATION, RESOURCE, "CONTACT");

    awaitState(ResourceGraphV1.target(ResourceGraphV1.INITIAL_STATE, "CONTACT"));
    LifecyclePersistence.awaitValue(
        before + 1, () -> resourceRecordsOf(jdbcTemplate, NEGOTIATION).size());
    ResourceRecord written = lastRecord();

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
   * A Record holds the destination only. {@code CONTACT} reaches {@code REPRESENTATIVE_CONTACTED}
   * from two States, and the two rows are equal once id and timestamps are removed.
   */
  @Test
  @DisplayName(
      "a Record names the destination only: the same State reached from two different States"
          + " leaves indistinguishable rows")
  void recordsNameTheDestinationOnly_soTwoRoutesIntoOneStateAreIndistinguishable() {
    String target = ResourceGraphV1.target("SUBMITTED", "CONTACT");
    assertEquals(
        target,
        ResourceGraphV1.target("REPRESENTATIVE_UNREACHABLE", "CONTACT"),
        "the contrast is only meaningful because both routes end in the same State");
    authenticateAs(ADMIN);
    int before = resourceRecordsOf(jdbcTemplate, NEGOTIATION).size();

    putResourceInState(jdbcTemplate, "SUBMITTED");
    adapter.sendResourceEvent(NEGOTIATION, RESOURCE, "CONTACT");
    LifecyclePersistence.awaitValue(
        before + 1, () -> resourceRecordsOf(jdbcTemplate, NEGOTIATION).size());

    putResourceInState(jdbcTemplate, "REPRESENTATIVE_UNREACHABLE");
    adapter.sendResourceEvent(NEGOTIATION, RESOURCE, "CONTACT");
    LifecyclePersistence.awaitValue(
        before + 2, () -> resourceRecordsOf(jdbcTemplate, NEGOTIATION).size());

    List<ResourceRecord> written = resourceRecordsOf(jdbcTemplate, NEGOTIATION);
    ResourceRecord fromSubmitted = written.get(written.size() - 2);
    ResourceRecord fromUnreachable = written.get(written.size() - 1);
    assertThat(fromUnreachable.withoutIdentityAndTimestamps())
        .as(
            "the row written from SUBMITTED and the row written from REPRESENTATIVE_UNREACHABLE"
                + " say the same thing")
        .isEqualTo(fromSubmitted.withoutIdentityAndTimestamps());
    assertThat(fromUnreachable.changedTo()).isEqualTo(target);
    assertThat(fromUnreachable.id())
        .as("only insertion order and the timestamps tell the rows apart")
        .isGreaterThan(fromSubmitted.id());
  }

  /**
   * The delivery chain: six Transitions in sequence leave six rows after the seeded one, in order.
   */
  @Test
  @DisplayName("a multi-step path leaves one Record per step, in the order the steps happened")
  void multiStepPath_leavesOneRecordPerStepInOrder() {
    List<String> seededTrail = resourceStatesRecordedFor(jdbcTemplate, NEGOTIATION);
    assertEquals(
        ResourceGraphV1.INITIAL_STATE, adapter.currentResourceState(NEGOTIATION, RESOURCE));

    List<String> visited =
        fireAll(
            new Step(ADMIN, "CONTACT"),
            new Step(REPRESENTATIVE, "MARK_AS_CHECKING_AVAILABILITY"),
            new Step(REPRESENTATIVE, "MARK_AS_AVAILABLE"),
            new Step(REPRESENTATIVE, "INDICATE_ACCESS_CONDITIONS"),
            new Step(CREATOR, "ACCEPT_ACCESS_CONDITIONS"),
            new Step(REPRESENTATIVE, "GRANT_ACCESS_TO_RESOURCE"));

    List<String> expected = new ArrayList<>(seededTrail);
    expected.addAll(visited);
    LifecyclePersistence.awaitValue(
        expected,
        () -> resourceStatesRecordedFor(jdbcTemplate, NEGOTIATION),
        "the trail the whole delivery chain left behind, after the seeded row");

    List<ResourceRecord> written = resourceRecordsOf(jdbcTemplate, NEGOTIATION);
    assertThat(written).extracting(ResourceRecord::id).isSorted();
    assertThat(written).extracting(ResourceRecord::creationDate).isSorted();
    assertThat(written)
        .as("every row of the trail concerns the Resource the chain was walked on")
        .allSatisfy(record -> assertThat(record.resourceSourceId()).isEqualTo(RESOURCE));
  }

  /**
   * Every row names a declared State, so a migration that maps the table by State name finds a State
   * row for each.
   *
   * <p>Checked over the whole table, the seeded rows of two Negotiations and the new ones, against
   * {@link ResourceGraphV1#allStateNames()}, Legacy State included.
   */
  @Test
  @DisplayName("every State name in the whole trail is one the Definition declares")
  void everyRecordedStateName_isDeclaredByTheDefinition() {
    int seededForTheSubject = resourceRecordsOf(jdbcTemplate, NEGOTIATION).size();

    List<String> visited =
        fireAll(
            new Step(ADMIN, "CONTACT"),
            new Step(REPRESENTATIVE, "MARK_AS_CHECKING_AVAILABILITY"),
            new Step(REPRESENTATIVE, "MARK_AS_UNAVAILABLE"));
    LifecyclePersistence.awaitValue(
        seededForTheSubject + visited.size(),
        () -> resourceRecordsOf(jdbcTemplate, NEGOTIATION).size());

    List<ResourceRecord> wholeTable = allResourceRecords(jdbcTemplate);

    assertThat(wholeTable)
        .as("seeded rows and written rows alike")
        .hasSizeGreaterThan(visited.size());
    assertThat(wholeTable)
        .extracting(ResourceRecord::changedTo)
        .isSubsetOf(ResourceGraphV1.allStateNames());
  }

  static Stream<Arguments> refusals() {
    return Stream.of(
        arguments(
            "an Event this caller has no Required Authority for",
            CREATOR,
            "CONTACT",
            ForbiddenRequestException.class),
        arguments(
            "an Event with no Transition from this State",
            ADMIN,
            "GRANT_ACCESS_TO_RESOURCE",
            LifecycleConflictException.class),
        arguments(
            "an Event that sits on no Transition at all",
            ADMIN,
            "OVERRIDE",
            LifecycleConflictException.class));
  }

  /** A refused send throws, moves nothing and records nothing. */
  @ParameterizedTest(name = "{0}")
  @MethodSource("refusals")
  @DisplayName("a refused send writes no Record")
  void refusedSend_writesNoRecord(
      String shapeOfTheRefusal,
      long caller,
      String event,
      Class<? extends RuntimeException> refusal) {
    authenticateAs(caller);
    List<ResourceRecord> before = allResourceRecords(jdbcTemplate);

    assertThatThrownBy(() -> adapter.sendResourceEvent(NEGOTIATION, RESOURCE, event))
        .as("the Resource service refuses by throwing: %s", shapeOfTheRefusal)
        .isInstanceOf(refusal);

    LifecyclePersistence.awaitValueAfterSettling(
        SETTLE,
        before,
        () -> allResourceRecords(jdbcTemplate),
        "a refused Event leaves every Resource trail exactly as it found it");
  }

  /**
   * The seed has Resource Records for this Negotiation and another, so the test checks that the
   * rows a Transition adds are distinguishable from the seeded ones.
   */
  @Test
  @DisplayName("a Record written by a Transition is distinguishable from the seeded Records")
  void writtenRecords_areDistinguishableFromTheSeededOnes() {
    List<ResourceRecord> seeded = allResourceRecords(jdbcTemplate);
    assertThat(seeded).as("the seed carries a trail of its own, or this pins nothing").isNotEmpty();
    assertThat(seeded)
        .as("every seeded Record predates this test")
        .allSatisfy(record -> assertThat(record.creationDate()).isBefore(testStarted));
    authenticateAs(ADMIN);

    adapter.sendResourceEvent(NEGOTIATION, RESOURCE, "CONTACT");

    awaitState(ResourceGraphV1.target(ResourceGraphV1.INITIAL_STATE, "CONTACT"));
    LifecyclePersistence.awaitValue(
        seeded.size() + 1, () -> allResourceRecords(jdbcTemplate).size());

    List<ResourceRecord> after = allResourceRecords(jdbcTemplate);
    assertThat(after)
        .as("the seeded rows are still there, unchanged")
        .startsWith(seeded.toArray(new ResourceRecord[0]));
    assertThat(after.subList(seeded.size(), after.size()))
        .singleElement()
        .satisfies(
            written -> {
              assertThat(written.creationDate()).isAfterOrEqualTo(testStarted);
              assertThat(written.negotiationId()).isEqualTo(NEGOTIATION);
              assertThat(written.resourceSourceId()).isEqualTo(RESOURCE);
              assertThat(written.id())
                  .isGreaterThan(seeded.stream().mapToLong(ResourceRecord::id).max().orElseThrow());
            });
  }

  /** One step of a walk: an Event and the caller whose Required Authority it needs. */
  private record Step(long caller, String event) {}

  /**
   * Fires each step and returns the States the graph says the Resource passed through. It refuses an
   * edge the table does not list.
   */
  private List<String> fireAll(Step... steps) {
    List<String> visited = new ArrayList<>();
    for (Step step : steps) {
      String source = adapter.currentResourceState(NEGOTIATION, RESOURCE);
      String target = ResourceGraphV1.target(source, step.event());
      authenticateAs(step.caller());
      adapter.sendResourceEvent(NEGOTIATION, RESOURCE, step.event());
      awaitState(target);
      visited.add(target);
    }
    return visited;
  }

  private ResourceRecord lastRecord() {
    List<ResourceRecord> records = resourceRecordsOf(jdbcTemplate, NEGOTIATION);
    return records.get(records.size() - 1);
  }

  private static List<String> append(List<String> trail, String state) {
    List<String> appended = new ArrayList<>(trail);
    appended.add(state);
    return appended;
  }

  private void awaitState(String expected) {
    LifecyclePersistence.awaitState(
        expected, () -> adapter.currentResourceState(NEGOTIATION, RESOURCE));
  }
}
