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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapter;
import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapterConfig;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.common.exceptions.LifecycleConflictException;
import eu.bbmri_eric.negotiator.common.exceptions.LifecycleProblemType;
import eu.bbmri_eric.negotiator.common.exceptions.WrongRequestException;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import java.time.Duration;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Pins the Information Requirement gate as the Resource Lifecycle service enforces it.
 *
 * <p>The gate is the first thing {@code sendEvent} does. If any Information Requirement names the
 * Event and no Information Submission exists for that Resource in that Negotiation, the send is
 * refused.
 *
 * <p>The Requirement lookup is global. It asks only whether some Requirement names the Event, not
 * whether one applies to this Resource, Negotiation or Access Form, so one row anywhere blocks that
 * Event everywhere.
 *
 * <p>The Submission lookup ignores the Requirement. It asks only whether some Submission exists for
 * this Resource in this Negotiation, so a Submission against an unrelated Requirement satisfies the
 * gate. That is probably a bug, pinned so it cannot change by accident. The lookup is scoped to the
 * Resource and Negotiation, so a Submission for a sibling Resource does not help.
 *
 * <p>The gate runs before the availability check and the parent-Negotiation gate, so an unmet
 * Requirement answers {@code unmet-requirement} even where the Event would otherwise be forbidden
 * or have no Transition. Both orderings are pinned.
 *
 * <p>The refusal is a {@code LifecycleConflictException} of type {@code UNMET_REQUIREMENT}. The
 * tests pin what a caller sees: the message verbatim, the problem type, that it is none of the
 * services' other refusal types, and the HTTP response the frontend reads.
 *
 * <p>Reads after a send poll through {@link LifecyclePersistence}. {@code @DirtiesContext} per
 * method restores the seed, because the class moves a shared Resource and writes Requirement rows,
 * and the global lookup means a leftover row would block that Event for every later test.
 */
