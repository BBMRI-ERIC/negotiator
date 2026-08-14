package eu.bbmri_eric.negotiator.characterization.service;

import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.ADMIN;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.ANOTHER_RESOURCE_ROW_ID;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.CREATOR;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.NEGOTIATION;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.RESOURCE;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.authenticateAs;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.putNegotiationInState;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.requireInformationFor;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.submitInformationFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapter;
import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapterConfig;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.common.exceptions.WrongRequestException;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Pins the Information Requirement gate exactly as the Resource Lifecycle service enforces it
 * today.
 *
 * <p>The gate is the very first thing {@code sendEvent} does, ahead of every other question the
 * service asks. If any Information Requirement is recorded for the Event being sent and no
 * Information Submission exists for that Resource in that Negotiation, the send is refused. ADR
 * 0005 turns this into a Built-in Stage, so the block it has to reproduce is written down here.
 *
 * <p><b>Three facts about the gate are sharper than its description.</b>
 *
 * <p><i>The Requirement lookup is global.</i> It asks only whether some Requirement names the Event
 * - not whether one applies to this Resource, this Negotiation, or this Access Form. One row
 * anywhere in the table blocks that Event everywhere.
 *
 * <p><i>The Submission lookup is not scoped to the Requirement.</i> It asks only whether some
 * Submission exists for this Resource in this Negotiation, so a Submission filed against an
 * unrelated Requirement satisfies the gate for all of them. That is very likely a bug; it is frozen
 * here precisely so the cutover cannot change it by accident. Its one boundary is pinned too: the
 * lookup <em>is</em> scoped to the Resource and the Negotiation, so a Submission for a sibling
 * Resource does not help.
 *
 * <p><i>The gate outranks the availability check and the parent-Negotiation gate.</i> A blocked
 * Event and an Event that merely is not currently offered therefore behave completely differently -
 * the first throws, the second returns the unchanged State in silence. Both orderings are pinned.
 *
 * <p><b>The refusal's type is a deliberate delta, and only its observable half is asserted.</b>
 * Today the throwable is Spring Statemachine's {@code StateMachineException}, a class the cutover
 * deletes, so an assertion naming it could only ever go red - the mistake ticket 09 recorded. What
 * is pinned instead is what a caller actually observes: the message text verbatim, that the
 * throwable is unchecked, that it is none of the service's own refusal types, and the HTTP response
 * the frontend reads. The type itself is recorded as a before-picture in ticket 05's findings,
 * where the cutover has to make a deliberate choice about it.
 *
 * <p><b>Asynchrony and ordering.</b> Reads after a send poll through {@link LifecyclePersistence}
 * under a bounded timeout, never a sleep. {@code @DirtiesContext} per method restores the seeded
 * corpus - this class both moves a shared Resource and writes Requirement rows the seed does not
 * carry, and the Requirement lookup is global, so a leftover row would block that Event for every
 * later test in the run.
 */
