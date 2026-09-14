package eu.bbmri_eric.negotiator.lifecycle.firing;

import eu.bbmri_eric.negotiator.lifecycle.evaluation.EvaluationOutcome;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext;
import java.util.Set;

/**
 * Event Firing: ids and a Caller in, a judged move out, committed when permitted.
 *
 * <p>The Transition Evaluator only judges. This owns everything around it: reading the Definition
 * Version Pin, resolving the graph, assembling the {@link EvaluationContext}, and after a permitted
 * move, writing the State and the Lifecycle Record and running the Actions. One implementation serves
 * both Definition Scopes so the assembly cannot drift between them.
 *
 * <p>{@link #possibleEvents} is a dry run of {@link #fire} over the Events out of the current
 * State, against a context built by the same code. That keeps the listing and the gate in
 * agreement.
 *
 * <p>No controller calls Event Firing. Requests go through the Spring Statemachine path.
 *
 * <p>It serves Resource scope only and refuses a Negotiation-scope reference with {@link
 * UnsupportedScopeException}.
 */
public interface EventFiring {

  /**
   * Fires {@code event} at the Lifecycle {@code lifecycle} names, on behalf of {@code caller}.
   *
   * <p>When the Evaluation Pipeline permits the move, this method has already committed it by the
   * time it returns: it has written the new State, appended one Lifecycle Record, then run the
   * Transition's Actions in order. When it refuses, nothing is written, no Action runs, and the
   * returned {@link EvaluationOutcome.Refused} carries the category and reason code.
   *
   * <p>A refusal is a return value, not an exception, so each caller maps the category to its own
   * HTTP status. Exceptions mean the data is unusable: an Unstarted Lifecycle, or a graph that
   * cannot compile.
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
   * <p>It leaves out any Event blocked by Required Authority, an unmet Information Requirement or a
   * Guard. It writes nothing and runs no Action, because only {@link #fire} runs Actions.
   *
   * <p>An empty set means nothing is available, such as in a terminal State. Every failure to
   * answer throws instead.
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
