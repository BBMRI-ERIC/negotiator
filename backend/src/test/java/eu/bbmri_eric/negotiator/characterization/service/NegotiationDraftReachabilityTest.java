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
 * Settles the {@code DRAFT} question, which is two questions that must not be conflated.
 *
 * <p><b>Occupied.</b> A Negotiation really can be sitting in {@code DRAFT}: {@code negotiation-6}
 * is seeded there. Anything that has to reproduce today's data - a migration seed, a fixture, a
 * rehearsal dataset - must be able to put a Negotiation in that State.
 *
 * <p><b>Enterable.</b> It is nevertheless not a State the graph can move a Negotiation into. The
 * initial State is {@code SUBMITTED}, {@code DRAFT --SUBMIT--> SUBMITTED} leads out of it, and no
 * Transition anywhere targets it. Within the graph {@code DRAFT} is a source with no way in, so a
 * Negotiation that leaves it can never return.
 *
 * <p>Both facts are true at once, and a design that treated "occupied" as evidence of "enterable"
 * would model the graph wrongly. Whatever a Negotiation is in {@code DRAFT} for, it is put there by
 * something outside this Lifecycle - not reached through it.
 *
 * <p>The third question is who may act on it, and the answer is the ordinary one: {@code SUBMIT}
 * carries no Required Authority, so it is offered to an admin and to the Negotiation's own creator,
 * and withheld from everyone else by the blanket check that precedes it.
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
   * The other side of the same coin: which States the seeded corpus actually occupies, read
   * straight out of the table the Lifecycle persists into rather than inferred from the four
   * Negotiations this suite happens to name. It settles both halves of the finding at once - {@code
   * DRAFT} is occupied, {@code APPROVED} is not.
   *
   * <p>That {@code APPROVED} is also declared by the Definition and named by no Transition is the
   * same finding from the Definition's side, and it is checked against the mechanical dump by
   * {@link NegotiationGraphV1BindingTest#legacyState_isDeclaredButUnusedInTheDump()} rather than
   * restated over the pinned table here.
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
   * An assertion over the pinned graph rather than over a call, and legitimate because that table
   * is bound to the mechanical dump by {@link NegotiationGraphV1BindingTest} - which makes the same
   * statement directly against the dump's own transitions array - and because every row of it is
   * fired against the real Lifecycle by {@link NegotiationTransitionParityTest}, with every State's
   * offering checked against the same rows there and in {@link NegotiationAuthorityParityTest}. So
   * "no row targets DRAFT" is a statement about the system and not merely about this file.
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
