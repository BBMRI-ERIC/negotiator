package eu.bbmri_eric.negotiator.lifecycle.evaluation;

import eu.bbmri_eric.negotiator.lifecycle.graph.ActionStep;
import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledTransition;
import eu.bbmri_eric.negotiator.lifecycle.graph.FailureCategory;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * What the Transition Evaluator answers about one Event. Either the move is permitted and this is
 * the edge it would take, or it is refused and this is the category and reason.
 *
 * <p>A permitted outcome is a <em>judgement</em>, not a commit. Nothing here has moved, written or
 * notified: ADR 0001 puts committing the move, running its Actions and writing history in the
 * services around the evaluator, and this record is the whole of what crosses that line.
 */
public sealed interface EvaluationOutcome {

  boolean permitted();

  /**
   * The move is legal now. {@code transition} names where it goes, and carries the ordered Action
   * chain the committing service is to run afterwards.
   *
   * <p>Reported, not run. The evaluator hands back what <em>would</em> happen and stops there,
   * which is what makes the Possible Events listing a safe dry run of the same function: evaluating
   * twenty candidate Events must not set twenty posts' visibility.
   */
  record Permitted(CompiledTransition transition) implements EvaluationOutcome {

    public Permitted {
      Objects.requireNonNull(transition, "transition");
    }

    /** The Actions to run once the move has committed, in order. */
    public List<ActionStep> actions() {
      return transition.actions();
    }

    @Override
    public boolean permitted() {
      return true;
    }
  }

  /**
   * The move is not legal now, and the pipeline stopped at the first stage that said so.
   *
   * @param category which stage refused, in ADR 0005's fixed order
   * @param reasonCode a stable key naming the specific refusal within that category
   * @param details whatever a caller needs to act on it, contributed by the refusing step
   */
  record Refused(FailureCategory category, String reasonCode, Map<String, Object> details)
      implements EvaluationOutcome {

    public Refused {
      Objects.requireNonNull(category, "category");
      Objects.requireNonNull(reasonCode, "reasonCode");
      details = Map.copyOf(details);
    }

    @Override
    public boolean permitted() {
      return false;
    }
  }
}
