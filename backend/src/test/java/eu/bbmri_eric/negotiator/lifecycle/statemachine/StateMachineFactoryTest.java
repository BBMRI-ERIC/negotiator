package eu.bbmri_eric.negotiator.lifecycle.statemachine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StateMachineFactoryTest {

  private record TestContext() implements TransitionContext {}

  private record MapBeanResolver(Map<String, Object> beans) implements BeanResolver {
    @Override
    public <T> T resolve(String name, Class<T> type) {
      return type.cast(beans.get(name));
    }
  }

  @Test
  void build_returnsMachineInitializedToGivenCurrentState() {
    StateMachineDefinition definition =
        new StateMachineDefinition(Set.of("DRAFT", "SUBMITTED"), List.of());
    StateMachineFactory<TestContext> factory = new StateMachineFactory<>(definition);

    NegotiatorStateMachine<TestContext> machine = factory.build("SUBMITTED", new TestContext());

    assertEquals("SUBMITTED", machine.currentState());
  }

  @Test
  void fire_definedTransition_changesStateAndReturnsFiredOutcome() {
    StateMachineDefinition definition =
        new StateMachineDefinition(
            Set.of("SUBMITTED", "IN_PROGRESS"),
            List.of(new TransitionDescriptor("SUBMITTED", "IN_PROGRESS", "APPROVE")));
    StateMachineFactory<TestContext> factory = new StateMachineFactory<>(definition);
    TestContext context = new TestContext();
    NegotiatorStateMachine<TestContext> machine = factory.build("SUBMITTED", context);

    TransitionOutcome<TestContext> outcome = machine.fire("APPROVE");

    assertEquals("SUBMITTED", outcome.fromState());
    assertEquals("IN_PROGRESS", outcome.toState());
    assertEquals("APPROVE", outcome.event());
    assertEquals(context, outcome.context());
    assertTrue(outcome.fired());
    assertEquals("IN_PROGRESS", machine.currentState());
  }

  @Test
  void fire_undefinedTransition_isNoOp() {
    StateMachineDefinition definition =
        new StateMachineDefinition(
            Set.of("SUBMITTED", "IN_PROGRESS"),
            List.of(new TransitionDescriptor("SUBMITTED", "IN_PROGRESS", "APPROVE")));
    StateMachineFactory<TestContext> factory = new StateMachineFactory<>(definition);
    NegotiatorStateMachine<TestContext> machine = factory.build("SUBMITTED", new TestContext());

    TransitionOutcome<TestContext> outcome = machine.fire("DECLINE");

    assertFalse(outcome.fired());
    assertEquals("SUBMITTED", outcome.fromState());
    assertEquals("SUBMITTED", outcome.toState());
    assertEquals("SUBMITTED", machine.currentState());
  }

  @Test
  void fire_transitionWithAction_invokesActionWithTypedContext() {
    TestContext context = new TestContext();
    List<TestContext> invokedWith = new ArrayList<>();
    TransitionAction<TestContext> action = invokedWith::add;
    BeanResolver resolver = new MapBeanResolver(Map.of("testAction", action));
    StateMachineDefinition definition =
        new StateMachineDefinition(
            Set.of("SUBMITTED", "IN_PROGRESS"),
            List.of(new TransitionDescriptor("SUBMITTED", "IN_PROGRESS", "APPROVE", "testAction")));
    StateMachineFactory<TestContext> factory = new StateMachineFactory<>(definition, resolver);
    NegotiatorStateMachine<TestContext> machine = factory.build("SUBMITTED", context);

    machine.fire("APPROVE");

    assertEquals(List.of(context), invokedWith);
  }

  @Test
  void fire_guardReturnsFalse_silentlyBlocksTransition() {
    TestContext context = new TestContext();
    Guard<TestContext> guard = ctx -> false;
    BeanResolver resolver = new MapBeanResolver(Map.of("testGuard", guard));
    StateMachineDefinition definition =
        new StateMachineDefinition(
            Set.of("SUBMITTED", "IN_PROGRESS"),
            List.of(
                new TransitionDescriptor(
                    "SUBMITTED", "IN_PROGRESS", "APPROVE", "testGuard", null)));
    StateMachineFactory<TestContext> factory = new StateMachineFactory<>(definition, resolver);
    NegotiatorStateMachine<TestContext> machine = factory.build("SUBMITTED", context);

    TransitionOutcome<TestContext> outcome = machine.fire("APPROVE");

    assertFalse(outcome.fired());
    assertEquals("SUBMITTED", machine.currentState());
  }

  @Test
  void fire_guardThrows_propagatesAndLeavesStateUnchanged() {
    TestContext context = new TestContext();
    class GuardFailure extends RuntimeException {}
    Guard<TestContext> guard =
        ctx -> {
          throw new GuardFailure();
        };
    BeanResolver resolver = new MapBeanResolver(Map.of("testGuard", guard));
    StateMachineDefinition definition =
        new StateMachineDefinition(
            Set.of("SUBMITTED", "IN_PROGRESS"),
            List.of(
                new TransitionDescriptor(
                    "SUBMITTED", "IN_PROGRESS", "APPROVE", "testGuard", null)));
    StateMachineFactory<TestContext> factory = new StateMachineFactory<>(definition, resolver);
    NegotiatorStateMachine<TestContext> machine = factory.build("SUBMITTED", context);

    assertThrows(GuardFailure.class, () -> machine.fire("APPROVE"));
    assertEquals("SUBMITTED", machine.currentState());
  }

  @Test
  void fire_definedTransition_invokesListenerSynchronouslyAfterTransition() {
    TestContext context = new TestContext();
    List<TransitionOutcome<TestContext>> received = new ArrayList<>();
    TransitionListener<TestContext> listener = received::add;
    StateMachineDefinition definition =
        new StateMachineDefinition(
            Set.of("SUBMITTED", "IN_PROGRESS"),
            List.of(new TransitionDescriptor("SUBMITTED", "IN_PROGRESS", "APPROVE")));
    StateMachineFactory<TestContext> factory =
        new StateMachineFactory<>(definition, new MapBeanResolver(Map.of()), List.of(listener));
    NegotiatorStateMachine<TestContext> machine = factory.build("SUBMITTED", context);

    TransitionOutcome<TestContext> outcome = machine.fire("APPROVE");

    assertEquals(List.of(outcome), received);
  }

  @Test
  void fire_undefinedTransition_doesNotInvokeListener() {
    List<TransitionOutcome<TestContext>> received = new ArrayList<>();
    TransitionListener<TestContext> listener = received::add;
    StateMachineDefinition definition =
        new StateMachineDefinition(
            Set.of("SUBMITTED", "IN_PROGRESS"),
            List.of(new TransitionDescriptor("SUBMITTED", "IN_PROGRESS", "APPROVE")));
    StateMachineFactory<TestContext> factory =
        new StateMachineFactory<>(definition, new MapBeanResolver(Map.of()), List.of(listener));
    NegotiatorStateMachine<TestContext> machine = factory.build("SUBMITTED", new TestContext());

    machine.fire("DECLINE");

    assertTrue(received.isEmpty());
  }

  @Test
  void build_resolvesActionBeanAtRequestTimeNotConstructionTime() {
    List<String> resolvedNames = new ArrayList<>();
    TransitionAction<TestContext> noopAction = ctx -> {};
    BeanResolver resolver =
        new BeanResolver() {
          @Override
          public <T> T resolve(String name, Class<T> type) {
            resolvedNames.add(name);
            return type.cast(noopAction);
          }
        };
    StateMachineDefinition definition =
        new StateMachineDefinition(
            Set.of("SUBMITTED", "IN_PROGRESS"),
            List.of(new TransitionDescriptor("SUBMITTED", "IN_PROGRESS", "APPROVE", "testAction")));

    StateMachineFactory<TestContext> factory = new StateMachineFactory<>(definition, resolver);
    assertTrue(resolvedNames.isEmpty(), "resolver must not be called at factory construction time");

    factory.build("SUBMITTED", new TestContext());

    assertEquals(List.of("testAction"), resolvedNames);
  }
}
