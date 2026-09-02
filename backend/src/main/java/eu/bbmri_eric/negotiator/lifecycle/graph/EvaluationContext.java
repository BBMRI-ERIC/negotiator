package eu.bbmri_eric.negotiator.lifecycle.graph;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Everything about the running domain that gating a move is allowed to depend on, handed in whole
 * rather than looked up. This is the type that makes ADR 0001's constraint real: an evaluator with
 * no repository can only know what is on here, so every new thing a Guard wants to know shows up as
 * a change to this record and has to be justified once, in the open, rather than as a quietly added
 * query.
 *
 * <p>It holds names and ids, never entities. A {@code Negotiation} on here would put the whole
 * object graph within a Guard's reach and the constraint would be back to a convention.
 *
 * <p><b>This is where the two Definition Scopes actually differ.</b> The compiled graph needed no
 * scope and the evaluator needs no scope, but the context does: a Resource-scope evaluation has a
 * parent Negotiation and no siblings, and a Negotiation-scope one has siblings and no parent. That
 * asymmetry is named by the two factory methods rather than by a discriminator field, so a caller
 * says which kind of Lifecycle it is moving at the moment it builds the context, and nothing
 * downstream branches on it.
 */
public record EvaluationContext(
    Caller caller,
    Subject subject,
    String parentNegotiationState,
    List<SiblingResource> siblingResources) {

  public EvaluationContext {
    Objects.requireNonNull(caller, "caller");
    Objects.requireNonNull(subject, "subject");
    siblingResources = List.copyOf(siblingResources);
  }

  /**
   * A Negotiation's own Lifecycle. Carries its Resources, because that is what a
   * terminal-aggregation Guard counts; carries no parent, because a Negotiation has none.
   */
  public static EvaluationContext forNegotiation(
      Caller caller, Subject subject, List<SiblingResource> resources) {
    return new EvaluationContext(caller, subject, null, resources);
  }

  /**
   * One Resource's Lifecycle within a Negotiation. Carries the parent's current State, because that
   * is what the parent-approval Guard reads — putting the State on the context rather than behind a
   * lookup is what lets that Guard need no port and do no I/O at all.
   */
  public static EvaluationContext forResource(
      Caller caller, Subject subject, String parentNegotiationState) {
    return new EvaluationContext(
        caller, subject, Objects.requireNonNull(parentNegotiationState), List.of());
  }

  /**
   * Who is firing. A Person and the system are two <em>kinds</em> of caller rather than one kind
   * with a flag's worth of difference, and the type says so: ADR 0007 makes an Event machine-fired
   * if and only if its authority is {@code SYSTEM}, so the two must not overlap in either direction
   * — a human may never satisfy {@code SYSTEM}, and an Orchestration Trigger may never satisfy
   * {@code NONE}.
   *
   * <p>Sealed rather than a boolean because that mutual exclusion is then structural. An authority
   * rule switching over this cannot compile while it has forgotten one of the two, whereas a rule
   * reading a {@code system} flag can silently fall through to the human branch — which is precisely
   * the overload of {@code NONE} that ADR 0007 introduced {@code SYSTEM} to avoid.
   */
  public sealed interface Caller {

    /**
     * @param authorities the granted authorities on the token, not columns of a Person row. {@code
     *     isAdmin} was verified to mean the {@code ROLE_ADMIN} authority and not the {@code admin}
     *     column, so a seeded Person with {@code admin = true} and no authority is offered nothing.
     */
    record Person(long personId, Set<String> authorities) implements Caller {

      public Person {
        authorities = Set.copyOf(authorities);
      }
    }

    /** The Orchestration Trigger, and nothing else. Satisfies {@code SYSTEM} and only that. */
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
   * <p>The two authority fields are given as raw facts rather than as pre-computed booleans on
   * purpose. A context carrying {@code isCreator} and {@code isRepresentative} would be smaller and
   * would move the rule out to each caller — which is the drift ADR 0001 wants gone, since the
   * whole reason there is one evaluator is that the two Lifecycles cannot disagree about how a move
   * is judged.
   *
   * <p>The two id fields are here because the Information Requirement stage needs them, and it is
   * the only thing on the context that needs an <em>identity</em> rather than a property. That is
   * also what makes satisfaction a port rather than a function: everything else about gating a move
   * can be handed in whole, and whether a form has been submitted cannot.
   *
   * @param negotiationId the Negotiation, whichever Lifecycle is moving
   * @param resourceId the Resource, or null when the Negotiation's own Lifecycle is moving
   * @param currentState the State the Lifecycle is in now
   * @param negotiationCreatorId the creator of this Negotiation, or of the Resource's parent
   * @param representativeIds who represents this Resource — exactly this Resource, not its
   *     Negotiation and not the representative's other Resources. Empty for a Negotiation.
   */
  public record Subject(
      String negotiationId,
      String resourceId,
      String currentState,
      Long negotiationCreatorId,
      Set<Long> representativeIds) {

    public Subject {
      Objects.requireNonNull(negotiationId, "negotiationId");
      Objects.requireNonNull(currentState, "currentState");
      representativeIds = Set.copyOf(representativeIds);
    }

    public static Subject negotiation(String negotiationId, String currentState, Long creatorId) {
      return new Subject(negotiationId, null, currentState, creatorId, Set.of());
    }

    public static Subject resource(
        String negotiationId,
        String resourceId,
        String currentState,
        Long parentCreatorId,
        Set<Long> representativeIds) {
      return new Subject(
          negotiationId,
          Objects.requireNonNull(resourceId, "resourceId"),
          currentState,
          parentCreatorId,
          representativeIds);
    }
  }

  /**
   * One Resource of the Negotiation being evaluated, paired with <em>its own</em> compiled graph.
   *
   * <p>The pairing is the point, and it is the most expensive thing on this record. Terminality
   * cannot be a list of State names, because two Resources of one Negotiation may run different
   * Definition Versions — so the only way to ask "is this Resource finished" is to ask the version
   * pinned to that Resource. Carrying the graph here is what keeps that question answerable without
   * the evaluator loading anything: the caller resolves N graphs through the cache before it calls,
   * which is exactly the "explicit, testable step" ADR 0001 wanted, and exactly the N-way load it
   * warned would otherwise be discovered under load.
   */
  public record SiblingResource(long resourceId, String currentState, CompiledGraph graph) {

    public SiblingResource {
      Objects.requireNonNull(currentState, "currentState");
      Objects.requireNonNull(graph, "graph");
    }

    /** Whether this Resource's own Definition Version calls its current State finished. */
    public boolean isFinished() {
      return graph.isTerminal(currentState);
    }
  }
}
