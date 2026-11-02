package eu.bbmri_eric.negotiator.lifecycle.graph;

import java.util.List;
import java.util.Set;
import lombok.NonNull;

/**
 * One Transition of a Compiled Graph: firing {@code event} in {@code fromState} moves to {@code
 * toState} if the caller holds any one of {@code requiredAuthority} and the Guard chain permits it.
 *
 * <p>States and Events are identified by name, because callers hold names from a {@code
 * current_state} column or a REST path. Row ids stay in the definition package.
 *
 * @param requiredAuthority who may fire the Event; holding any one value is enough, and an empty
 *     set admits any person. {@code SYSTEM} stands alone, because beside a person value it would
 *     make a System Event appear in Possible Events. The constructor checks this because the
 *     compiler receives rows, not the table's CHECK.
 * @param guards the effective Guard chain in run order, with definition-level Wiring ahead of this
 *     Transition's own, each group in {@code sort_order}. The Transition Evaluator runs one loop
 *     and never sees the two Guard scopes.
 * @param actions the Action chain in {@code sort_order}. Actions are wired only per Transition.
 *     Event Firing runs them after a permitted move.
 */
public record CompiledTransition(
    @NonNull String fromState,
    @NonNull String event,
    @NonNull String toState,
    @NonNull Set<RequiredAuthority> requiredAuthority,
    List<GuardStep> guards,
    List<ActionStep> actions) {

  public CompiledTransition {
    requiredAuthority = Set.copyOf(requiredAuthority);
    requireWellFormed(fromState, event, toState, requiredAuthority);
    guards = List.copyOf(guards);
    actions = List.copyOf(actions);
  }

  private static void requireWellFormed(
      String fromState, String event, String toState, Set<RequiredAuthority> required) {
    if (required.size() > 1 && required.contains(RequiredAuthority.SYSTEM)) {
      throw new InvalidGraphException(
          "Transition %s --%s--> %s requires %s. SYSTEM must stand alone."
              .formatted(fromState, event, toState, required.stream().sorted().toList()));
    }
  }

  /** A Transition with no Guards and no Actions. */
  public CompiledTransition(
      String fromState, String event, String toState, Set<RequiredAuthority> requiredAuthority) {
    this(fromState, event, toState, requiredAuthority, List.of(), List.of());
  }

  /** A Transition with Guards and no Actions. */
  public CompiledTransition(
      String fromState,
      String event,
      String toState,
      Set<RequiredAuthority> requiredAuthority,
      List<GuardStep> guards) {
    this(fromState, event, toState, requiredAuthority, guards, List.of());
  }
}
