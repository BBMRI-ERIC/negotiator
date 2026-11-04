package eu.bbmri_eric.negotiator.characterization.service;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * The bounded wait this package uses to read back a State, or anything else a Transition writes,
 * after a send.
 *
 * <p>Sending an Event persists through {@code handleEventWithStateReactively(...).subscribe()}, so
 * the call returns before the new State is written. Every read-back polls under a bounded timeout
 * instead of sleeping or asserting straight away. The timeout lives in this one place so a slow
 * container needs one change.
 */
final class LifecyclePersistence {

  /** Long enough that a slow container does not look like a lost Transition. */
  static final Duration PERSIST_TIMEOUT = Duration.ofSeconds(15);

  /** Waits for {@code currentState} to read back as {@code expected}. */
  static void awaitState(String expected, Supplier<String> currentState) {
    awaitValue(expected, currentState);
  }

  /**
   * Waits for any value a Transition writes, such as a State, a post flag or a row count, to read back
   * as {@code expected}. It uses the same bound as {@link #awaitState} because the same asynchronous
   * path writes them all.
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
   * Waits out {@code settle} before asserting. Without the delay, a refused Event and a Transition
   * that has not landed yet look the same.
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
