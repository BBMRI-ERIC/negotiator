package eu.bbmri_eric.negotiator.lifecycle.evaluation;

import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardVerdict;

/**
 * Whether the Information Requirements attached to an Event are satisfied. The evaluation context
 * cannot answer this because it depends on submissions.
 *
 * <p>This is the evaluator's only port. It reads data, so it is injected, and the evaluator still
 * names no repository and runs in tests without a database.
 *
 * <p>It answers in {@link GuardVerdict} so callers see one uniform list of failures. It is not a
 * registry Guard, though. No Wiring row names it, so no admin can omit it and no new Transition can
 * miss it.
 *
 * <p>The current check is weaker than its name. It passes when any Requirement exists for the
 * Event name and any submission exists for the resource and negotiation, even one made against a
 * different Requirement. Tightening it to a specific Audience and Quantifier changes behaviour and
 * needs a migration.
 */
public interface InformationRequirementSatisfaction {

  /**
   * @param event the name of the Event being fired. Requirements attach to the Event, so they apply
   *     from every State it fires from
   * @param context which Lifecycle is moving and who is firing
   * @return a passing verdict when nothing is outstanding; otherwise a reason code and the missing
   *     forms in the details
   */
  GuardVerdict check(String event, EvaluationContext context);
}
