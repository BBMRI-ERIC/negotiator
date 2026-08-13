package eu.bbmri_eric.negotiator.characterization.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapter;
import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapterConfig;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.util.WithMockNegotiatorUser;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Pins who the Negotiation Lifecycle offers what, and how it refuses everything else.
 *
 * <p>Authority is decided in two steps today and both are pinned. First a blanket one: a caller who
 * is neither an admin nor the Negotiation's creator is offered nothing at all, whatever State the
 * Negotiation is in and whatever Resources they represent. Then a per-Transition one: of the
 * Transitions leaving the current State, one survives if it carries no Required Authority, or if
 * its attributes intersect the caller's Spring roles. Exactly two Transitions - {@code APPROVE} and
 * {@code DECLINE} out of {@code SUBMITTED} - carry {@code ROLE_ADMIN}, so they are the entire
 * difference between an admin's offering and its creator's.
 *
 * <p>Refusal is the other half. Firing an Event that is not currently offered raises {@code
 * ForbiddenRequestException} with a message built from the Event's own label - user-visible text,
 * pinned verbatim.
 *
 * <p>Every test here reads; none drives a Transition, because a refused Event changes nothing and a
 * State's offering is a query. That is why the class carries no per-method {@code @DirtiesContext}:
 * the seeded corpus it reads is still the seeded corpus when it finishes. The subjects are the four
 * States seeded data already occupies, all four created by person 108.
 *
 * <p>It does declare {@code BEFORE_CLASS} instead, which is not about what this class leaves behind
 * but about what it inherits. Reading a seeded State is only meaningful against the seed, and the
 * Flyway strategy reloads the seed whenever a context is built - so forcing a fresh context before
 * the class makes these assertions independent of whatever test class ran previously and of whether
 * that class cleaned up after itself. It costs nothing in a suite where every other class already
 * dirties the context after itself.
 *
 * <p>The three States nothing is seeded in - {@code PAUSED}, {@code DECLINED}, {@code CONCLUDED} -
 * have their offerings pinned by {@link NegotiationTransitionParityTest}, which can afford to drive
 * a Negotiation into them.
 */
