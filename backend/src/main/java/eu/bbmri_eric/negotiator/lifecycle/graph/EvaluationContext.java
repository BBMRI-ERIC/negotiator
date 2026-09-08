package eu.bbmri_eric.negotiator.lifecycle.graph;

import java.util.List;
import java.util.Set;
import lombok.NonNull;

/**
 * Everything a Guard may depend on, handed in whole. The Transition Evaluator has no repository, so
 * anything new a Guard needs to know has to be added to this record.
 *
 * <p>It holds names and ids, never entities, so a Guard cannot reach into the object graph.
 *
 * <p>The two Definition Scopes differ only here. A Resource-scope context has a parent Negotiation
 * State and no siblings, and a Negotiation-scope context has siblings and no parent. The two
 * factory methods name the difference, so nothing downstream branches on it.
 */
public record EvaluationContext(
    @NonNull Caller caller,
    @NonNull Subject subject,
    String parentNegotiationState,
    List<SiblingResource> siblingResources) {

  public EvaluationContext {
    siblingResources = List.copyOf(siblingResources);
  }

  /** A Negotiation's own Lifecycle, with the Resources that {@code TERMINAL_AGGREGATION} reads. */
  public static EvaluationContext forNegotiation(
      Caller caller, Subject subject, List<SiblingResource> resources) {
    return new EvaluationContext(caller, subject, null, resources);
  }

  /**
   * One Resource's Lifecycle within a Negotiation. It carries the parent's current State so the
   * parent-approval Guard does no I/O.
   */
  public static EvaluationContext forResource(
      Caller caller, Subject subject, @NonNull String parentNegotiationState) {
    return new EvaluationContext(caller, subject, parentNegotiationState, List.of());
  }

  /**
   * Who is firing. A Person and the system are two <em>kinds</em> of caller rather than one kind
   * with a flag's worth of difference, and the type says so: ADR 0007 makes an Event machine-fired
   * if and only if its authority is {@code SYSTEM}, so the two must not overlap in either direction
   * — a human may never satisfy {@code SYSTEM}, and an Orchestration Trigger may never satisfy
   * {@code NONE}.
   *
   * <p>The interface is sealed so that an authority rule switching over it fails to compile if it
   * forgets a case. A boolean flag could fall through to the person branch unnoticed.
   */
  public sealed interface Caller {

    /**
     * @param authorities the granted authorities on the token, not columns of a Person row. A
     *     Person with {@code admin = true} and no {@code ROLE_ADMIN} authority is not an admin.
     */
    record Person(long personId, Set<String> authorities) implements Caller {

      public Person {
        authorities = Set.copyOf(authorities);
      }
    }

    /** The Orchestration Trigger. It satisfies {@code SYSTEM} and nothing else. */
    record TheSystem() implements Caller {}

    static Caller person(long personId, Set<String> authorities) {
      return new Person(personId, authorities);
    }

    static Caller system() {
      return new TheSystem();
    }
  }

  /**
   * The Lifecycle being moved: which one it is, where it is, and the facts an authority rule needs.
   *
   * <p>The creator and representatives are raw facts, not precomputed booleans, so the authority
   * rule stays in the Transition Evaluator and both Lifecycles judge a move the same way.
   *
   * <p>The Information Requirement stage needs the two ids, because whether a form was submitted
   * has to be looked up and cannot be handed in.
   *
   * @param negotiationId the Negotiation, whichever Lifecycle is moving
   * @param resourceId the Resource, or null when the Negotiation's own Lifecycle is moving
   * @param currentState the State the Lifecycle is in now
   * @param negotiationCreatorId the creator of this Negotiation, or of the Resource's parent
   * @param representativeIds who represents exactly this Resource. Empty for a Negotiation.
   */
  public record Subject(
      @NonNull String negotiationId,
      String resourceId,
      @NonNull String currentState,
      Long negotiationCreatorId,
      Set<Long> representativeIds) {

    public Subject {
      representativeIds = Set.copyOf(representativeIds);
    }

    public static Subject negotiation(String negotiationId, String currentState, Long creatorId) {
      return new Subject(negotiationId, null, currentState, creatorId, Set.of());
    }

    public static Subject resource(
        String negotiationId,
        @NonNull String resourceId,
        String currentState,
        Long parentCreatorId,
        Set<Long> representativeIds) {
      return new Subject(
          negotiationId, resourceId, currentState, parentCreatorId, representativeIds);
    }
  }

  /**
   * One Resource of the Negotiation being evaluated, paired with its own Compiled Graph.
   *
   * <p>Two Resources of one Negotiation may run different Definition Versions, so only the version
   * pinned to a Resource can say whether its State is terminal. The caller resolves these graphs
   * through the cache before evaluation, so the Transition Evaluator loads nothing.
   */
  public record SiblingResource(
      long resourceId, @NonNull String currentState, @NonNull CompiledGraph graph) {

    /** Whether this Resource's own Definition Version calls its current State finished. */
    public boolean isFinished() {
      return graph.isTerminal(currentState);
    }
  }
}