@IntegrationTest(loadTestData = true)
@AutoConfigureMockMvc
@Import(LifecycleTestAdapterConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ResourceInformationRequirementGateTest {

  /** The Event the gate is walked with, and where the graph says it leads. */
  private static final String GATED_EVENT = "CONTACT";

  /**
   * The refusal's message for {@link #GATED_EVENT}, verbatim. The exception handler copies it into
   * the problem detail the frontend shows, so users see it. It names the Event by its label.
   */
  private static final String UNMET_REQUIREMENT_MESSAGE = unmetRequirementMessage("Contact");

  /** Where the 409 problem's {@code type} points, under MockMvc's context path. */
  private static final String UNMET_REQUIREMENT_TYPE =
      "http://localhost/v3/problems/lifecycle/unmet-requirement";

  private static final String GATED_EVENT_TARGET =
      ResourceGraphV1.target(ResourceGraphV1.INITIAL_STATE, GATED_EVENT);

  /** Long enough for the asynchronous persist path to have run, had the Event been accepted. */
  private static final Duration SETTLE = Duration.ofSeconds(3);

  @Autowired LifecycleTestAdapter adapter;
  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired MockMvc mockMvc;

  @AfterEach
  void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }

  /**
   * An Event the caller is offered is still refused, because a Requirement names it and nothing has
   * been submitted. The test first asserts the Event is offered, so Required Authority cannot be the
   * cause.
   */
  @Test
  @DisplayName("an offered Event with an unmet Requirement is refused instead of fired")
  void offeredEvent_withAnUnmetRequirement_isRefused() {
    authenticateAs(ADMIN);
    assertTrue(
        adapter.possibleResourceEvents(NEGOTIATION, RESOURCE).contains(GATED_EVENT),
        GATED_EVENT + " must be offered here, or this pins nothing");

    requireInformationFor(jdbcTemplate, GATED_EVENT);

    assertUnmetRequirement(() -> adapter.sendResourceEvent(NEGOTIATION, RESOURCE, GATED_EVENT));
    awaitStillInTheInitialState();
  }

  /**
   * What a caller can rely on: the refusal is a lifecycle conflict of type {@code
   * UNMET_REQUIREMENT}, its message is the user-facing message, and it is none of the services'
   * other exceptions, so catching it never catches a missing Negotiation or a forbidden Event.
   */
  @Test
  @DisplayName(
      "the refusal is an unmet-requirement conflict carrying the user-facing message, and nothing"
          + " else")
  void theRefusal_isAnUnmetRequirementConflictAndNoneOfTheOtherRefusalTypes() {
    authenticateAs(ADMIN);
    requireInformationFor(jdbcTemplate, GATED_EVENT);

    Throwable thrown =
        catchThrowable(() -> adapter.sendResourceEvent(NEGOTIATION, RESOURCE, GATED_EVENT));

    assertThat(thrown)
        .isInstanceOfSatisfying(
            LifecycleConflictException.class,
            conflict -> assertEquals(LifecycleProblemType.UNMET_REQUIREMENT, conflict.getType()))
        .hasMessage(UNMET_REQUIREMENT_MESSAGE)
        .hasNoCause();
    assertThat(thrown)
        .as("the gate's refusal is distinct from every refusal the services raise themselves")
        .isNotInstanceOf(EntityNotFoundException.class)
        .isNotInstanceOf(ForbiddenRequestException.class)
        .isNotInstanceOf(WrongRequestException.class);
  }

  /**
   * The refusal as the frontend sees it: a 409 problem of type {@code unmet-requirement}, with the
   * gate's message as the problem detail. The test pins the title too, so rewording it fails.
   */
  @Test
  @WithUserDetails("TheBiobanker")
  @DisplayName("the refusal reaches the caller as a 409 unmet-requirement problem")
  void theRefusal_reachesTheCallerAsAnUnmetRequirementConflict() throws Exception {
    requireInformationFor(jdbcTemplate, "MARK_AS_CHECKING_AVAILABILITY");

    mockMvc
        .perform(
            MockMvcRequestBuilders.put(
                "/v3/negotiations/%s/resources/%s/lifecycle/%s"
                    .formatted(NEGOTIATION, RESOURCE, "MARK_AS_CHECKING_AVAILABILITY")))
        .andExpect(status().isConflict())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.type").value(UNMET_REQUIREMENT_TYPE))
        .andExpect(jsonPath("$.title").value("Information Requirement not met"))
        .andExpect(
            jsonPath("$.detail").value(unmetRequirementMessage("Mark as Checking Availability")));
  }

  /** With a Submission on file, the same send goes through and the Resource moves. */
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
   * The gate keys on the Event, so a Requirement for a different Event leaves this one alone, and
   * the send goes through with no Submission at all.
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
   * The Submission check ignores the Requirement, probably a bug.
   *
   * <p>The Submission here answers a Requirement for a different Event. The gate accepts it, because
   * it never compares the Submission with the blocking Requirement.
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
   * The Submission lookup ignores the Requirement but not the Resource: a Submission for a sibling
   * Resource, against the blocking Requirement, leaves the gate shut.
   */
  @Test
  @DisplayName("a Submission for a different Resource does not satisfy the gate")
  void submissionForADifferentResource_doesNotSatisfyTheGate() {
    authenticateAs(ADMIN);
    long requirement = requireInformationFor(jdbcTemplate, GATED_EVENT);
    submitInformationFor(jdbcTemplate, requirement, ANOTHER_RESOURCE_ROW_ID);

    assertUnmetRequirement(() -> adapter.sendResourceEvent(NEGOTIATION, RESOURCE, GATED_EVENT));
    awaitStillInTheInitialState();
  }

  /**
   * The gate runs before the availability check. Both cases are in one method, so they are the same
   * send by the same caller from the same State.
   *
   * <p>First the availability check refuses the Event as forbidden or as having no Transition. Then
   * the test records a Requirement for it, and the gate refuses the same send as an unmet
   * Requirement. The availability check is never reached.
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("eventsTheGraphAloneWouldRefuse")
  @DisplayName(
      "an unmet Requirement outranks the availability check: it answers before that refusal")
  void unmetRequirement_outranksTheAvailabilityCheck(
      String shape, long caller, String event, Class<? extends RuntimeException> graphRefusal) {
    authenticateAs(caller);
    assertFalse(
        adapter.possibleResourceEvents(NEGOTIATION, RESOURCE).contains(event),
        "%s must not be offered here (%s), or this pins nothing".formatted(event, shape));

    assertThatThrownBy(() -> adapter.sendResourceEvent(NEGOTIATION, RESOURCE, event))
        .as("without a Requirement the availability check refuses it")
        .isInstanceOf(graphRefusal);

    requireInformationFor(jdbcTemplate, event);

    assertThatThrownBy(() -> adapter.sendResourceEvent(NEGOTIATION, RESOURCE, event))
        .as("with a Requirement the same send is refused before availability is consulted")
        .isInstanceOfSatisfying(
            LifecycleConflictException.class,
            conflict -> assertEquals(LifecycleProblemType.UNMET_REQUIREMENT, conflict.getType()));
    awaitStillInTheInitialState();
  }

  static Stream<Arguments> eventsTheGraphAloneWouldRefuse() {
    return Stream.of(
        Arguments.of(
            "an Event this caller has no Required Authority for",
            CREATOR,
            GATED_EVENT,
            ForbiddenRequestException.class),
        Arguments.of(
            "an Event with no Transition from this State",
            ADMIN,
            "GRANT_ACCESS_TO_RESOURCE",
            LifecycleConflictException.class),
        Arguments.of(
            "an Event with no Transition anywhere in the graph",
            ADMIN,
            "OVERRIDE",
            LifecycleConflictException.class));
  }

  /**
   * The gate also runs before the parent-Negotiation gate. With the Negotiation outside IN_PROGRESS
   * the service offers nothing, but an unmet Requirement still answers first.
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

    assertUnmetRequirement(() -> adapter.sendResourceEvent(NEGOTIATION, RESOURCE, GATED_EVENT));
    awaitStillInTheInitialState();
  }

  private static String unmetRequirementMessage(String eventLabel) {
    return "%s cannot fire for this Resource until its Information Requirement form is submitted."
        .formatted(eventLabel);
  }

  private static void assertUnmetRequirement(ThrowingCallable send) {
    assertThatThrownBy(send)
        .isInstanceOfSatisfying(
            LifecycleConflictException.class,
            conflict -> assertEquals(LifecycleProblemType.UNMET_REQUIREMENT, conflict.getType()))
        .hasMessage(UNMET_REQUIREMENT_MESSAGE);
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
