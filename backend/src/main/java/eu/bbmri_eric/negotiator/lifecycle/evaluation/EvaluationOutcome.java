package eu.bbmri_eric.negotiator.lifecycle.evaluation;

import eu.bbmri_eric.negotiator.lifecycle.graph.ActionStep;
import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledTransition;
import eu.bbmri_eric.negotiator.lifecycle.graph.FailureCategory;
import java.util.List;
import java.util.Map;
import lombok.NonNull;

/**
 * What the Transition Evaluator answers about one Event. Either the move is permitted and this is
 * the edge it would take, or it is refused and this is the category and reason.
 *
 * <p>A permitted outcome commits nothing. The services around the evaluator commit the move, run
 * its Actions and write the Lifecycle Record.
 */
public sealed interface EvaluationOutcome {

  boolean permitted();

  /**
   * The move is legal now. {@code transition} names the target State and carries the ordered
   * Actions the committing service runs afterwards.
   *
   * <p>The evaluator reports the Actions and does not run them, so listing Possible Events can call
   * the same function without running anything.
   */
  record Permitted(@NonNull CompiledTransition transition) implements EvaluationOutcome {

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
   * @param category which stage refused
   * @param reasonCode a stable key naming the specific refusal within that category
   * @param details what a caller needs to act on the refusal, supplied by the refusing step
   */
  record Refused(
      @NonNull FailureCategory category, @NonNull String reasonCode, Map<String, Object> details)
      implements EvaluationOutcome {

    public Refused {
      details = Map.copyOf(details);
    }

    @Override
    public boolean permitted() {
      return false;
    }
  }
}
