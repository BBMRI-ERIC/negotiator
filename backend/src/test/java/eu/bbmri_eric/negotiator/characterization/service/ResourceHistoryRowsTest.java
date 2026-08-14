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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapter;
import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapterConfig;
import eu.bbmri_eric.negotiator.characterization.service.LifecycleHistory.ResourceRecord;
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
 * <p>The same ADR 0008 conversion applies to this table, so the same discipline applies to these
 * assertions: no column is named here, every read goes through {@link LifecycleHistory}, and a
 * recorded State is a string. The Resource is identified by its {@code source_id} rather than by
 * the row id the Record actually stores, for the reason {@link SeededResourceSubject} gives.
 *
 * <p><b>The interesting half of the refused-send question lives here.</b> The Resource service
 * refuses silently - it returns the unchanged current State and raises nothing - so "a refusal
 * writes no Record" cannot be inferred from an exception the way it can for a Negotiation. It is
 * asserted directly, after settling, for all three shapes of refusal ticket 04 pinned.
 *
 * <p><b>The subject.</b> {@link SeededResourceSubject}: {@code negotiation-1}, its one linked
 * Resource, and the three seeded callers that satisfy the graph's three Required Authority rules.
 * Unlike the Negotiation subject, this one starts with a seeded Record of its own - which is what
 * makes the seeded-versus-written distinction worth pinning rather than assuming - so every
 * expectation below is the trail as it stood before the send, plus what the send added.
 *
 * <p><b>Asynchrony.</b> As in the sibling class: bounded polling through {@link
 * LifecyclePersistence}, the target State awaited first, and a settling period wherever the claim
 * is that no row was written.
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

  /** Long enough for the asynchronous persist path to have written a row had it been going to. */
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
   * The core statement, made for all thirteen Transitions: firing an Event appends exactly one row
   * to the Resource's trail, naming the State the pinned graph says the Transition leads to, and
   * that row names both the Negotiation and the Resource the Event was fired at.
   *
   * <p>The expectation is computed from {@link ResourceGraphV1}, which {@link
   * ResourceGraphV1BindingTest} binds edge for edge to the committed mechanical dump.
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
   * The destination-only question again, and it answers the same way. {@code CONTACT} reaches
   * {@code REPRESENTATIVE_CONTACTED} from two different States, and the two rows are equal once
   * identity and timestamps are taken out - so a Resource's trail says where its Lifecycle ended up
   * and nothing about which Transition put it there.
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
        .as("the rows are told apart by insertion order, and by nothing else that is not a clock")
        .isGreaterThan(fromSubmitted.id());
  }

  /**
   * The delivery chain, walked as one Lifecycle and read back as one trail: six Transitions in
   * sequence leave six rows, appended after the seeded one, in the order they happened.
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
   * The precondition ADR 0009's backfill rests on, for the larger of the two tables: it resolves
   * the whole table by State name, so a row naming something the Definition does not declare would
   * not resolve to a State row at all.
   *
   * <p>Asserted over the whole table - the rows the seed carries for two different Negotiations,
   * and the ones a driven path just added - against {@link ResourceGraphV1#allStateNames()}, which
   * {@link ResourceGraphV1BindingTest} equates to the committed dump's States and to the States the
   * metadata endpoint publishes. The universe that has to hold is the declared one, Legacy State
   * included.
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
        arguments("an Event this caller has no Required Authority for", CREATOR, "CONTACT"),
        arguments("an Event with no Transition from this State", ADMIN, "GRANT_ACCESS_TO_RESOURCE"),
        arguments("an Event that sits on no Transition at all", ADMIN, "OVERRIDE"));
  }

  /**
   * The negative test the ticket asks for explicitly, because this service's refusal returns
   * normally: a refused send is not merely quiet, it is also inert.
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("refusals")
  @DisplayName("a refused send writes no Record, despite returning normally")
  void refusedSend_writesNoRecord_despiteReturningNormally(
      String shapeOfTheRefusal, long caller, String event) {
    authenticateAs(caller);
    List<ResourceRecord> before = allResourceRecords(jdbcTemplate);

    assertThatCode(() -> adapter.sendResourceEvent(NEGOTIATION, RESOURCE, event))
        .as("the Resource service refuses without raising - %s", shapeOfTheRefusal)
        .doesNotThrowAnyException();

    LifecyclePersistence.awaitValueAfterSettling(
        SETTLE,
        before,
        () -> allResourceRecords(jdbcTemplate),
        "a refused Event leaves every Resource trail exactly as it found it");
  }

  /**
   * The seeded trail and the written one, told apart. The seed carries Resource Records for this
   * Negotiation and for another, and ADR 0009's backfill joins on State names across the whole
   * table, so the rows a Transition adds have to be separable from the rows that were already
   * there.
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
   * Fires each step from wherever the Resource stands, and returns the States the pinned graph says
   * it passed through - so the walk can never quietly follow an edge the table does not claim.
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
