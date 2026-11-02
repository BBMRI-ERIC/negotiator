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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Answers what a Definition Version permits from a given State. It takes a Compiled Graph and the
 * runtime facts, holds no state, reads no data of its own and changes nothing.
 *
 * <p>It depends on no repository on purpose. Loading the graph then stays a separate, testable
 * step, and the evaluator cannot cause an N+1 query across a negotiation's resources. Its one
 * dependency, {@link InformationRequirementSatisfaction}, is a port for the one fact the context
 * cannot carry.
 *
 * <p>{@link #possibleEvents} runs the same check as {@link #evaluate} over every Event out of the
 * current State. The two cannot disagree, so the UI never offers an Event that firing then refuses.
 */
@Component
@RequiredArgsConstructor
public final class TransitionEvaluator {

  private final InformationRequirementSatisfaction requirements;

  /**
   * Whether {@code event} may fire against a Lifecycle in {@code context}'s current State, and
   * where it would go.
   *
   * <p>It checks the current State before the Event. Otherwise a corrupt Definition Version Pin
   * that coincides with an unknown Event would come back as {@code UNKNOWN_EVENT} and blame the
   * caller for a fault in the data.
   *
   * @throws eu.bbmri_eric.negotiator.lifecycle.graph.InvalidGraphException if {@code graph} does
   *     not declare the current State
   */
  public EvaluationOutcome evaluate(CompiledGraph graph, String event, EvaluationContext context) {
    String fromState = context.subject().currentState();
    graph.requireDeclaredState(fromState);
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
   *
   * <p>An empty set means only that nothing is available. It never means the graph could not
   * answer, because an undeclared State throws instead.
   *
   * @throws eu.bbmri_eric.negotiator.lifecycle.graph.InvalidGraphException if {@code graph} does
   *     not declare the current State
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
   * The Evaluation Pipeline, in fixed order: Required Authority, then Information Requirements, then
   * Guards. It stops at the first failure, so the caller sees the earliest category that failed.
   */
  private EvaluationOutcome gate(CompiledTransition transition, EvaluationContext context) {
    if (!holdsRequiredAuthority(transition.requiredAuthority(), context)) {
      return new EvaluationOutcome.Refused(
          FailureCategory.AUTHORIZATION,
          "REQUIRED_AUTHORITY_NOT_HELD",
          Map.of("requiredAuthority", namesOf(transition.requiredAuthority())));
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
   * A Guard's refusal is always a domain-state conflict. The Guard supplies the reason code and
   * details, and the pipeline assigns the category, so no Guard can report a different one.
   */
  private static EvaluationOutcome refusedByGuard(GuardStep guard, GuardVerdict verdict) {
    Map<String, Object> details = new LinkedHashMap<>(verdict.details());
    details.put("guard", guard.typeKey());
    return new EvaluationOutcome.Refused(
        FailureCategory.DOMAIN_STATE_CONFLICT, verdict.reasonCode(), details);
  }

  /**
   * Asks who is firing, not whether the domain permits the move. Holding any one value of the set
   * is enough, so {@code {IS_ADMIN, IS_CREATOR}} means "admin or creator". An empty set admits any
   * person.
   *
   * <p>The system and people never share a Transition. A person never holds {@code SYSTEM}, so a
   * System Event never appears in Possible Events or fires over REST. The Orchestration Trigger
   * holds only {@code SYSTEM}, so an empty set does not admit it. Otherwise it could fire every
   * Transition that requires no authority.
   */
  private static boolean holdsRequiredAuthority(
      Set<RequiredAuthority> required, EvaluationContext context) {
    return switch (context.caller()) {
      case EvaluationContext.Caller.TheSystem ignored ->
          required.contains(RequiredAuthority.SYSTEM);
      case EvaluationContext.Caller.Person person ->
          required.isEmpty()
              || required.stream().anyMatch(authority -> personHolds(authority, person, context));
    };
  }

  private static boolean personHolds(
      RequiredAuthority authority,
      EvaluationContext.Caller.Person person,
      EvaluationContext context) {
    return switch (authority) {
      case IS_ADMIN -> person.authorities().contains(ROLE_ADMIN);
      case IS_CREATOR ->
          Long.valueOf(person.personId()).equals(context.subject().negotiationCreatorId());
      case IS_REPRESENTATIVE -> context.subject().representativeIds().contains(person.personId());
      case SYSTEM -> false;
    };
  }

  /** In declaration order, so the same set is always reported the same way. */
  private static List<String> namesOf(Set<RequiredAuthority> authorities) {
    return authorities.stream().sorted().map(RequiredAuthority::name).toList();
  }

  /**
   * The granted authority on the token, as {@code isAdmin} checks it. The {@code admin} column of
   * the Person row does not grant admin Events.
   */
  private static final String ROLE_ADMIN = "ROLE_ADMIN";
}
