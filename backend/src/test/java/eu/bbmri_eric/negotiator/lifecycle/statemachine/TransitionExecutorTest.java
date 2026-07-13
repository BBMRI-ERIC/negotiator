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
  void fire_undefinedEvent_throwsInvalidTransitionWithoutEvaluatingGuards() {
    List<String> evaluated = new ArrayList<>();
    Guard<TestContext> machineGuard = ctx -> { evaluated.add("machineGuard"); return true; };
    Guard<TestContext> transitionGuard = ctx -> { evaluated.add("transitionGuard"); return true; };
    List<TestContext> actionCalls = new ArrayList<>();
    TransitionAction<TestContext> action = actionCalls::add;
    StateMachineDefinition definition =
        new StateMachineDefinition(
            "DRAFT",
            List.of(
                new TransitionDescriptor(
                    "DRAFT", "SUBMITTED", "SUBMIT", "enablePosts", "transitionGuard")),
            "machineGuard",
            null);
    TransitionExecutor<TestContext> executor =
        new TransitionExecutor<>(
            definition,
            beansOf(
                Map.of(
                    "enablePosts",
                    action,
                    "machineGuard",
                    machineGuard,
                    "transitionGuard",
                    transitionGuard)),
            noopListener());

    assertThatThrownBy(() -> executor.fire("DRAFT", "APPROVE", new TestContext("neg-1")))
        .isInstanceOf(InvalidTransitionException.class);
    assertThat(evaluated).isEmpty();
    assertThat(actionCalls).isEmpty();
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
                    "DRAFT", "SUBMITTED", "SUBMIT", "enablePosts", null)));
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
  void fire_transitionGuardDenies_throwsTransitionDeniedException() {
    Guard<TestContext> guard = context -> false;
    StateMachineDefinition definition =
        new StateMachineDefinition(
            "DRAFT",
            List.of(
                new TransitionDescriptor(
                    "DRAFT", "SUBMITTED", "SUBMIT", null, "denyGuard")));
    TransitionExecutor<TestContext> executor =
        new TransitionExecutor<>(definition, beansOf(Map.of("denyGuard", guard)), noopListener());

    assertThatThrownBy(() -> executor.fire("DRAFT", "SUBMIT", new TestContext("neg-1")))
        .isInstanceOf(TransitionDeniedException.class);
  }

  @Test
  void fire_guardAllows_transitionsNormally() {
    Guard<TestContext> guard = context -> true;
    StateMachineDefinition definition =
        new StateMachineDefinition(
            "DRAFT",
            List.of(
                new TransitionDescriptor(
                    "DRAFT", "SUBMITTED", "SUBMIT", null, "allowGuard")));
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

  @Test
  void fire_machineGuardDenies_throwsTransitionDeniedException() {
    Guard<TestContext> machineGuard = context -> false;
    StateMachineDefinition definition =
        new StateMachineDefinition(
            "DRAFT",
            List.of(new TransitionDescriptor("DRAFT", "SUBMITTED", "SUBMIT")),
            "machineGuard",
            null);
    TransitionExecutor<TestContext> executor =
        new TransitionExecutor<>(
            definition, beansOf(Map.of("machineGuard", machineGuard)), noopListener());

    assertThatThrownBy(() -> executor.fire("DRAFT", "SUBMIT", new TestContext("neg-1")))
        .isInstanceOf(TransitionDeniedException.class);
  }

  @Test
  void fire_preconditionRefuses_throwsPreconditionExceptionAndRunsNoActionOrListener() {
    List<String> executionOrder = new ArrayList<>();
    Precondition<TestContext> precondition =
        (ctx, event) -> {
          executionOrder.add("precondition");
          throw new TransitionPreconditionException("not yet");
        };
    TransitionAction<TestContext> action = ctx -> executionOrder.add("action");
    TransitionListener<TestContext> listener = outcome -> executionOrder.add("listener");
    StateMachineDefinition definition =
        new StateMachineDefinition(
            "DRAFT",
            List.of(new TransitionDescriptor("DRAFT", "SUBMITTED", "SUBMIT", "action", null)),
            null,
            "precondition");
    TransitionExecutor<TestContext> executor =
        new TransitionExecutor<>(
            definition,
            beansOf(Map.of("precondition", precondition, "action", action)),
            listener);

    assertThatThrownBy(() -> executor.fire("DRAFT", "SUBMIT", new TestContext("neg-1")))
        .isInstanceOf(TransitionPreconditionException.class);
    assertThat(executionOrder).containsExactly("precondition");
  }

  @Test
  void fire_executionOrder_machineGuardGuardPreconditionActionListenerOutcome() {
    List<String> executionOrder = new ArrayList<>();
    Guard<TestContext> machineGuard = ctx -> { executionOrder.add("machineGuard"); return true; };
    Guard<TestContext> transitionGuard = ctx -> { executionOrder.add("guard"); return true; };
    Precondition<TestContext> precondition = (ctx, event) -> executionOrder.add("precondition");
    TransitionAction<TestContext> action = ctx -> executionOrder.add("action");
    TransitionListener<TestContext> listener = outcome -> executionOrder.add("listener");
    StateMachineDefinition definition =
        new StateMachineDefinition(
            "DRAFT",
            List.of(
                new TransitionDescriptor(
                    "DRAFT", "SUBMITTED", "SUBMIT", "action", "guard")),
            "machineGuard",
            "precondition");
    TransitionExecutor<TestContext> executor =
        new TransitionExecutor<>(
            definition,
            beansOf(
                Map.of(
                    "machineGuard",
                    machineGuard,
                    "guard",
                    transitionGuard,
                    "precondition",
                    precondition,
                    "action",
                    action)),
            listener);

    TransitionOutcome<TestContext> outcome =
        executor.fire("DRAFT", "SUBMIT", new TestContext("neg-1"));

    assertThat(executionOrder)
        .containsExactly("machineGuard", "guard", "precondition", "action", "listener");
    assertThat(outcome.targetState()).isEqualTo("SUBMITTED");
  }

  @Test
  void permittedEvents_noGuards_returnsAllEventsFromCurrentState() {
    StateMachineDefinition definition =
        new StateMachineDefinition(
            "DRAFT",
            List.of(
                new TransitionDescriptor("DRAFT", "SUBMITTED", "SUBMIT"),
                new TransitionDescriptor("DRAFT", "APPROVED", "APPROVE")));
    TransitionExecutor<TestContext> executor =
        new TransitionExecutor<>(definition, noBeans(), noopListener());

    Set<String> events = executor.permittedEvents("DRAFT", new TestContext("neg-1"));

    assertThat(events).containsExactlyInAnyOrder("SUBMIT", "APPROVE");
  }

  @Test
  void permittedEvents_machineGuardDenies_returnsEmpty() {
    Guard<TestContext> machineGuard = context -> false;
    StateMachineDefinition definition =
        new StateMachineDefinition(
            "DRAFT",
            List.of(new TransitionDescriptor("DRAFT", "SUBMITTED", "SUBMIT")),
            "machineGuard",
            null);
    TransitionExecutor<TestContext> executor =
        new TransitionExecutor<>(
            definition, beansOf(Map.of("machineGuard", machineGuard)), noopListener());

    Set<String> events = executor.permittedEvents("DRAFT", new TestContext("neg-1"));

    assertThat(events).isEmpty();
  }

  @Test
  void permittedEvents_transitionGuardDenies_excludesEventKeepsSiblings() {
    Guard<TestContext> denyGuard = context -> false;
    Guard<TestContext> allowGuard = context -> true;
    StateMachineDefinition definition =
        new StateMachineDefinition(
            "DRAFT",
            List.of(
                new TransitionDescriptor("DRAFT", "SUBMITTED", "SUBMIT", null, "denyGuard"),
                new TransitionDescriptor("DRAFT", "APPROVED", "APPROVE", null, "allowGuard")));
    TransitionExecutor<TestContext> executor =
        new TransitionExecutor<>(
            definition,
            beansOf(Map.of("denyGuard", denyGuard, "allowGuard", allowGuard)),
            noopListener());

    Set<String> events = executor.permittedEvents("DRAFT", new TestContext("neg-1"));

    assertThat(events).containsExactly("APPROVE");
  }

  @Test
  void permittedEvents_preconditionNotEvaluated_eventStillListed() {
    Precondition<TestContext> precondition =
        (ctx, event) -> {
          throw new TransitionPreconditionException("not yet");
        };
    StateMachineDefinition definition =
        new StateMachineDefinition(
            "DRAFT",
            List.of(new TransitionDescriptor("DRAFT", "SUBMITTED", "SUBMIT")),
            null,
            "precondition");
    TransitionExecutor<TestContext> executor =
        new TransitionExecutor<>(
            definition, beansOf(Map.of("precondition", precondition)), noopListener());

    Set<String> events = executor.permittedEvents("DRAFT", new TestContext("neg-1"));

    assertThat(events).containsExactly("SUBMIT");
  }
}
