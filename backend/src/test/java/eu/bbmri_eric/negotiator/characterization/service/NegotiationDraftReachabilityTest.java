package eu.bbmri_eric.negotiator.characterization.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapter;
import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapterConfig;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.util.WithMockNegotiatorUser;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Pins two separate facts about {@code DRAFT}.
 *
 * <p>A Negotiation can be in {@code DRAFT}: {@code negotiation-6} is seeded there. Anything that
 * reproduces existing data, such as a migration seed or a fixture, must be able to put a
 * Negotiation in that State.
 *
 * <p>The graph cannot move a Negotiation into it. The initial State is {@code SUBMITTED}, {@code
 * DRAFT --SUBMIT--> SUBMITTED} leads out, and no Transition targets {@code DRAFT}, so a Negotiation
 * that leaves it never returns. Something outside the Lifecycle puts a Negotiation there.
 *
 * <p>{@code SUBMIT} has no Required Authority, so it is offered to an admin and the creator, and
 * the first authority check withholds it from everyone else.
 */
@IntegrationTest(loadTestData = true)
@Import(LifecycleTestAdapterConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class NegotiationDraftReachabilityTest {

  /** The one Negotiation the seeded corpus leaves in {@code DRAFT}. Its creator is person 108. */
  private static final String SEEDED_IN_DRAFT = "negotiation-6";

  @Autowired LifecycleTestAdapter adapter;

  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("DRAFT is observably occupied: a seeded Negotiation is in it")
  @WithMockNegotiatorUser(id = 101L, authorities = "ROLE_ADMIN")
  void draft_isObservablyOccupied() {
    assertEquals("DRAFT", adapter.currentNegotiationState(SEEDED_IN_DRAFT));
  }

  /**
   * Which States the seed occupies, read from the table the Lifecycle writes rather than from the
   * Negotiations this suite names: {@code DRAFT} is occupied, {@code APPROVED} is not.
   *
   * <p>{@link NegotiationGraphV1BindingTest#legacyState_isDeclaredButUnusedInTheDump()} checks
   * against the dump that {@code APPROVED} is declared and named by no Transition.
   */
  @Test
  @DisplayName("the seed occupies four States: DRAFT among them, the Legacy State not")
  void seededCorpus_occupiesDraftAndNotTheLegacyState() {
    Set<String> occupied =
        Set.copyOf(
            jdbcTemplate.queryForList(
                "select distinct current_state from negotiation", String.class));

    assertEquals(
        Set.of("DRAFT", "SUBMITTED", "IN_PROGRESS", "ABANDONED"),
        occupied,
        ("DRAFT is occupied, so something must be able to put a Negotiation there; the Legacy State"
                + " %s is not, and a Legacy State with a live occupant could not be modelled as one")
            .formatted(NegotiationGraphV1.LEGACY_STATE));
  }

  /**
   * Asserts over the table rather than a call. That holds for the system because {@link
   * NegotiationGraphV1BindingTest} binds the table to the dump and {@link
   * NegotiationTransitionParityTest} fires every row against the real Lifecycle.
   */
  @Test
  @DisplayName("DRAFT is not enterable: no Transition of the graph targets it")
  void draft_isTargetedByNoTransition() {
    assertFalse(
        NegotiationGraphV1.TRANSITIONS.stream().anyMatch(edge -> edge.target().equals("DRAFT")),
        "a Transition into DRAFT would make it reachable, and the seed could then be modelled as"
            + " an ordinary State rather than an entry point outside the graph");
    assertEquals(
        "SUBMITTED",
        NegotiationGraphV1.INITIAL_STATE,
        "the initial State is where a Lifecycle begins, and it is not DRAFT");
  }

  @Test
  @DisplayName("SUBMIT is offered from DRAFT to an admin")
  @WithMockNegotiatorUser(id = 101L, authorities = "ROLE_ADMIN")
  void draft_offersSubmitToAnAdmin() {
    assertEquals(Set.of("SUBMIT"), adapter.possibleNegotiationEvents(SEEDED_IN_DRAFT));
  }

  @Test
  @DisplayName("SUBMIT is offered from DRAFT to the creator and nothing else is")
  @WithUserDetails("TheResearcher")
  void draft_offersSubmitToTheCreator() {
    assertEquals(Set.of("SUBMIT"), adapter.possibleNegotiationEvents(SEEDED_IN_DRAFT));
  }

  @Test
  @DisplayName("DRAFT offers nothing to a caller who is neither admin nor creator")
  @WithUserDetails("researcher")
  void draft_offersNothingToAnUnrelatedCaller() {
    assertEquals(Set.of(), adapter.possibleNegotiationEvents(SEEDED_IN_DRAFT));
  }

  @Test
  @DisplayName("the creator can fire SUBMIT, and the Negotiation leaves DRAFT for good")
  @WithUserDetails("TheResearcher")
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
  void draft_submitFiresForTheCreatorAndLeavesDraftBehind() {
    assertEquals("DRAFT", adapter.currentNegotiationState(SEEDED_IN_DRAFT));

    adapter.sendNegotiationEvent(SEEDED_IN_DRAFT, "SUBMIT");

    LifecyclePersistence.awaitState(
        "SUBMITTED", () -> adapter.currentNegotiationState(SEEDED_IN_DRAFT));

    assertTrue(
        NegotiationGraphV1.possibleEventsForCreator("SUBMITTED").isEmpty(),
        "a creator has nothing left to fire once their Negotiation is SUBMITTED");
    assertEquals(Set.of(), adapter.possibleNegotiationEvents(SEEDED_IN_DRAFT));
  }
}
