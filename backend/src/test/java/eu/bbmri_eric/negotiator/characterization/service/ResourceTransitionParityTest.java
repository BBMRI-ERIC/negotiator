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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapter;
import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapterConfig;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.common.exceptions.LifecycleConflictException;
import eu.bbmri_eric.negotiator.common.exceptions.LifecycleProblemType;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import java.time.Duration;
import java.util.Map;
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
 * service refuses the Events it will not fire: by throwing, like the Negotiation service.
 *
 * <p>The rows come from {@link ResourceGraphV1}, which {@link ResourceGraphV1BindingTest} binds to
 * the committed dump; this class shows the service walks the graph the dump describes. {@link
 * SeededResourceSubject} explains the subject, the callers, and why a Resource is named by its
 * {@code source_id}.
 *
 * <p>{@code sendEvent} persists through {@code handleEventWithStateReactively(...).subscribe()}
 * and returns the State read back at once, usually the old one. So every check of the resulting
 * State polls through {@link LifecyclePersistence}, and no method is transactional.
 *
 * <p>{@code @DirtiesContext} per method restores the seed. A Resource moved to a terminal State
 * can conclude {@code negotiation-1}, which {@code NegotiationAuthorityParityTest} expects to find
 * IN_PROGRESS.
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

  /** Long enough for the asynchronous persist path to have run, had the Event been accepted. */
  private static final Duration SETTLE = Duration.ofSeconds(3);

  @Autowired LifecycleTestAdapter adapter;
  @Autowired JdbcTemplate jdbcTemplate;

  @AfterEach
  void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }

  /** The thirteen Transitions, taken from the table bound to the dump. */
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
   * The same graph walked as one Lifecycle, from the seeded initial State to delivery, using only
   * Events. Each step's target comes from the table, looked up from the Resource's current State.
   *
   * <p>The test ends by asserting the graph offers nothing out of the final State; {@code fire} has
   * already waited for each step to land.
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
   * A caller without the Transition's Required Authority gets {@code ForbiddenRequestException},
   * whose message is built from the Event's label in lower case. Users see it, so the test pins
   * it verbatim.
   *
   * <p>The settle afterwards lets the persist path run; without it, a refusal and a Transition that
   * has not landed yet look the same.
   */
  @Test
  @DisplayName("an Event the caller has no Required Authority for is forbidden")
  void eventWithoutRequiredAuthority_isForbidden() {
    authenticateAs(CREATOR);
    assertEquals(
        ResourceGraphV1.INITIAL_STATE, adapter.currentResourceState(NEGOTIATION, RESOURCE));
    assertFalse(
        adapter.possibleResourceEvents(NEGOTIATION, RESOURCE).contains("CONTACT"),
        "CONTACT must not be offered to the creator, or this pins nothing");

    assertThatThrownBy(() -> adapter.sendResourceEvent(NEGOTIATION, RESOURCE, "CONTACT"))
        .isInstanceOf(ForbiddenRequestException.class)
        .hasMessage("You are not allowed to contact for this Resource");

    awaitStillInTheInitialState();
  }

  /**
   * An Event with no Transition from the current State is a conflict, for an admin too, and the
   * same as on the Negotiation side. {@code RETURN_FOR_RESUBMISSION} and {@code OVERRIDE} sit on no
   * Transition at all; they are declared only because the configuration registers the whole enum.
   * The caller is an admin who is offered other Events at the same time, so the IN_PROGRESS gate
   * cannot be the cause.
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("eventsWithNoTransitionFromTheInitialState")
  @DisplayName("an Event with no Transition from the current State is a no-transition conflict")
  void eventWithNoTransition_isANoTransitionConflict(String event) {
    authenticateAs(ADMIN);
    assertTrue(adapter.possibleResourceEvents(NEGOTIATION, RESOURCE).contains("CONTACT"));

    assertThatThrownBy(() -> adapter.sendResourceEvent(NEGOTIATION, RESOURCE, event))
        .isInstanceOfSatisfying(
            LifecycleConflictException.class,
            conflict -> assertEquals(LifecycleProblemType.NO_TRANSITION, conflict.getType()))
        .hasMessageStartingWith(
            "The Resource is Submitted, and no Transition leaves that State on ");

    awaitStillInTheInitialState();
  }

  /** One Event with Transitions elsewhere in the graph, and the two that sit on none. */
  static Stream<Arguments> eventsWithNoTransitionFromTheInitialState() {
    return Stream.concat(
            Stream.of("GRANT_ACCESS_TO_RESOURCE"),
            ResourceGraphV1.EVENTS_ON_NO_TRANSITION.stream().sorted())
        .map(Arguments::of);
  }

  /**
   * The service reads the current State before it looks for a Transition, so when the link row has
   * no State it throws {@code EntityNotFoundException}. The message carries the Negotiation's id,
   * not the Resource's, because the lookup takes the Negotiation's id.
   */
  @Test
  @DisplayName("firing at a linked Resource with no recorded State fails with not found")
  void sendEvent_toALinkedResourceWithoutACurrentState_raisesEntityNotFound() {
    authenticateAs(ADMIN);
    clearResourceState(jdbcTemplate);

    assertThatThrownBy(() -> adapter.sendResourceEvent(NEGOTIATION, RESOURCE, "CONTACT"))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining(NEGOTIATION);
  }

  /**
   * A Resource with no link row fails the same way, because the State lookup finds nothing either
   * way. The test sends to another Resource, not the subject.
   */
  @Test
  @DisplayName("firing at a Resource not linked to the Negotiation fails with not found")
  void sendEvent_toAResourceNotLinkedToTheNegotiation_raisesEntityNotFound() {
    authenticateAs(ADMIN);

    assertThatThrownBy(() -> adapter.sendResourceEvent(NEGOTIATION, UNLINKED_RESOURCE, "CONTACT"))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining(NEGOTIATION);
  }

  /**
   * Fires {@code event} as {@code caller} and waits for the State the table says it leads to from
   * the Resource's current State.
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
