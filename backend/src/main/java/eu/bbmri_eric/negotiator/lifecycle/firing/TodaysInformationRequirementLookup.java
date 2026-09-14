package eu.bbmri_eric.negotiator.lifecycle.firing;

import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementRepository;
import eu.bbmri_eric.negotiator.info_submission.InformationSubmissionRepository;
import eu.bbmri_eric.negotiator.lifecycle.evaluation.InformationRequirementSatisfaction;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardVerdict;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The Information Requirement Built-in Stage, reproducing the check the old Resource path makes.
 *
 * <p>It lives in the firing package because it reads two repositories, and {@code
 * EvaluatorPurityGuardTest} bans repositories from {@code graph} and {@code evaluation}.
 *
 * <p>The check is weak on purpose. It asks whether any Requirement exists for the Event name, then
 * whether any submission exists for the Resource and Negotiation pair, not for that Requirement and
 * not by any particular person. A stronger check would change today's behaviour.
 *
 * <p>A Negotiation-scope context is refused, not passed. Only the Resource path has ever had a
 * requirement check, and passing would decide that Negotiations are never gated.
 *
 * <p>The old path runs this check before Required Authority. Here it runs second, after Required
 * Authority, as the Evaluation Pipeline orders its stages.
 */
@Component
@RequiredArgsConstructor
class TodaysInformationRequirementLookup implements InformationRequirementSatisfaction {

  /** Stable key for the one refusal this stage can produce. */
  static final String NOT_SATISFIED = "INFORMATION_REQUIREMENT_NOT_SATISFIED";

  private final InformationRequirementRepository requirements;
  private final InformationSubmissionRepository submissions;

  @Override
  public GuardVerdict check(String event, EvaluationContext context) {
    if (!requirements.existsByForEvent(event)) {
      return GuardVerdict.pass();
    }
    String resourceId = context.subject().resourceId();
    if (resourceId == null) {
      throw new UnsupportedScopeException(
          ("The Information Requirement stage has no Negotiation-scope behaviour to reproduce."
                  + " Today's check reads submissions by Resource and Negotiation together and a"
                  + " Negotiation's own Lifecycle has no Resource to key on; the Negotiation"
                  + " lifecycle service has no requirement check at all. Asked about Event '%s' on"
                  + " Negotiation %s.")
              .formatted(event, context.subject().negotiationId()));
    }
    if (submissions.existsByResource_SourceIdAndNegotiation_Id(
        resourceId, context.subject().negotiationId())) {
      return GuardVerdict.pass();
    }
    return GuardVerdict.fail(
        NOT_SATISFIED,
        Map.of(
            "event", event,
            "negotiationId", context.subject().negotiationId(),
            "resourceId", resourceId));
  }
}
