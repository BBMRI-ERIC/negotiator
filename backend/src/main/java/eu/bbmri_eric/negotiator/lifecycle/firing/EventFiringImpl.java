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
 * Event Firing assembled: rows read into a context, the context judged by the Transition Evaluator,
 * and — only when it permits — the move committed.
 *
 * <p><b>This is the first thing in the Lifecycle subsystem that writes.</b> Nothing in {@code
 * graph}, {@code evaluation} or {@code definition} carries a {@code @Transactional}, deliberately:
 * ADR 0001 puts committing a move and running its Actions "in the services around the evaluator",
 * and {@code EvaluationOutcome.Permitted.actions()} reports the chain rather than running it — a
 * property {@code TransitionEvaluatorTest} pins in a test whose name is the whole argument, {@code
 * evaluate_whenPermitted_reportsTheOrderedActionChainAndRunsNoneOfIt}. This class is where the
 * report becomes an effect.
 *
 * <p><b>The class is package private and its methods are public.</b> Spring's transaction proxy
 * applies to public methods only and ignores a package-private one <em>silently</em>, so a commit
 * written on a package-private method would run outside a transaction and look fine. The methods
 * are public because they implement {@link EventFiring}; the class is not, because {@link
 * EventFiring} is the whole of what this module offers.
 */
@Service
@RequiredArgsConstructor
class EventFiringImpl implements EventFiring {

  private final LifecycleContextAssembler assembler;
  private final TransitionEvaluator evaluator;
  private final NegotiationRepository negotiations;

  /**
   * One transaction covering the read, the judgement and the commit.
   *
   * <p>It has to cover the read as well as the write, and not only because the assembly traverses
   * lazy associations: the Negotiation the assembler loaded is the instance this method then writes
   * through, so a shorter transaction would judge one snapshot and commit to another. Guards read
   * State that a concurrent move can change, which is the ordinary reason a gate and its commit
   * belong in one.
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
   * Read-only, which sets the Hibernate session's flush mode to manual, so a dirtied entity is not
   * written back even if some future edit here dirties one. Not a sandbox — raw JDBC would still
   * write — but it removes the likeliest way for a listing to start writing by accident.
   *
   * <p>What makes the dry run safe is upstream of that: the evaluator reports an Action chain
   * rather than running it, so evaluating twenty candidate Events runs no Action at all, and only
   * {@link #fire} ever turns that report into an effect.
   */
  @Override
  @Transactional(readOnly = true)
  public Set<String> possibleEvents(LifecycleRef lifecycle, EvaluationContext.Caller caller) {
    AssembledEvaluation assembled = assembler.assemble(lifecycle, caller);
    return evaluator.possibleEvents(assembled.graph(), assembled.context());
  }

  /**
   * The new State, the Lifecycle Record, then the Action chain — in that order, because an Action
   * runs "only after a Transition commits" and must be able to read the State it is reacting to.
   *
   * <p><b>The State and the record are one call, on purpose.</b> {@code
   * Negotiation.setStateForResource} writes the link row and appends the Lifecycle Record together,
   * which is how every existing path already does it and is why "exactly one record" falls out
   * rather than being counted. Writing the two separately here would duplicate the entity's rule —
   * including the one exception, that arriving at the initial State appends nothing — and the two
   * copies would drift.
   *
   * <p><b>A committed State must be one the legacy enum still knows.</b> The Lifecycle Record
   * stores its State as {@code NegotiationResourceState} and resolves the name through {@code
   * valueOf}, so a Definition Version naming a State that enum does not carry compiles, evaluates
   * and is permitted — and then fails here, at the append. Nothing in this module can widen that:
   * it lifts when ADR 0008's {@code state_id} FK conversion replaces the enum column, which is the
   * cutover's. Worth knowing before seeding a definition with new State names.
   *
   * <p><b>No state-change event is published, and that is a gap rather than a settled answer.</b>
   * The Resource equivalent, {@code ResourcePersistStateChangeListener.onPersist}, publishes {@code
   * ResourceStateChangeEvent}, and notifications and webhook deliveries ride on it; a cutover that
   * swapped the service onto this path without adding a publish would silence both. It is left out
   * here because this slab's brief is the commit — State, Lifecycle Record, Actions — and because
   * nothing calls this from production, so nothing is currently unannounced; publishing eagerly
   * would instead mean firing notifications from a path with no callers. Whoever wires this in owns
   * the decision, and should know that the neighbouring question is already constrained: ticket 02
   * fixes that Spawn must <em>not</em> publish this event and that notification is to ride on a new
   * {@code ResourceLifecyclesSpawnedEvent} instead — so "publish the same event from everywhere" is
   * already known to be wrong for one caller.
   *
   * <p><b>Which Lifecycle is written comes from what was judged, not from the caller's
   * reference.</b> They name the same thing, and taking the Negotiation and the subject off the
   * assembled move is what makes that structural: the row written to is by construction the row the
   * pipeline judged, so no edit can commit a move to a Lifecycle other than the one it was
   * permitted for.
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
   * In configured order, and every one of them.
   *
   * <p><b>"Only after a Transition commits" is read as the move, not as the database.</b> The
   * Transition has committed in the sense that matters to an Action — the new State is written and
   * the Lifecycle Record appended, and an Action reading either sees the move — but all of it is
   * still one database transaction, so an Action that throws rolls back the State and the record
   * with it. Both readings are defensible and this is the conservative one: it cannot leave a moved
   * Lifecycle whose Actions half ran. The cost is that an Action failure is indistinguishable from
   * a refusal to the caller, and whether an Action ought to be able to fail independently of the
   * move it follows is a real question no ADR has answered. Today's equivalent path is the same
   * shape — {@code onPersist} is one {@code @Transactional} method that writes and then publishes.
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
