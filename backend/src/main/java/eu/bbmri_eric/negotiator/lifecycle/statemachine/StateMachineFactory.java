package eu.bbmri_eric.negotiator.lifecycle.statemachine;

import com.github.oxo42.stateless4j.StateConfiguration;
import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.StateMachineConfig;
import java.util.List;

public final class StateMachineFactory<C extends TransitionContext> {

  private final StateMachineDefinition definition;
  private final BeanResolver resolver;
  private final List<TransitionListener<C>> listeners;

  public StateMachineFactory(
      StateMachineDefinition definition,
      BeanResolver resolver,
      List<TransitionListener<C>> listeners) {
    this.definition = definition;
    this.resolver = resolver;
    this.listeners = List.copyOf(listeners);
  }

  public StateMachineFactory(StateMachineDefinition definition, BeanResolver resolver) {
    this(definition, resolver, List.of());
  }

  public StateMachineFactory(StateMachineDefinition definition) {
    this(
        definition,
        new BeanResolver() {
          @Override
          public <T> T resolve(String name, Class<T> type) {
            return null;
          }
        },
        List.of());
  }

  public NegotiatorStateMachine<C> build(String currentState, C context) {
    StateMachineConfig<String, String> config = new StateMachineConfig<>();
    for (TransitionDescriptor transition : definition.transitions()) {
      StateConfiguration<String, String> stateConfig = config.configure(transition.sourceState());
      Guard<C> guard = transition.guardName() != null ? resolveGuard(transition.guardName()) : null;
      TransitionAction<C> action =
          transition.actionName() != null ? resolveAction(transition.actionName()) : null;
      if (guard != null && action != null) {
        stateConfig.permitIf(
            transition.event(),
            transition.targetState(),
            () -> guard.isSatisfied(context),
            () -> action.execute(context));
      } else if (guard != null) {
        stateConfig.permitIf(
            transition.event(), transition.targetState(), () -> guard.isSatisfied(context));
      } else if (action != null) {
        stateConfig.permit(
            transition.event(), transition.targetState(), () -> action.execute(context));
      } else {
        stateConfig.permit(transition.event(), transition.targetState());
      }
    }
    StateMachine<String, String> machine = new StateMachine<>(currentState, config);
    return new NegotiatorStateMachine<>(machine, context, listeners);
  }

  @SuppressWarnings("unchecked")
  private TransitionAction<C> resolveAction(String name) {
    return resolver.resolve(name, TransitionAction.class);
  }

  @SuppressWarnings("unchecked")
  private Guard<C> resolveGuard(String name) {
    return resolver.resolve(name, Guard.class);
  }
}
