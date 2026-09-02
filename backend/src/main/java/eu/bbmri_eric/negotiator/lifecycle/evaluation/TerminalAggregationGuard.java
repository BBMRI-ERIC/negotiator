package eu.bbmri_eric.negotiator.lifecycle.evaluation;

import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardVerdict;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Passes when every Resource of the Negotiation has finished. Paired with a {@code SYSTEM} Event,
 * this is how a Negotiation concludes by itself.
 *
 * <p><b>Terminality is a question, not a list.</b> Two Resources of one Negotiation may run
 * different Definition Versions, so there is no set of State names this Guard could hold. It asks
 * each Resource's <em>own</em> pinned version whether the State that Resource is in carries the
 * terminal flag. That is ADR 0007's structural replacement for today's predicate, which compares
 * against two hardcoded names.
 *
 * <p><b>The mechanism only.</b> Which States carry the flag is seed content and belongs to the
 * migration slab. Behaviour-preserving means the v1 Resource seed flags exactly the two States
 * today's predicate counts; deriving terminality structurally invites four, because two more States
 * mean a Resource is finished in every practical sense — {@code RESOURCE_NOT_MADE_AVAILABLE}, where
 * a researcher's own refusal of the access conditions lands, and {@code
 * RESOURCE_UNAVAILABLE_WILLING_TO_COLLECT}. A Negotiation all of whose Resources end in either
 * stays in progress for ever today. That is pinned as behaviour, not endorsed, and widening it is a
 * behaviour change rather than a faithful reproduction.
 *
 * <p>No params, and ADR 0007 says why none are planned: there is no case for concluding a
 * Negotiation while Resources are still running. Because Guard params are jsonb, adding a
 * quantifier later needs no migration.
 *
 * <p>A Negotiation with no Resources at all passes. That is the vacuous reading of "every", and it
 * is left as the vacuous reading deliberately: refusing instead would need a rule about what an
 * empty Negotiation means, which is a product question nobody has asked.
 */
@Component
class TerminalAggregationGuard implements Guard<NoParams> {

  static final String TYPE_KEY = "TERMINAL_AGGREGATION";

  @Override
  public String typeKey() {
    return TYPE_KEY;
  }

  @Override
  public Class<NoParams> paramsType() {
    return NoParams.class;
  }

  @Override
  public GuardVerdict check(EvaluationContext context, NoParams params) {
    List<EvaluationContext.SiblingResource> unfinished =
        context.siblingResources().stream().filter(resource -> !resource.isFinished()).toList();
    if (unfinished.isEmpty()) {
      return GuardVerdict.pass();
    }
    return GuardVerdict.fail(
        "RESOURCES_STILL_RUNNING",
        Map.of(
            "unfinishedResourceIds",
            unfinished.stream().map(EvaluationContext.SiblingResource::resourceId).toList(),
            "unfinishedCount",
            unfinished.size(),
            "resourceCount",
            context.siblingResources().size()));
  }
}
