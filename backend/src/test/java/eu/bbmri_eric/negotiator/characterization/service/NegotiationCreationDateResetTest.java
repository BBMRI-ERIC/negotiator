package eu.bbmri_eric.negotiator.characterization.service;

import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.NO_RESOURCES;
import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.SEEDED_IN_DRAFT;
import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.creationDate;
import static org.assertj.core.api.Assertions.assertThat;

import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapter;
import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapterConfig;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.util.WithMockNegotiatorUser;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Pins the one effect of a Negotiation Transition that is not about posts at all: arriving in
 * {@code SUBMITTED} resets the Negotiation's creation date to the moment it arrived.
 *
 * <p>It belongs with the post side effects because it is written on the same path, by the same code
 * that writes the new State, rather than by any Transition's Action. Nothing in the graph announces
 * it, which is exactly why it needs a test: a redesign that rebuilds the write path from the
 * Definition alone would drop it silently, and every Negotiation submitted afterwards would carry
 * the date it was drafted instead of the date it was submitted.
 *
 * <p><b>It is keyed on the State arrived in, not on the Event.</b> Only {@code DRAFT --SUBMIT-->
 * SUBMITTED} reaches {@code SUBMITTED} in this graph, so no test can separate the two today; the
 * contrast that can be drawn is that a Transition arriving anywhere else leaves the date alone, and
 * that is pinned below.
 *
 * <p>{@code negotiation-6} is the only Negotiation the seed places in {@code DRAFT}, so the
 * submitting half needs no hand placement at all - it is driven from where the seed left it.
 *
 * <p>{@code @DirtiesContext} per method restores the seed: both halves move a shared Negotiation.
 */
@IntegrationTest(loadTestData = true)
@Import(LifecycleTestAdapterConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class NegotiationCreationDateResetTest {

  /** Long enough for the persist path to have rewritten the date had it been going to. */
  private static final Duration SETTLE = Duration.ofSeconds(3);

  @Autowired LifecycleTestAdapter adapter;
  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("reaching SUBMITTED resets the Negotiation's creation date to the time it arrived")
  @WithMockNegotiatorUser(id = SeededNegotiationSubject.ADMIN, authorities = "ROLE_ADMIN")
  void reachingSubmitted_resetsTheCreationDate() {
    LocalDateTime seeded = creationDate(jdbcTemplate, SEEDED_IN_DRAFT);
    LocalDateTime beforeSend = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    assertThat(seeded)
        .as("the seeded date must be in the past, or nothing here could tell a reset from a no-op")
        .isBefore(beforeSend);

    adapter.sendNegotiationEvent(SEEDED_IN_DRAFT, "SUBMIT");

    LifecyclePersistence.awaitState(
        NegotiationGraphV1.target("DRAFT", "SUBMIT"),
        () -> adapter.currentNegotiationState(SEEDED_IN_DRAFT));
    LifecyclePersistence.awaitValue(
        Boolean.TRUE,
        () -> !seeded.equals(creationDate(jdbcTemplate, SEEDED_IN_DRAFT)),
        "the creation date the Negotiation was drafted with must not survive submission");

    assertThat(creationDate(jdbcTemplate, SEEDED_IN_DRAFT))
        .as("the date it is reset to is the moment the Transition was persisted")
        .isBetween(beforeSend, LocalDateTime.now().plusMinutes(1));
  }

  @Test
  @DisplayName("a Transition arriving anywhere else leaves the creation date alone")
  @WithMockNegotiatorUser(id = SeededNegotiationSubject.ADMIN, authorities = "ROLE_ADMIN")
  void aTransitionArrivingElsewhere_leavesTheCreationDateAlone() {
    LocalDateTime seeded = creationDate(jdbcTemplate, NO_RESOURCES);

    adapter.sendNegotiationEvent(NO_RESOURCES, "APPROVE");

    LifecyclePersistence.awaitState(
        NegotiationGraphV1.target("SUBMITTED", "APPROVE"),
        () -> adapter.currentNegotiationState(NO_RESOURCES));
    LifecyclePersistence.awaitValueAfterSettling(
        SETTLE,
        seeded,
        () -> creationDate(jdbcTemplate, NO_RESOURCES),
        "leaving SUBMITTED is not arriving in it, so the date stays as it was");
  }
}
