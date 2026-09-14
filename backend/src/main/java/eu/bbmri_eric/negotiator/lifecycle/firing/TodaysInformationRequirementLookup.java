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
 * The Information Requirement Built-in Stage, reproducing <b>today's</b> check rather than the one
 * ADR 0006 describes.
 *
 * <p>It lives here and not in the evaluation package because it reads two tables, and {@code
 * EvaluatorPurityGuardTest} bans any name matching {@code \b\w*Repository\b} from {@code graph} and
 * {@code evaluation} — as a suffix rule, deliberately, so repositories that did not exist when it
 * was written are banned too. That guard is the one structural gate not scheduled for deletion, so
 * the lookup moves to the data rather than the rule moving to the lookup. It replaces {@code
 * UnbuiltInformationRequirementSatisfaction}, which threw on every path including {@code
 * possibleEvents}; deleting that class is how this slab announced itself.
 *
 * <p><b>The check is far weaker than its name suggests, and that is on purpose.</b> It asks whether
 * <em>any</em> Requirement exists for the Event name anywhere in the deployment, and then whether
 * <em>any</em> submission exists for that resource-and-negotiation pair — not for that Requirement,
 * and not by anyone in particular. So a form filled in against a different Requirement satisfies
 * it, and a form filled in by someone with no business answering satisfies it too. The
 * characterization suite pins every one of those, {@code
 * ResourceInformationRequirementGateTest#submissionAgainstADifferentRequirement_satisfiesTheGate}
 * most pointedly.
 *
 * <p>Writing the stronger check here would have been a behaviour change smuggled in as an
 * implementation: ADR 0006's Audience, Audience Resolver, Qualifying Submission and Quantifier are
 * each a strengthening of this with a migration story of its own, and slab 09's user story 40
 * already filed the real lookup as later work. Reproduce it, let the characterization suite hold
 * it, and change it where the change can be argued.
 *
 * <p><b>A Negotiation-scope context is refused rather than passed.</b> There is no
 * Negotiation-scope behaviour to reproduce — only the Resource path has ever had a requirement
 * check — so passing would not be reproducing today, it would be deciding that Negotiations are
 * ungated for ever, in a branch, in the slab that refused Negotiation scope by name everywhere
 * else. Refusing leaves the decision where {@link UnsupportedScopeException} says it belongs.
 * Unreachable from {@link EventFiring} today, which refuses Negotiation scope earlier; reachable at
 * all only because the port is a bean and the evaluator can be handed any context.
 *
 * <p><b>Where this stage sits differs from today even though the check does not.</b> Today's check
 * runs before everything, so an unmet Requirement outranks even an Event the caller has no
 * authority for; ADR 0005 puts it second, after Required Authority. That reordering is one of the
 * intended deltas, and it belongs to whichever slab wires this path into a controller — not here,
 * where nothing calls it. The old path is untouched and keeps its order.
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
