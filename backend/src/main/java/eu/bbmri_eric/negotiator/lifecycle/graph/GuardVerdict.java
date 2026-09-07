package eu.bbmri_eric.negotiator.lifecycle.graph;

import java.util.Map;
import lombok.NonNull;

/**
 * What one step of the Evaluation Pipeline answers: it passed, or it did not and here is why.
 *
 * <p>Note what a verdict deliberately cannot say: which {@link FailureCategory} its failure belongs
 * to. The category is assigned by the stage that ran the step, not claimed by the step — which is
 * what keeps ADR 0005's categories monotonic by construction rather than by review. A Guard cannot
 * report an authorization failure however it is written, and the Information Requirement stage
 * reports an unmet requirement even though it speaks this same contract.
 *
 * @param reasonCode a stable machine-readable key, for a caller that has to branch or translate.
 *     Empty on a pass.
 * @param details whatever the caller needs to act on the refusal — ADR 0005 asks the requirement
 *     stage for "the missing forms" here. Keys are the step's own business.
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
   * @throws IllegalArgumentException if the reason code is missing
   */
  public static GuardVerdict fail(String reasonCode, Map<String, Object> details) {
    // Deliberately NOT an InvalidGraphException, and kept as an IllegalArgumentException on
    // purpose. Every other rejection in these three packages was folded into that one type; this
    // one was looked at and left. A Guard handing back a refusal with no reason code is a
    // programming error in a strategy — the graph it was asked about may be perfectly well formed,
    // and the argument really is the caller's. Do not collect it in the next sweep.
    if (reasonCode == null || reasonCode.isBlank()) {
      throw new IllegalArgumentException(
          "A failed verdict needs a reason code: it is the only thing a caller can branch on.");
    }
    return new GuardVerdict(false, reasonCode, details);
  }
}
