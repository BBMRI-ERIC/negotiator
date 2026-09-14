package eu.bbmri_eric.negotiator.lifecycle.firing;

import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledGraph;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import lombok.NonNull;

/**
 * One assembled move: everything the Transition Evaluator takes about a Lifecycle, and the row a
 * permitted move is then written through.
 *
 * <p>The graph and the context travel together because they have to agree — the context's current
 * State must be a State this graph declares, and the graph is the pinned version's precisely so
 * that it is. Handing them around separately is how a caller ends up judging one Lifecycle's State
 * against another version's graph, which the Compiled Graph cache already refuses at its own level
 * and which nothing would catch at this one.
 *
 * <p><b>The Negotiation rides along so that the commit writes through the instance that was
 * judged</b>, rather than loading it a second time and relying on the persistence context to return
 * the same object. It would return the same object — this all runs in one transaction — but that
 * makes "judged and written are the same row" a fact about Hibernate's identity map rather than
 * about this code, and it leaves a second not-found branch that cannot be reached and cannot be
 * tested. Carrying it says the thing outright.
 *
 * <p>Holding the entity is why this record is package private and stays so. The module's edge deals
 * in ids and {@code graph} vocabulary; nothing outside {@code firing} is handed a Negotiation.
 *
 * <p>Only the Event is missing, and only because it differs between the two callers: {@link
 * EventFiring#fire} has one, {@link EventFiring#possibleEvents} tries every candidate the graph
 * offers from here.
 */
record AssembledEvaluation(
    @NonNull CompiledGraph graph,
    @NonNull EvaluationContext context,
    @NonNull Negotiation negotiation) {}
