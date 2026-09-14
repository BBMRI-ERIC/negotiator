package eu.bbmri_eric.negotiator.lifecycle.firing;

import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledGraph;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import lombok.NonNull;

/**
 * Everything the Transition Evaluator needs about one Lifecycle, plus the Negotiation a permitted
 * move is written through. The Event is left out because {@link EventFiring#fire} has one and
 * {@link EventFiring#possibleEvents} tries each candidate.
 *
 * <p>The graph and the context travel together because the context's current State must belong to
 * the pinned version's graph. Passed separately, a caller could judge a State against another
 * version's graph.
 *
 * <p>The commit writes through the Negotiation that was judged, so it needs no second load and no
 * unreachable not-found branch. Holding the entity keeps this record package-private.
 */
record AssembledEvaluation(
    @NonNull CompiledGraph graph,
    @NonNull EvaluationContext context,
    @NonNull Negotiation negotiation) {}
