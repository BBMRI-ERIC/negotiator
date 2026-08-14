package eu.bbmri_eric.negotiator.characterization.service;

import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.ADMIN;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.CREATOR;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.NEGOTIATION;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.REPRESENTATIVE;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.RESOURCE;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.authenticateAs;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.clearResourceState;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.putResourceInState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapter;
import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapterConfig;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
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
 * Fires every Transition of the Resource Lifecycle Definition graph for real, and pins how the
 * service refuses the Events it will not fire.
 *
 * <p>Every State and Event is named as a string literal. Nothing here imports Spring Statemachine
 * or a Lifecycle enum, so these assertions can be re-run byte-identical against the replacement
 * subsystem - only {@link LifecycleTestAdapter}'s implementation changes. The table being driven is
 * {@link ResourceGraphV1}, which {@link ResourceGraphV1BindingTest} equates edge for edge to the
 * committed mechanical dump; this class supplies the other half of the argument, that the graph the
 * dump describes is the graph the service actually walks.
 *
 * <p><b>The subject.</b> {@link SeededResourceSubject} - which is also where the three seeded
 * callers, the {@code source_id}-not-row-id rule and the hand-rolled authentication are explained.
 *
 * <p><b>Asynchrony.</b> {@code sendEvent} drives the persist handler through {@code
 * handleEventWithStateReactively(...).subscribe()} and returns the State read back immediately -
 * that is, usually the State the Resource was already in. Every assertion about the resulting State
 * therefore polls through {@link LifecyclePersistence} under a bounded timeout, and no method here
 * is transactional.
 *
 * <p>{@code @DirtiesContext} per method rebuilds the context, and with it Flyway's
 * clean-and-migrate of the seeded data, because these tests move shared seeded rows. That is the
 * rule ticket 03 left behind for this class: a Resource driven to a terminal State can conclude
 * {@code negotiation-1}, which {@code NegotiationAuthorityParityTest} reads expecting IN_PROGRESS,
 * so a driving class that did not restore the seed could turn ticket 03 red by test ordering alone.
 */
