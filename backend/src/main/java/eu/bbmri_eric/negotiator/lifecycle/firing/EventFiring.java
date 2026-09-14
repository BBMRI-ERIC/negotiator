package eu.bbmri_eric.negotiator.lifecycle.firing;

import eu.bbmri_eric.negotiator.lifecycle.evaluation.EvaluationOutcome;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext;
import java.util.Set;

/**
 * Event Firing, as everything outside this module sees it: ids and a Caller in, a judged — and when
 * permitted, committed — move out.
 *
 * <p>This is the half of the Lifecycle subsystem the Transition Evaluator deliberately refuses to
 * be. The evaluator is handed a Compiled Graph and an already-assembled {@link EvaluationContext}
 * and answers; it reads nothing and writes nothing. Everything between a caller's two ids and that
 * answer — reading the Definition Version Pin, resolving the graph, gathering the creator, the
 * representatives and the parent Negotiation's State — and everything after a permitted answer —
 * the new State, the Lifecycle Record, the Action chain — has no other owner. Written twice, once
 * per Definition Scope, it would be exactly the drift ADR 0001 says one evaluator exists to
 * prevent, relocated into the assembly.
 *
 * <p><b>The two methods share one assembly and one gate.</b> {@link #possibleEvents} is a dry run
 * of {@link #fire} over the Events reachable from the current State, against a context built by the
 * same code from the same rows. ADR 0005's central point is that the listing and the gate can never
 * disagree; that holds only while the context they judge is also the same, which is why the listing
 * is not a lighter query here.
 *
 * <p><b>It replaces neither Lifecycle service.</b> Standing decision 2 is to decouple consumers
 * first and swap the engine second, so the Spring Statemachine path stays live alongside this one
 * and nothing calls this from a controller. Wiring it in is the cutover slab's.
 *
 * <p><b>Resource scope only, today.</b> A Negotiation-scope reference is refused by name rather
 * than half-served — see {@link LifecycleRef} for why the two Scopes are a closed set, and {@link
 * UnsupportedScopeException} for what the missing half costs.
 */
public interface EventFiring {

  /**
   * Fires {@code event} at the Lifecycle {@code lifecycle} names, on behalf of {@code caller}.
   *
   * <p>When the Evaluation Pipeline permits the move, this method has already committed it by the
   * time it returns: the new State written, exactly one Lifecycle Record appended, and the
   * Transition's Action chain run in order — in that sequence, because an Action runs "only after a
   * Transition commits". When it refuses, nothing is written, no Action runs, and the returned
   * {@link EvaluationOutcome.Refused} carries the category and reason code intact.
   *
   * <p><b>A refusal is a return value, not an exception.</b> ADR 0005's categories map to 403, 422
   * and 409 and {@code FailureCategory} deliberately does not carry the status, because that
   * mapping belongs to whatever surface is answering — so this hands back the category and lets it
   * decide. What <em>is</em> thrown is the data being unusable rather than the move being
   * disallowed: an unstarted Lifecycle, or a graph that cannot be compiled.
   *
   * @throws UnstartedLifecycleException if the Lifecycle carries no Definition Version Pin or no
   *     State, so there is nothing to judge the move against
   * @throws eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException if the ids name no
   *     Negotiation, or no Resource of it
   * @throws eu.bbmri_eric.negotiator.lifecycle.graph.InvalidGraphException if the pinned Definition
   *     Version does not exist or does not compile
   * @throws UnsupportedScopeException if {@code lifecycle} is a Negotiation's
   */
  EvaluationOutcome fire(LifecycleRef lifecycle, String event, EvaluationContext.Caller caller);

  /**
   * The Events {@code caller} could fire at this Lifecycle right now.
   *
   * <p>Anything blocked — by Required Authority, by an unmet Information Requirement, or by a Guard
   * — is left out rather than listed as unavailable, so the set is exactly what the caller may act
   * on. Nothing is written and no Action is run: evaluating twenty candidate Events must not set
   * twenty posts' visibility, which is why the evaluator reports an Action chain rather than
   * running it and why this module runs one only on the {@link #fire} path.
   *
   * <p>An empty set means "nothing is available" and only that — a terminal State, or a State every
   * Event out of which is blocked. It never means the question could not be answered: every way
   * this could fail to answer throws.
   *
   * @throws UnstartedLifecycleException if the Lifecycle carries no Definition Version Pin or no
   *     State
   * @throws eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException if the ids name no
   *     Negotiation, or no Resource of it
   * @throws eu.bbmri_eric.negotiator.lifecycle.graph.InvalidGraphException if the pinned Definition
   *     Version does not exist or does not compile
   * @throws UnsupportedScopeException if {@code lifecycle} is a Negotiation's
   */
  Set<String> possibleEvents(LifecycleRef lifecycle, EvaluationContext.Caller caller);
}