@IntegrationTest(loadTestData = true)
@AutoConfigureMockMvc
@Import(LifecycleTestAdapterConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ResourceInformationRequirementGateTest {

  /**
   * The refusal's message, verbatim. It is user-facing: the exception handler copies it into the
   * problem detail the frontend surfaces, so it is part of the contract and not an internal string.
   */
  private static final String UNMET_REQUIREMENT_MESSAGE =
      "The requirement for this operation was not met."
          + " Please make sure you have submitted the required form and try again.";

  /** The Event the gate is walked with, and where the graph says it leads. */
  private static final String GATED_EVENT = "CONTACT";

  private static final String GATED_EVENT_TARGET =
      ResourceGraphV1.target(ResourceGraphV1.INITIAL_STATE, GATED_EVENT);

  /** Long enough for the asynchronous persist path to have run had the Event been accepted. */
  private static final Duration SETTLE = Duration.ofSeconds(3);

  @Autowired LifecycleTestAdapter adapter;
  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired MockMvc mockMvc;

  @AfterEach
  void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }

  /**
   * The gate itself: an Event the caller is being offered at this very moment is refused anyway,
   * because a Requirement names it and nothing has been submitted. The Event is asserted to be
   * offered first, so the refusal cannot be confused with any of the Required Authority rules.
   */
  @Test
  @DisplayName("an offered Event with an unmet Requirement is refused instead of fired")
  void offeredEvent_withAnUnmetRequirement_isRefused() {
    authenticateAs(ADMIN);
    assertTrue(
        adapter.possibleResourceEvents(NEGOTIATION, RESOURCE).contains(GATED_EVENT),
        GATED_EVENT + " must be offered here, or this pins nothing");

    requireInformationFor(jdbcTemplate, GATED_EVENT);

    assertThatThrownBy(() -> adapter.sendResourceEvent(NEGOTIATION, RESOURCE, GATED_EVENT))
        .hasMessage(UNMET_REQUIREMENT_MESSAGE);
    awaitStillInTheInitialState();
  }

  /**
   * What a caller can rely on about the refusal without naming a class the cutover deletes: it is
   * unchecked, it carries the user-facing message as its own message and nothing else, and it is
   * none of the exceptions the Lifecycle services raise for their own reasons - so catching the
   * gate's refusal can never accidentally catch a missing Negotiation or a forbidden Event.
   */
  @Test
  @DisplayName(
      "the refusal is unchecked, carries the user-facing message, and is a type of its own")
  void theRefusal_isUncheckedAndIsNoneOfTheServicesOwnRefusalTypes() {
    authenticateAs(ADMIN);
    requireInformationFor(jdbcTemplate, GATED_EVENT);

    Throwable thrown =
        catchThrowable(() -> adapter.sendResourceEvent(NEGOTIATION, RESOURCE, GATED_EVENT));

    assertThat(thrown)
        .isInstanceOf(RuntimeException.class)
        .hasMessage(UNMET_REQUIREMENT_MESSAGE)
        .hasNoCause();
    assertThat(thrown)
        .as("the gate's refusal is distinct from every refusal the services raise themselves")
        .isNotInstanceOf(EntityNotFoundException.class)
        .isNotInstanceOf(ForbiddenRequestException.class)
        .isNotInstanceOf(WrongRequestException.class);
  }

  /**
   * The refusal as the frontend sees it, which is the half of "the exception type" that has to
   * survive the cutover: 400, with the gate's message as the problem detail. The title is today's
   * prose and is pinned with it, so rewording it is a decision rather than a drift.
   */
  @Test
  @WithUserDetails("TheBiobanker")
  @DisplayName("the refusal reaches the caller as 400 with the message as the problem detail")
  void theRefusal_reachesTheCallerAsBadRequestCarryingTheMessage() throws Exception {
    requireInformationFor(jdbcTemplate, "MARK_AS_CHECKING_AVAILABILITY");

    mockMvc
        .perform(
            MockMvcRequestBuilders.put(
                "/v3/negotiations/%s/resources/%s/lifecycle/%s"
                    .formatted(NEGOTIATION, RESOURCE, "MARK_AS_CHECKING_AVAILABILITY")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.title").value("Could not advance the state machine"))
        .andExpect(jsonPath("$.detail").value(UNMET_REQUIREMENT_MESSAGE));
  }

  /**
   * With a Submission of its own on file, the very same send goes through and the Resource moves.
   */
  @Test
  @DisplayName("the same send succeeds once a Submission exists for this Resource and Negotiation")
  void unmetRequirement_isMetByASubmission() {
    authenticateAs(ADMIN);
    long requirement = requireInformationFor(jdbcTemplate, GATED_EVENT);
    submitInformationFor(jdbcTemplate, requirement);

    adapter.sendResourceEvent(NEGOTIATION, RESOURCE, GATED_EVENT);

    awaitState(GATED_EVENT_TARGET);
  }

  /**
   * The gate is keyed on the Event, so a Requirement for a different Event leaves this one alone -
   * no Submission exists at all here and the send still goes through.
   */
  @Test
  @DisplayName("an Event no Requirement names is unaffected by the gate")
  void eventWithNoRequirement_isNotGated() {
    authenticateAs(ADMIN);
    requireInformationFor(jdbcTemplate, "MARK_AS_CHECKING_AVAILABILITY");

    adapter.sendResourceEvent(NEGOTIATION, RESOURCE, GATED_EVENT);

    awaitState(GATED_EVENT_TARGET);
  }

  /**
   * The unscoped Submission check, pinned as the bug it probably is.
   *
   * <p>The Submission filed here belongs to a Requirement for a different Event entirely and could
   * not possibly answer the one blocking this send. The gate accepts it, because it never compares
   * the Submission to the Requirement it is standing in for.
   */
  @Test
  @DisplayName("a Submission against a different Requirement satisfies the gate")
  void submissionAgainstADifferentRequirement_satisfiesTheGate() {
    authenticateAs(ADMIN);
    requireInformationFor(jdbcTemplate, GATED_EVENT);
    long unrelatedRequirement = requireInformationFor(jdbcTemplate, "MARK_AS_UNREACHABLE");
    submitInformationFor(jdbcTemplate, unrelatedRequirement);

    adapter.sendResourceEvent(NEGOTIATION, RESOURCE, GATED_EVENT);

    awaitState(GATED_EVENT_TARGET);
  }

  /**
   * The boundary of the previous statement. The Submission lookup ignores the Requirement but not
   * the Resource: a Submission filed for a sibling Resource of the same Negotiation, against the
   * very Requirement that is blocking, leaves the gate shut.
   */
  @Test
  @DisplayName("a Submission for a different Resource does not satisfy the gate")
  void submissionForADifferentResource_doesNotSatisfyTheGate() {
    authenticateAs(ADMIN);
    long requirement = requireInformationFor(jdbcTemplate, GATED_EVENT);
    submitInformationFor(jdbcTemplate, requirement, ANOTHER_RESOURCE_ROW_ID);

    assertThatThrownBy(() -> adapter.sendResourceEvent(NEGOTIATION, RESOURCE, GATED_EVENT))
        .hasMessage(UNMET_REQUIREMENT_MESSAGE);
    awaitStillInTheInitialState();
  }

  /**
   * The gate's precedence over the availability check, walked in both directions within one method
   * so the two outcomes are the same send under the same caller from the same State.
   *
   * <p>First the Event is refused the way ticket 04 pinned - silently, returning the unchanged
   * State. Then a Requirement is recorded for it and the identical send throws instead. The
   * availability check is never reached, which is why an Event that could not have been fired
   * anyway still produces a user-facing error.
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("eventsTheGraphAloneWouldRefuseSilently")
  @DisplayName(
      "an unmet Requirement outranks the availability check: it throws where that is silent")
  void unmetRequirement_outranksTheAvailabilityCheck(String shape, long caller, String event) {
    authenticateAs(caller);
    assertFalse(
        adapter.possibleResourceEvents(NEGOTIATION, RESOURCE).contains(event),
        "%s must not be offered here (%s), or this pins nothing".formatted(event, shape));

    assertEquals(
        ResourceGraphV1.INITIAL_STATE,
        adapter.sendResourceEvent(NEGOTIATION, RESOURCE, event),
        "without a Requirement the refusal is silent and returns the unchanged State");

    requireInformationFor(jdbcTemplate, event);

    assertThatThrownBy(() -> adapter.sendResourceEvent(NEGOTIATION, RESOURCE, event))
        .as("with a Requirement the same send throws before availability is consulted")
        .hasMessage(UNMET_REQUIREMENT_MESSAGE);
    awaitStillInTheInitialState();
  }

  static Stream<Arguments> eventsTheGraphAloneWouldRefuseSilently() {
    return Stream.of(
        Arguments.of("an Event this caller has no Required Authority for", CREATOR, GATED_EVENT),
        Arguments.of(
            "an Event with no Transition from this State", ADMIN, "GRANT_ACCESS_TO_RESOURCE"),
        Arguments.of("an Event with no Transition anywhere in the graph", ADMIN, "OVERRIDE"));
  }

  /**
   * The gate sits ahead of the parent-Negotiation gate as well. With the Negotiation out of the
   * State that lets any Event be offered at all, the service would normally offer nothing and
   * refuse in silence; an unmet Requirement still throws first.
   */
  @Test
  @DisplayName("an unmet Requirement outranks the parent Negotiation gate too")
  void unmetRequirement_outranksTheParentNegotiationGate() {
    authenticateAs(ADMIN);
    putNegotiationInState(jdbcTemplate, NegotiationGraphV1.INITIAL_STATE);
    assertThat(adapter.possibleResourceEvents(NEGOTIATION, RESOURCE))
        .as(
            "the parent Negotiation gate offers nothing outside %s",
            ResourceGraphV1.REQUIRED_PARENT_STATE)
        .isEmpty();

    requireInformationFor(jdbcTemplate, GATED_EVENT);

    assertThatThrownBy(() -> adapter.sendResourceEvent(NEGOTIATION, RESOURCE, GATED_EVENT))
        .hasMessage(UNMET_REQUIREMENT_MESSAGE);
    awaitStillInTheInitialState();
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
