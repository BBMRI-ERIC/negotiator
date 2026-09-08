package eu.bbmri_eric.negotiator.lifecycle.graph;

import java.util.Map;
import lombok.NonNull;

/**
 * What one step of the Evaluation Pipeline answers: it passed, or it failed and why.
 *
 * <p>A verdict carries no {@link FailureCategory}. The stage that ran the step assigns it, so a
 * Guard cannot report an authorization failure however it is written.
 *
 * @param reasonCode a stable machine-readable key a caller can branch on or translate. Empty on a
 *     pass.
 * @param details what the caller needs to act on the refusal, such as the missing forms of an
 *     Information Requirement. Each step chooses its own keys.
 */
public record GuardVerdict(
    boolean passed, @NonNull String reasonCode, Map<String, Object> details) {

  private static final GuardVerdict PASSED = new GuardVerdict(true, "", Map.of());

  public GuardVerdict {
    details = Map.copyOf(details);
  }

  /** Named {@code pass} rather than {@code passed} because a record may not shadow its accessor. */
  public static GuardVerdict pass() {
    return PASSED;
  }

  public static GuardVerdict fail(String reasonCode) {
    return fail(reasonCode, Map.of());
  }

  /**
   * Builds a refusal, which must carry a reason code.
   *
   * @throws IllegalArgumentException if the reason code is missing
   */
  public static GuardVerdict fail(String reasonCode, Map<String, Object> details) {
    if (reasonCode == null || reasonCode.isBlank()) {
      throw new IllegalArgumentException(
          "A failed verdict needs a reason code: it is the only thing a caller can branch on.");
    }
    return new GuardVerdict(false, reasonCode, details);
  }
}