@IntegrationTest(loadTestData = true)
@Import(LifecycleTestAdapterConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class NegotiationAuthorityParityTest {

  /** Seeded {@code SUBMITTED}, created by 108, and free of Resources. */
  private static final String SUBMITTED_NEGOTIATION = "negotiation-2";

  /** Seeded {@code IN_PROGRESS}, created by 108, holding Resource 4 - represented by 109. */
  private static final String IN_PROGRESS_NEGOTIATION = "negotiation-1";

  private static final String NO_SUCH_NEGOTIATION = "no-such-negotiation";

  @Autowired LifecycleTestAdapter adapter;

  /** The four States the seeded corpus occupies, each in a Negotiation created by person 108. */
  static Stream<Arguments> seededStates() {
    return Stream.of(
        arguments("negotiation-6", "DRAFT"),
        arguments("negotiation-2", "SUBMITTED"),
        arguments("negotiation-1", "IN_PROGRESS"),
        arguments("negotiation-v2", "ABANDONED"));
  }

  @ParameterizedTest(name = "an admin at {1}")
  @MethodSource("seededStates")
  @DisplayName("an admin is offered every Transition leaving the current State")
  @WithMockNegotiatorUser(id = 101L, authorities = "ROLE_ADMIN")
  void possibleEvents_asAdmin_areEveryTransitionFromTheState(String negotiationId, String state) {
    assertEquals(state, adapter.currentNegotiationState(negotiationId));
    assertEquals(
        NegotiationGraphV1.possibleEventsForAdmin(state),
        adapter.possibleNegotiationEvents(negotiationId));
  }

  @ParameterizedTest(name = "the creator at {1}")
  @MethodSource("seededStates")
  @DisplayName("the creator is offered the same Transitions, less the admin-only ones")
  @WithUserDetails("TheResearcher")
  void possibleEvents_asCreator_omitTheAdminOnlyTransitions(String negotiationId, String state) {
    assertEquals(state, adapter.currentNegotiationState(negotiationId));
    assertEquals(
        NegotiationGraphV1.possibleEventsForCreator(state),
        adapter.possibleNegotiationEvents(negotiationId));
  }

  @ParameterizedTest(name = "an unrelated user at {1}")
  @MethodSource("seededStates")
  @DisplayName("a caller who is neither admin nor creator is offered nothing, in any State")
  @WithUserDetails("researcher")
  void possibleEvents_asUnrelatedUser_areEmpty(String negotiationId, String state) {
    assertEquals(state, adapter.currentNegotiationState(negotiationId));
    assertEquals(Set.of(), adapter.possibleNegotiationEvents(negotiationId));
  }

  @Test
  @DisplayName("representing a Resource of the Negotiation confers no authority over it")
  @WithUserDetails("TheBiobanker")
  void possibleEvents_asRepresentativeOfAResource_areEmpty() {
    assertEquals("IN_PROGRESS", adapter.currentNegotiationState(IN_PROGRESS_NEGOTIATION));
    assertEquals(Set.of(), adapter.possibleNegotiationEvents(IN_PROGRESS_NEGOTIATION));
  }

  @Test
  @DisplayName("the two admin-secured Transitions are offered to an admin")
  @WithMockNegotiatorUser(id = 101L, authorities = "ROLE_ADMIN")
  void adminSecuredTransitions_areOfferedToAnAdmin() {
    assertEquals(
        NegotiationGraphV1.ADMIN_ONLY_EVENTS,
        adapter.possibleNegotiationEvents(SUBMITTED_NEGOTIATION));
  }

  @Test
  @DisplayName("the two admin-secured Transitions are withheld from the non-admin creator")
  @WithUserDetails("TheResearcher")
  void adminSecuredTransitions_areWithheldFromTheCreator() {
    assertEquals(Set.of(), adapter.possibleNegotiationEvents(SUBMITTED_NEGOTIATION));
  }

  /** The six Events a {@code SUBMITTED} Negotiation offers nobody, admin included. */
  static Stream<Arguments> eventsNotOfferedFromSubmitted() {
    return NegotiationGraphV1.eventsNotOfferedFrom("SUBMITTED").stream()
        .sorted()
        .map(event -> arguments(event, NegotiationGraphV1.refusalMessage(event)));
  }

  @ParameterizedTest(name = "{0} is refused with \"{1}\"")
  @MethodSource("eventsNotOfferedFromSubmitted")
  @DisplayName("firing an Event the current State does not offer is refused, even for an admin")
  @WithMockNegotiatorUser(id = 101L, authorities = "ROLE_ADMIN")
  void sendUnofferedEvent_asAdmin_isForbidden(String event, String expectedMessage) {
    ForbiddenRequestException refusal =
        assertThrows(
            ForbiddenRequestException.class,
            () -> adapter.sendNegotiationEvent(SUBMITTED_NEGOTIATION, event));

    assertEquals(expectedMessage, refusal.getMessage());
    assertEquals("SUBMITTED", adapter.currentNegotiationState(SUBMITTED_NEGOTIATION));
  }

  @ParameterizedTest(name = "{0} is refused with \"{1}\"")
  @MethodSource("adminOnlyEventsWithTheirRefusals")
  @DisplayName("firing an admin-only Event as the creator is refused in the same words")
  @WithUserDetails("TheResearcher")
  void sendAdminOnlyEvent_asCreator_isForbidden(String event, String expectedMessage) {
    ForbiddenRequestException refusal =
        assertThrows(
            ForbiddenRequestException.class,
            () -> adapter.sendNegotiationEvent(SUBMITTED_NEGOTIATION, event));

    assertEquals(expectedMessage, refusal.getMessage());
    assertEquals("SUBMITTED", adapter.currentNegotiationState(SUBMITTED_NEGOTIATION));
  }

  static Stream<Arguments> adminOnlyEventsWithTheirRefusals() {
    return NegotiationGraphV1.ADMIN_ONLY_EVENTS.stream()
        .sorted()
        .map(event -> arguments(event, NegotiationGraphV1.refusalMessage(event)));
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"APPROVE", "DECLINE", "PAUSE", "ABANDON"})
  @DisplayName("a caller offered nothing is refused whichever Event they try")
  @WithUserDetails("researcher")
  void sendAnyEvent_asUnrelatedUser_isForbidden(String event) {
    ForbiddenRequestException refusal =
        assertThrows(
            ForbiddenRequestException.class,
            () -> adapter.sendNegotiationEvent(SUBMITTED_NEGOTIATION, event));

    assertEquals(NegotiationGraphV1.refusalMessage(event), refusal.getMessage());
  }

  @Test
  @DisplayName("the message carries a reason too, and it is the Event's label lowercased")
  @WithMockNegotiatorUser(id = 101L, authorities = "ROLE_ADMIN")
  void refusalMessage_withAMessageArgument_isTheSame() {
    ForbiddenRequestException refusal =
        assertThrows(
            ForbiddenRequestException.class,
            () ->
                adapter.sendNegotiationEvent(SUBMITTED_NEGOTIATION, "ABANDON", "no longer needed"));

    assertEquals("You are not allowed to abandon the Negotiation", refusal.getMessage());
  }

  @Test
  @DisplayName(
      "asking for the Possible Events of a Negotiation that does not exist fails as an admin")
  @WithMockNegotiatorUser(id = 101L, authorities = "ROLE_ADMIN")
  void possibleEvents_ofUnknownNegotiation_asAdmin_raisesEntityNotFound() {
    EntityNotFoundException notFound =
        assertThrows(
            EntityNotFoundException.class,
            () -> adapter.possibleNegotiationEvents(NO_SUCH_NEGOTIATION));

    assertEquals(
        "Resource with id %s not found".formatted(NO_SUCH_NEGOTIATION), notFound.getMessage());
  }

  /**
   * The blanket authority check runs before the Negotiation is ever looked up, so a caller who is
   * nobody's admin and nobody's creator cannot tell a missing Negotiation from one they may not
   * touch. Pinned deliberately: it is the reason the criterion above has to name the admin.
   */
  @Test
  @DisplayName("the same question from an unrelated caller returns nothing instead of failing")
  @WithUserDetails("researcher")
  void possibleEvents_ofUnknownNegotiation_asUnrelatedUser_areEmpty() {
    assertEquals(Set.of(), adapter.possibleNegotiationEvents(NO_SUCH_NEGOTIATION));
  }

  @Test
  @DisplayName("firing an Event at a Negotiation that does not exist fails as an admin")
  @WithMockNegotiatorUser(id = 101L, authorities = "ROLE_ADMIN")
  void sendEvent_toUnknownNegotiation_raisesEntityNotFound() {
    assertThrows(
        EntityNotFoundException.class,
        () -> adapter.sendNegotiationEvent(NO_SUCH_NEGOTIATION, "APPROVE"));
  }

  @Test
  @DisplayName("reading the State of a Negotiation that does not exist fails")
  @WithMockNegotiatorUser(id = 101L, authorities = "ROLE_ADMIN")
  void currentState_ofUnknownNegotiation_raisesEntityNotFound() {
    assertThrows(
        EntityNotFoundException.class, () -> adapter.currentNegotiationState(NO_SUCH_NEGOTIATION));
  }

  /**
   * With no caller at all the service never reaches its own refusal: resolving the caller's
   * internal id fails first, and it fails with Spring Security's own exception rather than the
   * {@code ForbiddenRequestException} the service's {@code ClassCastException} branch was written
   * to produce. That branch is unreachable today, and this pins what happens instead.
   */
  @Test
  @DisplayName("with no authenticated caller the credentials failure surfaces, not a refusal")
  void possibleEvents_withoutAnAuthenticatedCaller_raisesCredentialsNotFound() {
    assertThrows(
        AuthenticationCredentialsNotFoundException.class,
        () -> adapter.possibleNegotiationEvents(SUBMITTED_NEGOTIATION));
  }
}
