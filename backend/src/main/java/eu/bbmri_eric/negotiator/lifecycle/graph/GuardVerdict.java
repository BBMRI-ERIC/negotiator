package eu.bbmri_eric.negotiator.lifecycle.graph;

import java.util.Map;
import java.util.Objects;

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
public record GuardVerdict(boolean passed, String reasonCode, Map<String, Object> details) {

  private static final GuardVerdict PASSED = new GuardVerdict(true, "", Map.of());

  public GuardVerdict {
    Objects.requireNonNull(reasonCode, "reasonCode");
    details = Map.copyOf(details);
  }

  /** Named {@code pass} rather than {@code passed} because a record may not shadow its accessor. */
  public static GuardVerdict pass() {
    return PASSED;
  }

  public static GuardVerdict fail(String reasonCode) {
    return fail(reasonCode, Map.of());
  }

  public static GuardVerdict fail(String reasonCode, Map<String, Object> details) {
    if (reasonCode == null || reasonCode.isBlank()) {
      throw new IllegalArgumentException(
          "A failed verdict needs a reason code: it is the only thing a caller can branch on.");
    }
    return new GuardVerdict(false, reasonCode, details);
  }
}