@IntegrationTest(loadTestData = true)
@Import(LifecycleTestAdapterConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ResourceTransitionParityTest {

  /** A Resource of the seed that {@code negotiation-1} has no link row for. */
  private static final String UNLINKED_RESOURCE = "biobank:3:collection:1";

  /** Which seeded caller satisfies which of the graph's three Required Authority rules. */
  private static final Map<String, Long> CALLER_SATISFYING =
      Map.of(
          ResourceGraphV1.IS_ADMIN, ADMIN,
          ResourceGraphV1.IS_REPRESENTATIVE, REPRESENTATIVE,
          ResourceGraphV1.IS_CREATOR, CREATOR);

  /** Long enough for the asynchronous persist path to have run had the Event been accepted. */
  private static final Duration SETTLE = Duration.ofSeconds(3);

  @Autowired LifecycleTestAdapter adapter;
  @Autowired JdbcTemplate jdbcTemplate;

  @AfterEach
  void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }

  /**
   * The thirteen Transitions of the graph, taken from the table the dump is bound to rather than
   * retyped here, so this test cannot drift from the committed artifact.
   */
  static Stream<Arguments> transitions() {
    return ResourceGraphV1.TRANSITIONS.stream()
        .map(
            edge ->
                Arguments.of(
                    edge.source(),
                    edge.event(),
                    edge.target(),
                    CALLER_SATISFYING.get(edge.requiredAuthority())));
  }

  @ParameterizedTest(name = "{0} --{1}--> {2}")
  @MethodSource("transitions")
  @DisplayName("every Transition of the Resource graph moves the Resource to its target State")
  void transition_movesResourceToTargetState(
      String source, String event, String target, long caller) {
    putResourceInState(jdbcTemplate, source);
    authenticateAs(caller);

    assertEquals(source, adapter.currentResourceState(NEGOTIATION, RESOURCE));
    assertTrue(
        adapter.possibleResourceEvents(NEGOTIATION, RESOURCE).contains(event),
        "%s should be offered from %s to caller %d".formatted(event, source, caller));

    adapter.sendResourceEvent(NEGOTIATION, RESOURCE, event);

    awaitState(target);
  }

  /**
   * The same graph walked as one Lifecycle rather than thirteen independent statements, from the
   * seeded initial State to delivery, using nothing but Events. Each step's target is looked up in
   * the pinned table from the State the Resource is actually in, so the walk can never quietly
   * follow an edge the table does not claim.
   *
   * <p>The walk arrives at a State the graph never leaves, which is the fact worth asserting at the
   * end - {@code fire} has already waited for the Transition itself to land, so re-reading the
   * State would only restate its last step.
   */
  @Test
  @DisplayName(
      "the delivery chain runs end to end from the initial State, changing caller at every gate")
  void deliveryChain_reachesResourceMadeAvailable() {
    assertEquals(
        ResourceGraphV1.INITIAL_STATE, adapter.currentResourceState(NEGOTIATION, RESOURCE));

    fire(ADMIN, "CONTACT");
    fire(REPRESENTATIVE, "MARK_AS_CHECKING_AVAILABILITY");
    fire(REPRESENTATIVE, "MARK_AS_AVAILABLE");
    fire(REPRESENTATIVE, "INDICATE_ACCESS_CONDITIONS");
    fire(CREATOR, "ACCEPT_ACCESS_CONDITIONS");
    fire(REPRESENTATIVE, "GRANT_ACCESS_TO_RESOURCE");

    assertThat(adapter.possibleResourceEvents(NEGOTIATION, RESOURCE))
        .as("delivery is the end of the chain: the graph offers nothing out of it")
        .isEmpty();
  }

  /**
   * The refusal asymmetry, pinned deliberately.
   *
   * <p>An Event that is not offered leaves the Resource Lifecycle service silent: it returns the
   * unchanged current State and raises nothing at all. The Negotiation Lifecycle service refuses
   * the same situation by throwing {@code ForbiddenRequestException}. Callers depend on that
   * difference today, so it is frozen here rather than smoothed over - making the two consistent
   * later must be a decision that breaks this test, not an accident nobody notices.
   *
   * <p>The {@code pollDelay} is what makes "silent" mean something: without it, a no-op and a
   * transition that simply had not landed yet would look the same. The State is read only after the
   * asynchronous persist path has had time to run.
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("refusals")
  @DisplayName(
      "an Event the caller may not fire returns the unchanged State and raises nothing"
          + " - unlike the Negotiation service, which throws")
  void unofferedEvent_returnsUnchangedStateAndRaisesNothing_unlikeTheNegotiationService(
      String shapeOfTheRefusal, long caller, String event) {
    authenticateAs(caller);
    assertEquals(
        ResourceGraphV1.INITIAL_STATE, adapter.currentResourceState(NEGOTIATION, RESOURCE));
    assertFalse(
        adapter.possibleResourceEvents(NEGOTIATION, RESOURCE).contains(event),
        "%s must not be offered here (%s), or this pins nothing"
            .formatted(event, shapeOfTheRefusal));

    assertSilentlyRefused(event);

    awaitStillInTheInitialState();
  }

  static Stream<Arguments> refusals() {
    return Stream.of(
        Arguments.of("an Event this caller has no Required Authority for", CREATOR, "CONTACT"),
        Arguments.of(
            "an Event with no Transition from this State", ADMIN, "GRANT_ACCESS_TO_RESOURCE"));
  }

  /**
   * An Event that exists in the Definition but sits on no Transition is refused exactly like an
   * Event the caller has no authority for: silently. Both {@code RETURN_FOR_RESUBMISSION} and
   * {@code OVERRIDE} are declared only because the configuration registers the whole enum, and the
   * frozen dump records no Transition carrying either. The caller here is an admin who is being
   * offered Events at the same moment, so the refusal cannot be confused with the IN_PROGRESS gate.
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("eventsOnNoTransition")
  @DisplayName("an Event that sits on no Transition is refused silently, for an admin too")
  void eventOnNoTransition_isSilentlyRefused(String event) {
    authenticateAs(ADMIN);
    assertTrue(adapter.possibleResourceEvents(NEGOTIATION, RESOURCE).contains("CONTACT"));

    assertSilentlyRefused(event);

    awaitStillInTheInitialState();
  }

  static Stream<Arguments> eventsOnNoTransition() {
    return ResourceGraphV1.EVENTS_ON_NO_TRANSITION.stream().sorted().map(Arguments::of);
  }

  /**
   * The one place the silent refusal is not silent.
   *
   * <p>"Returns the unchanged current State" needs a current State to return. The refusal path
   * reads it back unguarded, so when the link row records no State the read raises {@code
   * EntityNotFoundException} - carrying the Negotiation's id, not the Resource's, because that is
   * the argument the lookup was written with. So the asymmetry with the Negotiation service is
   * narrower than it looks: the Resource service is silent only where there is something to be
   * silent about.
   */
  @Test
  @DisplayName("refusing an Event for a linked Resource with no recorded State is not silent")
  void sendEvent_toALinkedResourceWithoutACurrentState_raisesEntityNotFound() {
    authenticateAs(ADMIN);
    clearResourceState(jdbcTemplate);

    assertThatThrownBy(() -> adapter.sendResourceEvent(NEGOTIATION, RESOURCE, "CONTACT"))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining(NEGOTIATION);
  }

  /**
   * A Resource the Negotiation has no link row for at all fails identically - the State lookup
   * finds nothing either way, so "not linked" and "linked but blank" are indistinguishable to a
   * caller. The subject Resource is deliberately left alone here: it is not what is being sent to.
   */
  @Test
  @DisplayName("refusing an Event for a Resource not linked to the Negotiation is not silent")
  void sendEvent_toAResourceNotLinkedToTheNegotiation_raisesEntityNotFound() {
    authenticateAs(ADMIN);

    assertThatThrownBy(() -> adapter.sendResourceEvent(NEGOTIATION, UNLINKED_RESOURCE, "CONTACT"))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining(NEGOTIATION);
  }

  /**
   * Fires {@code event} as {@code caller} and waits for the State the pinned table says it leads
   * to, from wherever the Resource currently stands.
   */
  private void fire(long caller, String event) {
    String source = adapter.currentResourceState(NEGOTIATION, RESOURCE);
    String target = ResourceGraphV1.target(source, event);
    authenticateAs(caller);
    assertTrue(
        adapter.possibleResourceEvents(NEGOTIATION, RESOURCE).contains(event),
        "%s should be offered from %s to caller %d".formatted(event, source, caller));

    adapter.sendResourceEvent(NEGOTIATION, RESOURCE, event);

    awaitState(target);
  }

  /**
   * The two halves of "silently refused", asserted separately so that a wrong returned State is
   * reported as a wrong State rather than as an unexpected throwable.
   */
  private void assertSilentlyRefused(String event) {
    AtomicReference<String> returned = new AtomicReference<>();

    assertThatCode(() -> returned.set(adapter.sendResourceEvent(NEGOTIATION, RESOURCE, event)))
        .as("the Resource service refuses without raising anything")
        .doesNotThrowAnyException();

    assertEquals(
        ResourceGraphV1.INITIAL_STATE,
        returned.get(),
        "a refused Event returns the unchanged current State");
  }

  private void awaitState(String expected) {
    LifecyclePersistence.awaitState(
        expected, () -> adapter.currentResourceState(NEGOTIATION, RESOURCE));
  }

  private void awaitStillInTheInitialState() {
    LifecyclePersistence.awaitStateAfterSettling(
        SETTLE,
        ResourceGraphV1.INITIAL_STATE,
        () -> adapter.currentResourceState(NEGOTIATION, RESOURCE));
  }
}
