package eu.bbmri_eric.negotiator.lifecycle.statemachine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TransitionExecutorTest {

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
  void fire_definedTransition_returnsOutcomeWithSourceTargetEventAndContext() {
    TransitionExecutor<TestContext> executor =
        new TransitionExecutor<>(SIMPLE_DEFINITION, noBeans(), noopListener());
    TestContext context = new TestContext("neg-1");

    TransitionOutcome<TestContext> outcome = executor.fire("DRAFT", "SUBMIT", context);

    assertThat(outcome).isEqualTo(new TransitionOutcome<>("DRAFT", "SUBMITTED", "SUBMIT", context));
  }

  @Test
  void fire_undefinedEvent_throwsInvalidTransitionAndInvokesNoActionOrListener() {
    List<TestContext> actionCalls = new ArrayList<>();
    List<TransitionOutcome<TestContext>> outcomes = new ArrayList<>();
    TransitionAction<TestContext> action = actionCalls::add;
    StateMachineDefinition definition =
        new StateMachineDefinition(
            "DRAFT",
            List.of(
                new TransitionDescriptor(
                    "DRAFT", "SUBMITTED", "SUBMIT", "enablePosts", null, Set.of())));
    TransitionExecutor<TestContext> executor =
        new TransitionExecutor<>(definition, beansOf(Map.of("enablePosts", action)), outcomes::add);

    assertThatThrownBy(() -> executor.fire("DRAFT", "APPROVE", new TestContext("neg-1")))
        .isInstanceOf(InvalidTransitionException.class);
    assertThat(actionCalls).isEmpty();
    assertThat(outcomes).isEmpty();
  }

  @Test
  void fire_definedTransitionWithAction_executesActionWithContext() {
    List<TestContext> received = new ArrayList<>();
    TransitionAction<TestContext> action = received::add;
    StateMachineDefinition definition =
        new StateMachineDefinition(
            "DRAFT",
            List.of(
                new TransitionDescriptor(
                    "DRAFT", "SUBMITTED", "SUBMIT", "enablePosts", null, Set.of())));
    TransitionExecutor<TestContext> executor =
        new TransitionExecutor<>(
            definition, beansOf(Map.of("enablePosts", action)), noopListener());
    TestContext context = new TestContext("neg-1");

    executor.fire("DRAFT", "SUBMIT", context);

    assertThat(received).containsExactly(context);
  }

  @Test
  void fire_definedTransition_runsListenerBeforeReturningOutcome() {
    List<TransitionOutcome<TestContext>> received = new ArrayList<>();
    TransitionListener<TestContext> listener = received::add;
    TransitionExecutor<TestContext> executor =
        new TransitionExecutor<>(SIMPLE_DEFINITION, noBeans(), listener);
    TestContext context = new TestContext("neg-1");

    TransitionOutcome<TestContext> outcome = executor.fire("DRAFT", "SUBMIT", context);

    assertThat(received).containsExactly(outcome);
  }

  @Test
  void fire_guardDenies_throwsInvalidTransitionWithLibraryExceptionAsCause() {
    Guard<TestContext> guard = context -> false;
    StateMachineDefinition definition =
        new StateMachineDefinition(
            "DRAFT",
            List.of(
                new TransitionDescriptor(
                    "DRAFT", "SUBMITTED", "SUBMIT", null, "denyGuard", Set.of())));
    TransitionExecutor<TestContext> executor =
        new TransitionExecutor<>(definition, beansOf(Map.of("denyGuard", guard)), noopListener());

    assertThatThrownBy(() -> executor.fire("DRAFT", "SUBMIT", new TestContext("neg-1")))
        .isInstanceOf(InvalidTransitionException.class)
        .hasCauseInstanceOf(IllegalStateException.class);
  }

  @Test
  void fire_guardAllows_transitionsNormally() {
    Guard<TestContext> guard = context -> true;
    StateMachineDefinition definition =
        new StateMachineDefinition(
            "DRAFT",
            List.of(
                new TransitionDescriptor(
                    "DRAFT", "SUBMITTED", "SUBMIT", null, "allowGuard", Set.of())));
    TransitionExecutor<TestContext> executor =
        new TransitionExecutor<>(definition, beansOf(Map.of("allowGuard", guard)), noopListener());

    TransitionOutcome<TestContext> outcome =
        executor.fire("DRAFT", "SUBMIT", new TestContext("neg-1"));

    assertThat(outcome.targetState()).isEqualTo("SUBMITTED");
  }

  @Test
  void fire_throwingListener_propagatesExceptionUntouchedAndReturnsNoOutcome() {
    RuntimeException persistFailure = new RuntimeException("persist failed");
    TransitionListener<TestContext> listener =
        outcome -> {
          throw persistFailure;
        };
    TransitionExecutor<TestContext> executor =
        new TransitionExecutor<>(SIMPLE_DEFINITION, noBeans(), listener);

    assertThatThrownBy(() -> executor.fire("DRAFT", "SUBMIT", new TestContext("neg-1")))
        .isSameAs(persistFailure);
  }
}
