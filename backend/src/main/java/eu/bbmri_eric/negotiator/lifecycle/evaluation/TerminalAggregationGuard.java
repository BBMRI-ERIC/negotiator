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
 * <p>Resources of one Negotiation may pin different Definition Versions, so this Guard holds no list
 * of State names. It asks each Resource's own Definition Version whether its current State is
 * terminal.
 *
 * <p>Which States are terminal is seed content. To keep current behaviour, the Resource seed marks
 * only the two States the old check counted. A Negotiation whose Resources end in {@code
 * RESOURCE_NOT_MADE_AVAILABLE} or {@code RESOURCE_UNAVAILABLE_WILLING_TO_COLLECT} therefore never
 * concludes.
 *
 * <p>A Negotiation with no Resources passes.
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
