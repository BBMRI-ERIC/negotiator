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
 * Turns a {@link LifecycleRef} into what the Transition Evaluator takes: the Compiled Graph of the
 * pinned Definition Version and an {@link EvaluationContext} filled in from the rows the ids name.
 *
 * <p>{@link EventFiring#fire} and {@link EventFiring#possibleEvents} both use this assembly, so
 * Possible Events and the gate judge the same context.
 *
 * <p>It reads the Definition Version Pin and never runs Definition Resolution. A Lifecycle in
 * flight is judged against the version pinned when it started, or publishing a new version would
 * move work already under way.
 */
@Component
@RequiredArgsConstructor
class LifecycleContextAssembler {

  private final NegotiationRepository negotiations;
  private final LifecycleDefinitions definitions;

  /**
   * Reads the rows {@code lifecycle} names and pairs the context with the graph that judges it.
   *
   * <p>It runs inside the caller's transaction, because it traverses lazy associations and the
   * entity it reads is the one a permitted move writes through.
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
   * The Resource as this Negotiation links it, found by source id. A Resource not linked to this
   * Negotiation has no Lifecycle here, so the lookup goes through the Negotiation.
   */
  private static Resource resourceOf(Negotiation negotiation, String resourceId) {
    return negotiation.getResources().stream()
        .filter(candidate -> candidate.getSourceId().equals(resourceId))
        .findFirst()
        .orElseThrow(() -> new EntityNotFoundException(resourceId));
  }

  /**
   * The Definition Version Pin, refused when absent. Falling back to the active version would judge
   * a Lifecycle in flight against a graph published after it started.
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
   * <p>The column is nullable, and without this check a null would surface as a bare {@code
   * NullPointerException} from the context factory. Guards read this State, so a default would
   * decide them instead of refusing.
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
   * Null when the creator row is gone. Then {@code IS_CREATOR} holds for nobody and every other
   * authority still answers.
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
