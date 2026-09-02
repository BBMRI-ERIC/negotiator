package eu.bbmri_eric.negotiator.lifecycle.evaluation;

import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardVerdict;
import org.springframework.stereotype.Component;

/**
 * The placeholder behind {@link InformationRequirementSatisfaction} until the slab that reads
 * submissions builds the real one. It refuses to answer.
 *
 * <p>Refusing rather than returning "everything is satisfied" is the whole point. This slab wires
 * nothing into production, so nothing calls it today — but the moment the cutover wires the
 * evaluator into a lifecycle service, a permissive placeholder would silently drop the Information
 * Requirement gate and every Event guarded by a form would start firing. Throwing makes that a
 * failed request on the first attempt instead of a missing check nobody notices.
 *
 * <p>It exists at all so that the port has a bean and the wiring is real rather than hypothetical.
 * Deleting this class is how the IR slab announces itself.
 */
@Component
class UnbuiltInformationRequirementSatisfaction implements InformationRequirementSatisfaction {

  @Override
  public GuardVerdict check(String event, EvaluationContext context) {
    throw new UnsupportedOperationException(
        ("Information Requirement satisfaction is not built yet, so no move may be gated on it. "
                + "Asked about Event '%s' on Negotiation %s. Replace this bean with the real "
                + "lookup before wiring the Transition Evaluator into a lifecycle service.")
            .formatted(event, context.subject().negotiationId()));
  }
}
