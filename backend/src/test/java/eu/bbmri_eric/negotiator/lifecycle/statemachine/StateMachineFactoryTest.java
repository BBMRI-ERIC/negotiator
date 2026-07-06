package eu.bbmri_eric.negotiator.lifecycle.statemachine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.oxo42.stateless4j.StateMachine;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StateMachineFactoryTest {

  private record TestContext(String id) implements TransitionContext {}

  private static final StateMachineDefinition SIMPLE_DEFINITION =
      new StateMachineDefinition(
          "DRAFT", List.of(new TransitionDescriptor("DRAFT", "SUBMITTED", "SUBMIT")));

  private static BeanResolver noBeans() {
    return beansOf(Map.of());
  }

  private static BeanResolver beansOf(Map<String, Object> beans) {
    return new BeanResolver() {
      @Override
      @SuppressWarnings("unchecked")
      public <T> T resolve(String beanName, Class<T> type) {
        Object bean = beans.get(beanName);
        if (bean == null) {
          throw new IllegalStateException("no bean registered for " + beanName);
        }
        return (T) bean;
      }
    };
  }

  private static TransitionListener<TestContext> noopListener() {
    return outcome -> {};
  }

  @Test
  void fire_definedTransition_movesToTargetState() {
    StateMachineFactory<TestContext> factory =
        new StateMachineFactory<>(SIMPLE_DEFINITION, noBeans(), noopListener());
    StateMachine<String, String> machine = factory.build("DRAFT", new TestContext("neg-1"));

    machine.fire("SUBMIT");

    assertThat(machine.getState()).isEqualTo("SUBMITTED");
  }

  @Test
  void fire_undefinedTransition_throwsAndDoesNotChangeState() {
    StateMachineFactory<TestContext> factory =
        new StateMachineFactory<>(SIMPLE_DEFINITION, noBeans(), noopListener());
    StateMachine<String, String> machine = factory.build("DRAFT", new TestContext("neg-1"));

    assertThatThrownBy(() -> machine.fire("APPROVE")).isInstanceOf(IllegalStateException.class);
    assertThat(machine.getState()).isEqualTo("DRAFT");
  }

  @Test
  void fire_withActionDescriptor_executesResolvedActionWithContext() {
    List<TestContext> received = new ArrayList<>();
    TransitionAction<TestContext> action = received::add;
    StateMachineDefinition definition =
        new StateMachineDefinition(
            "DRAFT",
            List.of(
                new TransitionDescriptor(
                    "DRAFT", "SUBMITTED", "SUBMIT", "enablePosts", null, java.util.Set.of())));
    StateMachineFactory<TestContext> factory =
        new StateMachineFactory<>(
            definition, beansOf(Map.of("enablePosts", action)), noopListener());
    TestContext context = new TestContext("neg-1");
    StateMachine<String, String> machine = factory.build("DRAFT", context);

    machine.fire("SUBMIT");

    assertThat(received).containsExactly(context);
  }

  @Test
  void fire_definedTransition_invokesListenerWithOutcome() {
    List<TransitionOutcome<TestContext>> outcomes = new ArrayList<>();
    TransitionListener<TestContext> listener = outcomes::add;
    StateMachineFactory<TestContext> factory =
        new StateMachineFactory<>(SIMPLE_DEFINITION, noBeans(), listener);
    TestContext context = new TestContext("neg-1");
    StateMachine<String, String> machine = factory.build("DRAFT", context);

    machine.fire("SUBMIT");

    assertThat(outcomes)
        .containsExactly(new TransitionOutcome<>("DRAFT", "SUBMITTED", "SUBMIT", context));
  }

  @Test
  void fire_guardDenies_throwsAndDoesNotExecuteActionOrListener() {
    List<TestContext> actionCalls = new ArrayList<>();
    List<TransitionOutcome<TestContext>> outcomes = new ArrayList<>();
    TransitionAction<TestContext> action = actionCalls::add;
    Guard<TestContext> guard = context -> false;
    StateMachineDefinition definition =
        new StateMachineDefinition(
            "DRAFT",
            List.of(
                new TransitionDescriptor(
                    "DRAFT",
                    "SUBMITTED",
                    "SUBMIT",
                    "enablePosts",
                    "denyGuard",
                    java.util.Set.of())));
    StateMachineFactory<TestContext> factory =
        new StateMachineFactory<>(
            definition, beansOf(Map.of("enablePosts", action, "denyGuard", guard)), outcomes::add);
    StateMachine<String, String> machine = factory.build("DRAFT", new TestContext("neg-1"));

    assertThatThrownBy(() -> machine.fire("SUBMIT")).isInstanceOf(IllegalStateException.class);
    assertThat(machine.getState()).isEqualTo("DRAFT");
    assertThat(actionCalls).isEmpty();
    assertThat(outcomes).isEmpty();
  }

  @Test
  void fire_guardAllows_transitionsNormally() {
    Guard<TestContext> guard = context -> true;
    StateMachineDefinition definition =
        new StateMachineDefinition(
            "DRAFT",
            List.of(
                new TransitionDescriptor(
                    "DRAFT", "SUBMITTED", "SUBMIT", null, "allowGuard", java.util.Set.of())));
    StateMachineFactory<TestContext> factory =
        new StateMachineFactory<>(definition, beansOf(Map.of("allowGuard", guard)), noopListener());
    StateMachine<String, String> machine = factory.build("DRAFT", new TestContext("neg-1"));

    machine.fire("SUBMIT");

    assertThat(machine.getState()).isEqualTo("SUBMITTED");
  }
}
