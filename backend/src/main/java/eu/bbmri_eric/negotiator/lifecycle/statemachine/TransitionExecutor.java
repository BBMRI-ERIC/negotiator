package eu.bbmri_eric.negotiator.lifecycle.statemachine;

import com.github.oxo42.stateless4j.StateConfiguration;
import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.StateMachineConfig;
import java.util.HashSet;
import java.util.Set;

/**
 * Executes a single lifecycle transition against a {@link StateMachineDefinition} (the Transition
 * Table) and returns its {@link TransitionOutcome}.
 *
 * <p>This is the one seam over the underlying stateless4j library: callers never see a stateless4j
 * type. The execution order of a successful transition is <em>guard → transition action → persist
 * transition listener → outcome</em>. The outcome is returned only after the listener completes, so
 * a returned target state always corresponds to a persisted (pending-commit) state change; a
 * listener failure propagates and the caller's transaction rolls back.
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
   * @throws InvalidTransitionException if {@code event} is not defined from {@code currentState} in
   *     the Transition Table, or if a guard denies the transition (the library exception is
   *     preserved as the cause).
   */
  public TransitionOutcome<C> fire(String currentState, String event, C context) {
    if (isNotDefined(currentState, event)) {
      throw new InvalidTransitionException(currentState, event);
    }
    OutcomeHolder<C> holder = new OutcomeHolder<>();
    StateMachine<String, String> machine = build(currentState, context, holder);
    try {
      machine.fire(event);
    } catch (IllegalStateException guardDenial) {
      // The event is defined (pre-checked above), so the library can only refuse it here because a
      // guard denied the transition. Any other exception (action, listener) is not caught.
      throw new InvalidTransitionException(currentState, event, guardDenial);
    }
    return holder.outcome;
  }

  private boolean isNotDefined(String currentState, String event) {
    return definition.transitionsFrom(currentState).stream()
        .noneMatch(descriptor -> descriptor.event().equals(event));
  }

  private StateMachine<String, String> build(
      String currentState, C context, OutcomeHolder<C> holder) {
    StateMachineConfig<String, String> config = new StateMachineConfig<>();
    Set<String> statesWithListener = new HashSet<>();
    for (TransitionDescriptor descriptor : definition.transitions()) {
      StateConfiguration<String, String> stateConfig = config.configure(descriptor.sourceState());
      Runnable action = resolveAction(descriptor, context);
      if (descriptor.guardName() != null) {
        Guard<C> guard = beanResolver.resolve(descriptor.guardName(), Guard.class);
        stateConfig.permitIf(
            descriptor.event(),
            descriptor.targetState(),
            () -> guard.evaluate(context),
            action::run);
      } else {
        stateConfig.permit(descriptor.event(), descriptor.targetState(), action::run);
      }
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
