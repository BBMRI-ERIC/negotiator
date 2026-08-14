package eu.bbmri_eric.negotiator.characterization.service;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * The one bounded wait every class in this package reads a post-send State through - and, since a
 * Transition writes more than a State, every other observable one leaves behind.
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
    awaitValue(expected, currentState);
  }

  /**
   * Waits for any observable of a Transition to read back as {@code expected} - a State, a post
   * flag, a row count. The same bound as {@link #awaitState}, because they are all written by the
   * same asynchronous path.
   */
  static <T> void awaitValue(T expected, Supplier<T> observed) {
    awaitValue(expected, observed, null);
  }

  /** {@link #awaitValue(Object, Supplier)}, saying what the observable is when it never arrives. */
  static <T> void awaitValue(T expected, Supplier<T> observed, String describedAs) {
    await()
        .atMost(PERSIST_TIMEOUT)
        .untilAsserted(() -> assertEquals(expected, observed.get(), describedAs));
  }

  /**
   * Waits out {@code settle} first and only then asserts, which is what makes "the Event was a
   * no-op" mean something: without the delay, a refused Event and a Transition that simply had not
   * landed yet would look the same.
   */
  static void awaitStateAfterSettling(
      Duration settle, String expected, Supplier<String> currentState) {
    awaitValueAfterSettling(settle, expected, currentState);
  }

  /** {@link #awaitStateAfterSettling} for any observable, for the same reason. */
  static <T> void awaitValueAfterSettling(Duration settle, T expected, Supplier<T> observed) {
    awaitValueAfterSettling(settle, expected, observed, null);
  }

  /** {@link #awaitValueAfterSettling(Duration, Object, Supplier)}, with a description. */
  static <T> void awaitValueAfterSettling(
      Duration settle, T expected, Supplier<T> observed, String describedAs) {
    await()
        .pollDelay(settle)
        .atMost(PERSIST_TIMEOUT)
        .untilAsserted(() -> assertEquals(expected, observed.get(), describedAs));
  }

  private LifecyclePersistence() {}
}
