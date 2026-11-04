package eu.bbmri_eric.negotiator.characterization.adapter;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.util.WithMockNegotiatorUser;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Checks that the adapter drives a real Lifecycle end to end with States and Events as strings.
 *
 * <p>The seeded {@code negotiation-2} is {@code SUBMITTED}, created by person 108, and has no
 * Resources, so approving it fires a Transition without Spawn and its notifications.
 */
@IntegrationTest(loadTestData = true)
@Import(LifecycleTestAdapterConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LifecycleTestAdapterSmokeTest {

  private static final String NEGOTIATION_UNDER_REVIEW = "negotiation-2";

  /**
   * {@code sendEvent} persists through {@code handleEventWithStateReactively(...).subscribe()}, so
   * the test reads the resulting State back under a bounded wait.
   */
  private static final Duration PERSIST_TIMEOUT = Duration.ofSeconds(5);

  @Autowired LifecycleTestAdapter adapter;

  @Test
  @DisplayName("an admin approving a submitted Negotiation moves it to IN_PROGRESS")
  @WithMockNegotiatorUser(id = 109L, authorities = "ROLE_ADMIN")
  void approveSubmittedNegotiation_movesToInProgress() {
    assertEquals("SUBMITTED", adapter.currentNegotiationState(NEGOTIATION_UNDER_REVIEW));
    assertTrue(adapter.possibleNegotiationEvents(NEGOTIATION_UNDER_REVIEW).contains("APPROVE"));

    adapter.sendNegotiationEvent(NEGOTIATION_UNDER_REVIEW, "APPROVE");

    await()
        .atMost(PERSIST_TIMEOUT)
        .untilAsserted(
            () ->
                assertEquals(
                    "IN_PROGRESS", adapter.currentNegotiationState(NEGOTIATION_UNDER_REVIEW)));
  }
}
