package eu.bbmri_eric.negotiator.lifecycle.evaluation;

import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardVerdict;

/**
 * Whether the Information Requirements attached to an Event have been satisfied — the one question
 * gating a move that cannot be answered from the evaluation context, because it depends on
 * submissions.
 *
 * <p>This is a <b>port</b>, and it is the evaluator's only one. Everything else the pipeline needs
 * is handed in whole; this reads data, so it is injected and the evaluator holds nothing but the
 * interface. The evaluator therefore still names no repository and can still be tested with no
 * database, which is ADR 0001's constraint kept intact rather than bent.
 *
 * <p>It answers in {@link GuardVerdict}, the same vocabulary a Guard answers in, because ADR 0005
 * says the stage "still speaks the Guard contract, emitting a normal result with a reason code and
 * the missing forms in its details, so callers see one uniform list of failures". It is nonetheless
 * <em>not</em> a registry Guard: there is no Wiring row for it anywhere, so no admin can omit it
 * and no newly added Transition can miss it. A wireable Guard would have to be attached to each
 * Transition by hand, and one forgotten row silently reintroduces the dead-click bug the design
 * exists to remove.
 *
 * <p>What sits behind it is a later slab's. Worth knowing before writing that: today's check is far
 * weaker than its name suggests. It asks whether <em>any</em> Requirement exists for the Event name
 * anywhere in the deployment, and then whether <em>any</em> submission exists for that
 * resource-and-negotiation pair — not for that Requirement, and not by anyone in particular, so a
 * form filled in against a different Requirement satisfies it. ADR 0005's reason codes and ADR
 * 0006's Audience and Quantifier are all strengthenings of that, each a behaviour change with a
 * migration story rather than a typo to fix in passing.
 */
public interface InformationRequirementSatisfaction {

  /**
   * @param event the name of the Event being fired — Requirements attach to the Event, so they
   *     apply wherever it fires, whatever State it fires from
   * @param context which Lifecycle is moving and who is firing
   * @return a passing verdict when nothing is outstanding; otherwise a reason code and the missing
   *     forms in the details
   */
  GuardVerdict check(String event, EvaluationContext context);
}
