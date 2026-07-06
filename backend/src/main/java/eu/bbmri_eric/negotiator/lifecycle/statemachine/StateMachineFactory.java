package eu.bbmri_eric.negotiator.lifecycle.statemachine;

import com.github.oxo42.stateless4j.StateConfiguration;
import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.StateMachineConfig;
import java.util.HashSet;
import java.util.Set;

/**
 * Builds a fresh, disposable stateless4j {@link StateMachine} from a {@link
 * StateMachineDefinition}.
 */
public class StateMachineFactory<C extends TransitionContext> {

  private final StateMachineDefinition definition;
  private final BeanResolver beanResolver;
  private final TransitionListener<C> listener;

  public StateMachineFactory(
      StateMachineDefinition definition,
      BeanResolver beanResolver,
      TransitionListener<C> listener) {
    this.definition = definition;
    this.beanResolver = beanResolver;
    this.listener = listener;
  }

  public StateMachine<String, String> build(String currentState, C context) {
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
      attachListener(config, descriptor.targetState(), statesWithListener, context);
    }
    return new StateMachine<>(currentState, config);
  }

  private void attachListener(
      StateMachineConfig<String, String> config,
      String targetState,
      Set<String> statesWithListener,
      C context) {
    if (!statesWithListener.add(targetState)) {
      return;
    }
    config
        .configure(targetState)
        .onEntry(
            transition ->
                listener.onTransition(
                    new TransitionOutcome<>(
                        transition.getSource(),
                        transition.getDestination(),
                        transition.getTrigger(),
                        context)));
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
}
