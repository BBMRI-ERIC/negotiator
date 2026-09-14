package eu.bbmri_eric.negotiator.lifecycle.firing;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.lifecycle.definition.LifecycleDefinitions;
import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledGraph;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.user.Person;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Turns a {@link LifecycleRef} into the two things the Transition Evaluator takes: the Compiled
 * Graph of the Definition Version pinned to that Lifecycle, and an {@link EvaluationContext} filled
 * in from the rows the ids name.
 *
 * <p><b>This class is the reason the slab exists.</b> The evaluator's context is wide where its
 * interface is narrow — a Resource-scope evaluation needs the Negotiation's creator id, the
 * Resource's representative ids, the parent Negotiation's State and the Resource's own pinned graph
 * — and until this existed every one of those was hand-built in a test. All 26 of {@code
 * TransitionEvaluatorTest}'s contexts still are, and that is correct for a pure unit test; what no
 * test in the suite could reach was a context assembled wrongly <em>from real rows</em>, which is
 * where the remaining bugs were.
 *
 * <p><b>One assembly, both callers.</b> {@link EventFiring#fire} and {@link
 * EventFiring#possibleEvents} come through here, so the listing judges the same context the gate
 * does. A lighter read for the listing would be the second code path ADR 0005 says must not exist.
 *
 * <p><b>The pin is read, never resolved.</b> Definition Resolution answers what <em>new</em> work
 * runs under; a Lifecycle already in flight is judged against the version pinned to it when it
 * started, or the pin would do nothing and publishing a new version would move work under way. So
 * this reads {@code lifecycleDefinitionId} off the row and asks {@link LifecycleDefinitions} for
 * that version's graph — {@code resolveForResource()} is not called here and must not be.
 */
@Component
@RequiredArgsConstructor
class LifecycleContextAssembler {

  private final NegotiationRepository negotiations;
  private final LifecycleDefinitions definitions;

  /**
   * Reads the rows {@code lifecycle} names and pairs the resulting context with the graph it is to
   * be judged against.
   *
   * <p>Called inside the caller's transaction: the Negotiation is traversed to its Resources and
   * their representatives, which are lazy associations, and the entity read here is the same
   * instance a permitted move then writes through.
   */
  AssembledEvaluation assemble(LifecycleRef lifecycle, EvaluationContext.Caller caller) {
    return switch (lifecycle) {
      case LifecycleRef.ResourceLifecycle resource -> assembleResource(resource, caller);
      case LifecycleRef.NegotiationLifecycle negotiation ->
          throw new UnsupportedScopeException(
              ("Event Firing does not assemble a Negotiation-scope context yet, so the Lifecycle of"
                      + " Negotiation %s cannot be moved through it. Resource scope is built; a"
                      + " Negotiation-scope evaluation needs every sibling Resource paired with its"
                      + " own Compiled Graph, and assembling one without them would let"
                      + " TERMINAL_AGGREGATION pass by counting nothing.")
                  .formatted(negotiation.negotiationId()));
    };
  }

  private AssembledEvaluation assembleResource(
      LifecycleRef.ResourceLifecycle lifecycle, EvaluationContext.Caller caller) {
    Negotiation negotiation =
        negotiations
            .findById(lifecycle.negotiationId())
            .orElseThrow(() -> new EntityNotFoundException(lifecycle.negotiationId()));
    Resource resource = resourceOf(negotiation, lifecycle.resourceId());

    CompiledGraph graph = definitions.graphFor(pinOf(negotiation, lifecycle));
    EvaluationContext.Subject subject =
        EvaluationContext.Subject.resource(
            negotiation.getId(),
            resource.getSourceId(),
            currentStateOf(negotiation, lifecycle),
            creatorIdOf(negotiation),
            representativeIdsOf(resource));
    return new AssembledEvaluation(
        graph,
        EvaluationContext.forResource(caller, subject, parentStateOf(negotiation)),
        negotiation);
  }

  /**
   * The Resource as this Negotiation links it, found by source id.
   *
   * <p>Looked up through the Negotiation rather than through a Resource repository on purpose: a
   * Resource that exists but is not linked to this Negotiation has no Lifecycle here, and a lookup
   * that found it anyway would assemble a context for a Lifecycle that does not exist.
   */
  private static Resource resourceOf(Negotiation negotiation, String resourceId) {
    return negotiation.getResources().stream()
        .filter(candidate -> candidate.getSourceId().equals(resourceId))
        .findFirst()
        .orElseThrow(() -> new EntityNotFoundException(resourceId));
  }

  /**
   * The Definition Version Pin, refused rather than defaulted when it is absent.
   *
   * <p>Falling back to the active version is the tempting thing here and is exactly wrong: it would
   * judge work in flight against a graph published after it started, which is the failure the pin
   * exists to prevent, and it would do it silently on every row until the migration backfills them.
   */
  private static long pinOf(Negotiation negotiation, LifecycleRef.ResourceLifecycle lifecycle) {
    Long pin = negotiation.getLifecycleDefinitionIdForResource(lifecycle.resourceId());
    if (pin == null) {
      throw new UnstartedLifecycleException(
          ("Resource %s of Negotiation %s carries no Definition Version Pin, so there is no graph to"
                  + " judge a move against. Its Lifecycle has not been spawned, or predates the"
                  + " pin and has not been backfilled.")
              .formatted(lifecycle.resourceId(), lifecycle.negotiationId()));
    }
    return pin;
  }

  /**
   * The parent Negotiation's own State, refused when it has none.
   *
   * <p>{@code EvaluationContext.forResource} declares this parameter {@code @NonNull}, so without
   * this check a Negotiation whose {@code current_state} is null — the column is nullable — fails
   * as a bare Lombok {@code NullPointerException} naming a parameter, from inside a factory method
   * the caller never called. That contradicts what {@link EventFiring} promises, which is that
   * every way this can fail to answer throws something that says what is wrong with which row.
   *
   * <p>It is the same fact as an unstarted Resource Lifecycle, one level up: a Resource-scope
   * evaluation is judged partly on where its parent has got to, and a parent that has got nowhere
   * cannot answer that. Guards read it — the live Resource path gates every Event on the parent
   * being IN_PROGRESS — so defaulting it to anything would decide those Guards rather than refuse.
   */
  private static String parentStateOf(Negotiation negotiation) {
    String parentState = negotiation.getCurrentState();
    if (parentState == null) {
      throw new UnstartedLifecycleException(
          ("Negotiation %s is in no State, so a move of one of its Resources cannot be judged: a"
                  + " Resource-scope evaluation is gated partly on where its parent Negotiation"
                  + " has got to.")
              .formatted(negotiation.getId()));
    }
    return parentState;
  }

  /** The State the move starts from, refused when the link row records none. */
  private static String currentStateOf(
      Negotiation negotiation, LifecycleRef.ResourceLifecycle lifecycle) {
    String currentState = negotiation.getCurrentStateForResource(lifecycle.resourceId());
    if (currentState == null) {
      throw new UnstartedLifecycleException(
          ("Resource %s of Negotiation %s is in no State, so no move has anywhere to start from."
                  + " Its Lifecycle has not been spawned.")
              .formatted(lifecycle.resourceId(), lifecycle.negotiationId()));
    }
    return currentState;
  }

  /**
   * Null on a Negotiation whose creator row is gone. Carried through as null rather than refused:
   * {@code IS_CREATOR} then holds for nobody, which is the safe reading, and every other authority
   * still answers.
   */
  private static Long creatorIdOf(Negotiation negotiation) {
    return negotiation.getCreatedBy() == null ? null : negotiation.getCreatedBy().getId();
  }

  private static Set<Long> representativeIdsOf(Resource resource) {
    return resource.getRepresentatives().stream()
        .map(Person::getId)
        .collect(Collectors.toUnmodifiableSet());
  }
}
