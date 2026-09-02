package eu.bbmri_eric.negotiator.lifecycle.evaluation;

import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledGraph;
import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledTransition;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext;
import eu.bbmri_eric.negotiator.lifecycle.graph.FailureCategory;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardStep;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardVerdict;
import eu.bbmri_eric.negotiator.lifecycle.graph.RequiredAuthority;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * The stateless component that answers what a Definition Version permits from a given State. It is
 * handed an already-compiled graph plus the runtime facts, and answers; it holds no state, reads no
 * data of its own, and changes nothing.
 *
 * <p><b>Look at the constructor.</b> There is no repository on it, no {@code EntityManager} and no
 * Spring Data type, and that is the design rather than an accident of the current feature set. ADR
 * 0001 calls it "a deliberate constraint rather than a performance claim: an evaluator that
 * structurally cannot query the database makes loading the definition graph an explicit, testable
 * step". An evaluator that owned its persistence would invite an N+1 across a negotiation's
 * resources, discovered only under load.
 *
 * <p>The one thing on the constructor is {@link InformationRequirementSatisfaction}, and it is an
 * interface for exactly that reason: whether a form has been submitted is the single question about
 * a move that cannot be handed in on the context, so it goes behind a port instead of behind a
 * query.
 *
 * <p><b>{@link #evaluate} and {@link #possibleEvents} are one path, not two.</b> The listing is a
 * dry run of the same function over the Events reachable from the current State, with blocked ones
 * omitted. ADR 0005's central point is that the two can therefore never disagree — which is why
 * today's dead-click bug, where an Event is offered and then refused on submit, cannot be written
 * here even by mistake.
 */
@Component
public final class TransitionEvaluator {

  private final InformationRequirementSatisfaction requirements;

  public TransitionEvaluator(InformationRequirementSatisfaction requirements) {
    this.requirements = requirements;
  }

  /**
   * Whether {@code event} may fire against a Lifecycle in {@code context}'s current State, and
   * where it would go.
   */
  public EvaluationOutcome evaluate(CompiledGraph graph, String event, EvaluationContext context) {
    String fromState = context.subject().currentState();
    if (!graph.declaresEvent(event)) {
      return new EvaluationOutcome.Refused(
          FailureCategory.NO_TRANSITION,
          "UNKNOWN_EVENT",
          Map.of("event", event, "definitionVersionId", graph.definitionVersionId()));
    }
    Optional<CompiledTransition> edge = graph.transition(fromState, event);
    if (edge.isEmpty()) {
      return new EvaluationOutcome.Refused(
          FailureCategory.NO_TRANSITION,
          "NO_TRANSITION_FOR_EVENT",
          Map.of("event", event, "fromState", fromState));
    }
    return gate(edge.get(), context);
  }

  /**
   * The Events a caller could fire right now. Anything blocked is left out rather than listed as
   * unavailable, so the set is exactly what a caller may act on.
   */
  public Set<String> possibleEvents(CompiledGraph graph, EvaluationContext context) {
    Set<String> permitted = new LinkedHashSet<>();
    for (CompiledTransition candidate : graph.transitionsFrom(context.subject().currentState())) {
      if (gate(candidate, context).permitted()) {
        permitted.add(candidate.event());
      }
    }
    return Set.copyOf(permitted);
  }

  /**
   * The Evaluation Pipeline. ADR 0005 fixes the order — Required Authority, then the Information
   * Requirement check, then Guards — and it short-circuits at the first failure, so the category a
   * caller sees is the earliest thing wrong rather than the last thing checked.
   */
  private EvaluationOutcome gate(CompiledTransition transition, EvaluationContext context) {
    if (!holdsRequiredAuthority(transition.requiredAuthority(), context)) {
      return new EvaluationOutcome.Refused(
          FailureCategory.AUTHORIZATION,
          "REQUIRED_AUTHORITY_NOT_HELD",
          Map.of("requiredAuthority", transition.requiredAuthority().name()));
    }
    GuardVerdict requirement = requirements.check(transition.event(), context);
    if (!requirement.passed()) {
      return new EvaluationOutcome.Refused(
          FailureCategory.UNMET_REQUIREMENT, requirement.reasonCode(), requirement.details());
    }
    for (GuardStep guard : transition.guards()) {
      GuardVerdict verdict = guard.check(context);
      if (!verdict.passed()) {
        return refusedByGuard(guard, verdict);
      }
    }
    return new EvaluationOutcome.Permitted(transition);
  }

  /**
   * A Guard's refusal is always a domain-state conflict. The Guard says <em>what</em> is wrong
   * through its reason code and details; the category is the stage's to assign, which is what keeps
   * ADR 0005's categories monotonic no matter how a Guard is written or where it is wired.
   */
  private static EvaluationOutcome refusedByGuard(GuardStep guard, GuardVerdict verdict) {
    Map<String, Object> details = new LinkedHashMap<>(verdict.details());
    details.put("guard", guard.typeKey());
    return new EvaluationOutcome.Refused(
        FailureCategory.DOMAIN_STATE_CONFLICT, verdict.reasonCode(), details);
  }

  /**
   * Asks <em>who is firing</em>, and nothing about whether the domain permits the move.
   *
   * <p>{@code SYSTEM} and the human authorities are mutually exclusive in both directions. ADR 0007
   * says an Event is machine-fired if and only if its authority is {@code SYSTEM}, so a human
   * caller can never satisfy it — the Event never appears in Possible Events and can never be fired
   * over REST — and, less obviously, the Orchestration Trigger can never satisfy {@code NONE}.
   * Reading {@code NONE} as "including the system" would have made every open Event
   * machine-fireable, which is exactly the overload ADR 0007 introduced {@code SYSTEM} to avoid.
   *
   * <p>Both halves of that are held by the type rather than by this code. {@code Caller} is sealed,
   * so the outer switch cannot compile while it has forgotten one of the two kinds, and there is no
   * fall-through from the machine case into the human rules for a later edit to introduce.
   */
  private static boolean holdsRequiredAuthority(
      RequiredAuthority required, EvaluationContext context) {
    return switch (context.caller()) {
      case EvaluationContext.Caller.TheSystem ignored -> required == RequiredAuthority.SYSTEM;
      case EvaluationContext.Caller.Person person ->
          switch (required) {
            case NONE -> true;
            case IS_ADMIN -> person.authorities().contains(ROLE_ADMIN);
            case IS_CREATOR ->
                Long.valueOf(person.personId())
                    .equals(context.subject().negotiationCreatorId());
            case IS_REPRESENTATIVE ->
                context.subject().representativeIds().contains(person.personId());
            case SYSTEM -> false;
          };
    };
  }

  /**
   * The granted authority on the token. Verified to be what {@code isAdmin} has always meant on
   * both machines — not the {@code admin} column of the Person row, which a seeded caller holds
   * while being offered nothing anywhere.
   */
  private static final String ROLE_ADMIN = "ROLE_ADMIN";
}
