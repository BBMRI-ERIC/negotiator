package eu.bbmri_eric.negotiator.characterization.service;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * The one bounded wait every class in this package reads a post-send State through.
 *
 * <p>Sending an Event drives an asynchronous persist path - {@code
 * handleEventWithStateReactively(...).subscribe()} - so the call returns before the new State is
 * written. Every read-back is therefore polled under a bounded timeout, never slept on and never
 * asserted immediately after the send. The timeout lives here rather than being redeclared per
 * class, so that a slow container is tuned for in one place.
 */
final class LifecyclePersistence {

  /** Bounded, and generous enough that a slow container is not mistaken for a lost Transition. */
  static final Duration PERSIST_TIMEOUT = Duration.ofSeconds(15);

  /** Waits for {@code currentState} to read back as {@code expected}. */
  static void awaitState(String expected, Supplier<String> currentState) {
    await().atMost(PERSIST_TIMEOUT).untilAsserted(() -> assertEquals(expected, currentState.get()));
  }

  /**
   * Waits out {@code settle} first and only then asserts, which is what makes "the Event was a
   * no-op" mean something: without the delay, a refused Event and a Transition that simply had not
   * landed yet would look the same.
   */
  static void awaitStateAfterSettling(
      Duration settle, String expected, Supplier<String> currentState) {
    await()
        .pollDelay(settle)
        .atMost(PERSIST_TIMEOUT)
        .untilAsserted(() -> assertEquals(expected, currentState.get()));
  }

  private LifecyclePersistence() {}
}
