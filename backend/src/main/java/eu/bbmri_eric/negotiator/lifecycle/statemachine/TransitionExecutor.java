package eu.bbmri_eric.negotiator.lifecycle.statemachine;

import com.github.oxo42.stateless4j.StateConfiguration;
import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.StateMachineConfig;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The deep module over the underlying stateless4j library. Its public interface is two methods:
 * {@code fire(currentState, event, context)}, which returns a {@link TransitionOutcome}, and
 * {@code permittedEvents(currentState, context)}, which lists the events whose guards pass for that
 * context. Both answer through the same guard evaluation, so listing and firing cannot drift.
 * stateless4j is an implementation detail confined behind this seam; callers never see a library
 * type.
 *
 * <p>The execution order of a successful transition is <em>machine guard → transition guard →
 * precondition → transition action → persist transition listener → outcome</em>. The outcome is
 * returned only after the listener completes, so a returned target state always corresponds to a
 * persisted (pending-commit) state change; a listener failure propagates and the caller's
 * transaction rolls back.
 */
public class TransitionExecutor<C extends TransitionContext> {

  private final StateMachineDefinition definition;
  private final BeanResolver beanResolver;
  private final TransitionListener<C> listener;

  public TransitionExecutor(
      StateMachineDefinition definition,
      BeanResolver beanResolver,
      TransitionListener<C> listener) {
    this.definition = definition;
    this.beanResolver = beanResolver;
    this.listener = listener;
  }

  /**
   * Fires {@code event} from {@code currentState} and returns the resulting outcome.
   *
   * <p>Execution order: definedness check → machine guard → transition guard → precondition →
   * transition action → persist transition listener → outcome.
   *
   * @throws InvalidTransitionException if {@code event} is not defined from {@code currentState}.
   * @throws TransitionDeniedException if a machine or transition guard denies the caller.
   * @throws TransitionPreconditionException if a precondition refuses the firing.
   */
  public TransitionOutcome<C> fire(String currentState, String event, C context) {
    TransitionDescriptor descriptor = findDescriptor(currentState, event);
    if (descriptor == null) {
      throw new InvalidTransitionException(currentState, event);
    }
    evaluateMachineGuard(currentState, event, context);
    evaluateTransitionGuard(descriptor, currentState, event, context);
    evaluatePrecondition(context, event);
    OutcomeHolder<C> holder = new OutcomeHolder<>();
    StateMachine<String, String> machine = build(currentState, context, holder);
    machine.fire(event);
    return holder.outcome;
  }

  /**
   * Lists the events from {@code currentState} whose guards pass for {@code context}. Preconditions
   * are not evaluated, so an event with an unmet precondition stays discoverable.
   */
  public Set<String> permittedEvents(String currentState, C context) {
    if (definition.machineGuardName() != null) {
      Guard<C> machineGuard = resolveGuard(definition.machineGuardName());
      if (!machineGuard.evaluate(context)) {
        return Set.of();
      }
    }
    return definition.transitionsFrom(currentState).stream()
        .filter(descriptor -> transitionGuardPasses(descriptor, context))
        .map(TransitionDescriptor::event)
        .collect(Collectors.toSet());
  }

  private TransitionDescriptor findDescriptor(String currentState, String event) {
    return definition.transitionsFrom(currentState).stream()
        .filter(d -> d.event().equals(event))
        .findFirst()
        .orElse(null);
  }

  @SuppressWarnings("unchecked")
  private void evaluateMachineGuard(String currentState, String event, C context) {
    if (definition.machineGuardName() != null) {
      Guard<C> machineGuard = beanResolver.resolve(definition.machineGuardName(), Guard.class);
      if (!machineGuard.evaluate(context)) {
        throw new TransitionDeniedException(currentState, event);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void evaluateTransitionGuard(
      TransitionDescriptor descriptor, String currentState, String event, C context) {
    if (descriptor.guardName() != null) {
      Guard<C> guard = beanResolver.resolve(descriptor.guardName(), Guard.class);
      if (!guard.evaluate(context)) {
        throw new TransitionDeniedException(currentState, event);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void evaluatePrecondition(C context, String event) {
    if (definition.preconditionName() != null) {
      Precondition<C> precondition =
          beanResolver.resolve(definition.preconditionName(), Precondition.class);
      precondition.check(context, event);
    }
  }

  @SuppressWarnings("unchecked")
  private boolean transitionGuardPasses(TransitionDescriptor descriptor, C context) {
    if (descriptor.guardName() == null) {
      return true;
    }
    Guard<C> guard = beanResolver.resolve(descriptor.guardName(), Guard.class);
    return guard.evaluate(context);
  }

  @SuppressWarnings("unchecked")
  private Guard<C> resolveGuard(String name) {
    return beanResolver.resolve(name, Guard.class);
  }

  private StateMachine<String, String> build(
      String currentState, C context, OutcomeHolder<C> holder) {
    StateMachineConfig<String, String> config = new StateMachineConfig<>();
    Set<String> statesWithListener = new HashSet<>();
    for (TransitionDescriptor descriptor : definition.transitions()) {
      StateConfiguration<String, String> stateConfig = config.configure(descriptor.sourceState());
      Runnable action = resolveAction(descriptor, context);
      stateConfig.permit(descriptor.event(), descriptor.targetState(), action::run);
      attachListener(config, descriptor.targetState(), statesWithListener, context, holder);
    }
    return new StateMachine<>(currentState, config);
  }

  private void attachListener(
      StateMachineConfig<String, String> config,
      String targetState,
      Set<String> statesWithListener,
      C context,
      OutcomeHolder<C> holder) {
    if (!statesWithListener.add(targetState)) {
      return;
    }
    config
        .configure(targetState)
        .onEntry(
            transition -> {
              TransitionOutcome<C> outcome =
                  new TransitionOutcome<>(
                      transition.getSource(),
                      transition.getDestination(),
                      transition.getTrigger(),
                      context);
              listener.onTransition(outcome);
              holder.outcome = outcome;
            });
  }

  @SuppressWarnings("unchecked")
  private Runnable resolveAction(TransitionDescriptor descriptor, C context) {
    if (descriptor.actionName() == null) {
      return () -> {};
    }
    TransitionAction<C> action =
        (TransitionAction<C>) beanResolver.resolve(descriptor.actionName(), TransitionAction.class);
    return () -> action.execute(context);
  }

  private static final class OutcomeHolder<C extends TransitionContext> {
    private TransitionOutcome<C> outcome;
  }
}
