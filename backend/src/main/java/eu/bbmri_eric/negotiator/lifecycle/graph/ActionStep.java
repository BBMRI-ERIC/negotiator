package eu.bbmri_eric.negotiator.lifecycle.graph;

/**
 * One Action on a compiled graph, with its configuration already applied — the Action-side twin of
 * {@link GuardStep}, and bound the same way at the same moment.
 *
 * <p>Actions and Guards never interleave: Guards run before a commit and Actions only after one, so
 * they share no ordering and each has its own chain. That is also why an Action's chain is
 * transition-scoped only. There is no definition-wide Action wiring anywhere, and the {@code
 * action_wiring} table has no definition column to express one with.
 */
public interface ActionStep {

  /** The catalogue key this step was bound from. For messages, never for dispatch. */
  String typeKey();

  /** Runs the effect. Called by the service that committed the move, never by the evaluator. */
  void run(ActionContext context);
}
