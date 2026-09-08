package eu.bbmri_eric.negotiator.lifecycle.graph;

/**
 * One Action on a Compiled Graph, with its params already bound.
 *
 * <p>Actions run only after a commit and Guards only before one, so each has its own chain. Action
 * Wiring is per Transition only, and the {@code action_wiring} table has no definition column.
 */
public interface ActionStep {

  /** The catalogue key this step was bound from. For messages, never for dispatch. */
  String typeKey();

  /** Called by the service that committed the move, never by the evaluator. */
  void run(ActionContext context);
}
