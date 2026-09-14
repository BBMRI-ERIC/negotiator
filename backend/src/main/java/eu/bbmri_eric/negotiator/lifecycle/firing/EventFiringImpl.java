package eu.bbmri_eric.negotiator.lifecycle.firing;

import eu.bbmri_eric.negotiator.lifecycle.evaluation.EvaluationOutcome;
import eu.bbmri_eric.negotiator.lifecycle.evaluation.TransitionEvaluator;
import eu.bbmri_eric.negotiator.lifecycle.graph.ActionContext;
import eu.bbmri_eric.negotiator.lifecycle.graph.ActionStep;
import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledTransition;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads rows into a context, has the Transition Evaluator judge it, and commits the move only when
 * it is permitted.
 *
 * <p>This is the only writer in the Lifecycle packages. The Transition Evaluator reports the Action
 * chain and this class runs it.
 *
 * <p>The methods are public because Spring's transaction proxy silently ignores package-private
 * methods.
 */
@Service
@RequiredArgsConstructor
class EventFiringImpl implements EventFiring {

  private final LifecycleContextAssembler assembler;
  private final TransitionEvaluator evaluator;
  private final NegotiationRepository negotiations;

  /**
   * One transaction covers the read, the judgement and the commit. The commit writes through the
   * Negotiation the assembler loaded, so a shorter transaction would judge one snapshot and commit
   * to another.
   */
  @Override
  @Transactional
  public EvaluationOutcome fire(
      LifecycleRef lifecycle, String event, EvaluationContext.Caller caller) {
    AssembledEvaluation assembled = assembler.assemble(lifecycle, caller);
    EvaluationOutcome outcome = evaluator.evaluate(assembled.graph(), event, assembled.context());
    if (outcome instanceof EvaluationOutcome.Permitted permitted) {
      commit(permitted, assembled);
    }
    return outcome;
  }

  /**
   * Read-only, so Hibernate does not flush a dirtied entity. The Transition Evaluator only reports
   * Actions, so the dry run runs none.
   */
  @Override
  @Transactional(readOnly = true)
  public Set<String> possibleEvents(LifecycleRef lifecycle, EvaluationContext.Caller caller) {
    AssembledEvaluation assembled = assembler.assemble(lifecycle, caller);
    return evaluator.possibleEvents(assembled.graph(), assembled.context());
  }

  /**
   * Writes the new State and the Lifecycle Record, then runs the Actions, so an Action can read the
   * State it reacts to.
   *
   * <p>{@code Negotiation.setStateForResource} writes the State and the Lifecycle Record in one
   * call, so the rule that arriving at the initial State appends no record lives in one place.
   *
   * <p>The committed State must still exist in {@code NegotiationResourceState}. The Lifecycle
   * Record resolves its State through {@code valueOf}, so a Definition Version with a new State
   * name compiles and is permitted, then fails here. Check this before seeding new State names.
   *
   * <p>No {@code ResourceStateChangeEvent} is published, unlike {@code
   * ResourcePersistStateChangeListener.onPersist}. Notifications and webhooks depend on that event,
   * so whoever routes production traffic here must add the publish. Spawn must not publish it.
   *
   * <p>The Negotiation and subject come from the assembled move, so the row written is the row the
   * Evaluation Pipeline judged.
   */
  private void commit(EvaluationOutcome.Permitted permitted, AssembledEvaluation assembled) {
    CompiledTransition transition = permitted.transition();
    Negotiation negotiation = assembled.negotiation();

    negotiation.setStateForResource(
        assembled.context().subject().resourceId(), transition.toState());
    negotiations.save(negotiation);

    runActions(permitted, assembled.context(), transition);
  }

  /**
   * Runs every Action in configured order.
   *
   * <p>Actions run inside the same database transaction as the move, so an Action that throws rolls
   * back the State and the Lifecycle Record. This never leaves a moved Lifecycle with half its
   * Actions run, but the caller cannot tell an Action failure from a refusal. Whether an Action may
   * fail without undoing the move is still open.
   */
  private static void runActions(
      EvaluationOutcome.Permitted permitted,
      EvaluationContext context,
      CompiledTransition transition) {
    ActionContext actionContext = new ActionContext(context, transition);
    for (ActionStep action : permitted.actions()) {
      action.run(actionContext);
    }
  }
}
