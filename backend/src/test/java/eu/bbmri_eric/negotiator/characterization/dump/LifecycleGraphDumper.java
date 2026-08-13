package eu.bbmri_eric.negotiator.characterization.dump;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.guard.Guard;
import org.springframework.statemachine.security.SecurityRule;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.Transition;
import org.springframework.statemachine.trigger.Trigger;

/**
 * Walks a live Spring Statemachine bean and renders its Definition graph as canonical JSON, plus a
 * Mermaid diagram rendered from that JSON.
 *
 * <p>This is the <strong>only</strong> component of the characterization suite permitted to touch
 * Spring Statemachine internals. It is a deliberate, sanctioned exception: it is a throwaway that
 * is deleted at cutover, and it is a generator rather than a parity test.
 *
 * <p>Guard and Action identity is recovered by reflective unwrap. Spring Statemachine 4.0 hands
 * back {@code Function<StateContext, Mono<...>>} lambdas rather than the configured {@link Guard}
 * and {@link Action} beans, so the underlying bean is recovered from the lambda's captured field.
 * If any unwrap fails the dumper <strong>throws</strong> and nothing is written: a partially
 * faithful dump must never ship, because faithfulness is the artifact's entire value.
 *
 * <p>Output is byte-reproducible. Every map is a {@link LinkedHashMap} written in a fixed key
 * order, every collection is sorted by a total order, and the pretty printer pins two-space
 * indentation and a {@code \n} line separator so the artifact does not vary by platform.
 */
final class LifecycleGraphDumper {

  private static final String LINE_SEPARATOR = "\n";
  private static final String SSM_PACKAGE_PREFIX = "org.springframework.statemachine.";
  private static final int MAX_UNWRAP_DEPTH_NODES = 256;

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private LifecycleGraphDumper() {}

  /**
   * Renders the graph of one state machine bean as canonical JSON.
   *
   * @param graphName the graph's short name, used as the {@code graph} field
   * @param beanName the Spring bean name the machine is registered under
   * @param stateMachine the live bean to walk
   * @return canonical JSON, newline-terminated
   */
  static String toCanonicalJson(
      String graphName, String beanName, StateMachine<String, String> stateMachine) {
    Map<String, Object> graph = new LinkedHashMap<>();
    graph.put("graph", graphName);
    graph.put("beanName", beanName);
    graph.put("initialState", stateId(stateMachine.getInitialState()));
    graph.put("states", sortedStateIds(stateMachine.getStates()));

    List<Map<String, Object>> transitions = new ArrayList<>();
    for (Transition<String, String> transition : stateMachine.getTransitions()) {
      transitions.add(describeTransition(transition));
    }
    transitions.sort(TRANSITION_ORDER);

    graph.put("events", eventsOf(transitions));
    graph.put("transitionCount", transitions.size());
    graph.put("transitions", transitions);

    try {
      return canonicalWriter().writeValueAsString(graph) + LINE_SEPARATOR;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to serialise the " + graphName + " graph dump", e);
    }
  }

  /**
   * Renders a Mermaid state diagram of both graphs from their JSON dumps.
   *
   * <p>Deliberately reads the JSON rather than the beans, so the diagram can only ever describe
   * what the committed JSON says.
   */
  static String toMermaid(String negotiationJson, String resourceJson) {
    JsonNode negotiation = parse(negotiationJson);
    JsonNode resource = parse(resourceJson);

    StringBuilder mermaid = new StringBuilder();
    mermaid.append("%% Generated from negotiation-graph-v1.json and resource-graph-v1.json.");
    mermaid.append(LINE_SEPARATOR);
    mermaid.append("%% Do not edit by hand - see LifecycleGraphDumpGeneratorTest.");
    mermaid.append(LINE_SEPARATOR);
    mermaid.append("stateDiagram-v2").append(LINE_SEPARATOR);
    appendMermaidGraph(mermaid, negotiation, "N", "Negotiation Lifecycle");
    appendMermaidGraph(mermaid, resource, "R", "Resource Lifecycle");
    return mermaid.toString();
  }

  private static void appendMermaidGraph(
      StringBuilder mermaid, JsonNode graph, String prefix, String label) {
    mermaid.append("  state \"").append(label).append("\" as ").append(prefix).append(" {");
    mermaid.append(LINE_SEPARATOR);

    for (JsonNode state : graph.get("states")) {
      String id = state.asText();
      mermaid
          .append("    state \"")
          .append(id)
          .append("\" as ")
          .append(prefix)
          .append('_')
          .append(id)
          .append(LINE_SEPARATOR);
    }

    JsonNode initial = graph.get("initialState");
    if (!initial.isNull()) {
      mermaid
          .append("    [*] --> ")
          .append(prefix)
          .append('_')
          .append(initial.asText())
          .append(LINE_SEPARATOR);
    }

    for (JsonNode transition : graph.get("transitions")) {
      JsonNode source = transition.get("source");
      JsonNode target = transition.get("target");
      if (source.isNull() || target.isNull()) {
        mermaid
            .append("    %% unattached transition fragment: ")
            .append(mermaidTransitionLabel(transition))
            .append(" (source=")
            .append(source.isNull() ? "none" : source.asText())
            .append(", target=")
            .append(target.isNull() ? "none" : target.asText())
            .append(')')
            .append(LINE_SEPARATOR);
        continue;
      }
      mermaid
          .append("    ")
          .append(prefix)
          .append('_')
          .append(source.asText())
          .append(" --> ")
          .append(prefix)
          .append('_')
          .append(target.asText())
          .append(" : ")
          .append(mermaidTransitionLabel(transition))
          .append(LINE_SEPARATOR);
    }

    mermaid.append("  }").append(LINE_SEPARATOR);
  }

  private static String mermaidTransitionLabel(JsonNode transition) {
    List<String> parts = new ArrayList<>();
    JsonNode event = transition.get("event");
    parts.add(event.isNull() ? "no event" : event.asText());

    JsonNode securityRule = transition.get("securityRule");
    if (!securityRule.isNull()) {
      StringBuilder secured = new StringBuilder("secured ");
      List<String> attributes = new ArrayList<>();
      securityRule.get("attributes").forEach(attribute -> attributes.add(attribute.asText()));
      secured.append(String.join(",", attributes));
      JsonNode comparisonType = securityRule.get("comparisonType");
      if (!comparisonType.isNull()) {
        secured.append(" (").append(comparisonType.asText()).append(')');
      }
      parts.add(secured.toString());
    }

    JsonNode guard = transition.get("guard");
    if (!guard.isNull()) {
      parts.add("guard " + guard.asText());
    }

    JsonNode actions = transition.get("actions");
    if (actions.size() > 0) {
      List<String> names = new ArrayList<>();
      actions.forEach(action -> names.add(action.asText()));
      parts.add("action " + String.join(",", names));
    }

    return String.join(" | ", parts);
  }

  private static Map<String, Object> describeTransition(Transition<String, String> transition) {
    Map<String, Object> described = new LinkedHashMap<>();
    described.put("source", stateId(transition.getSource()));
    described.put("event", eventOf(transition.getTrigger()));
    described.put("target", stateId(transition.getTarget()));
    described.put("kind", transition.getKind() == null ? null : transition.getKind().name());
    described.put("securityRule", describeSecurityRule(transition.getSecurityRule()));
    described.put("guard", guardBeanName(transition));
    described.put("actions", actionBeanNames(transition));
    return described;
  }

  private static Map<String, Object> describeSecurityRule(SecurityRule securityRule) {
    if (securityRule == null) {
      return null;
    }
    Map<String, Object> described = new LinkedHashMap<>();
    Collection<String> attributes = securityRule.getAttributes();
    described.put(
        "attributes", attributes == null ? List.of() : new TreeSet<>(attributes).stream().toList());
    described.put(
        "comparisonType",
        securityRule.getComparisonType() == null ? null : securityRule.getComparisonType().name());
    described.put("expression", securityRule.getExpression());
    return described;
  }

  private static String guardBeanName(Transition<String, String> transition) {
    Object guardFunction = transition.getGuard();
    if (guardFunction == null) {
      return null;
    }
    return unwrapBeanSimpleName(guardFunction, Guard.class, "guard", describeForError(transition));
  }

  /**
   * Recovers the simple class name of the {@link Guard} bean captured inside a Spring Statemachine
   * wrapper lambda. Exposed so the unwrap can be exercised directly: the only Guard in the system
   * turns out to sit on no Transition, so walking the beans never reaches this path.
   */
  static String unwrapGuardBeanName(Object guardFunction) {
    return unwrapBeanSimpleName(guardFunction, Guard.class, "guard", "<not a transition>");
  }

  /** Recovers the simple class name of the {@link Action} bean captured inside a wrapper lambda. */
  static String unwrapActionBeanName(Object actionFunction) {
    return unwrapBeanSimpleName(actionFunction, Action.class, "action", "<not a transition>");
  }

  private static List<String> actionBeanNames(Transition<String, String> transition) {
    Collection<?> actionFunctions = transition.getActions();
    if (actionFunctions == null || actionFunctions.isEmpty()) {
      return List.of();
    }
    List<String> names = new ArrayList<>();
    for (Object actionFunction : actionFunctions) {
      if (actionFunction == null) {
        throw new IllegalStateException(
            "Null action on transition " + describeForError(transition) + " - refusing to dump");
      }
      names.add(
          unwrapBeanSimpleName(
              actionFunction, Action.class, "action", describeForError(transition)));
    }
    Collections.sort(names);
    return names;
  }

  /**
   * Recovers the simple class name of the {@link Guard} or {@link Action} bean captured inside a
   * Spring Statemachine wrapper lambda.
   *
   * @throws IllegalStateException if the bean cannot be recovered. Degrading to {@code "unknown"}
   *     would produce a dump that looks faithful and is not.
   */
  private static String unwrapBeanSimpleName(
      Object wrapper, Class<?> beanType, String kind, String transitionDescription) {
    Object bean = findCapturedBean(wrapper, beanType);
    if (bean == null) {
      throw new IllegalStateException(
          "Could not reflectively unwrap the "
              + kind
              + " bean from "
              + wrapper.getClass().getName()
              + " on transition "
              + transitionDescription
              + ". A partially faithful dump must never be written, so nothing was generated.");
    }
    return bean.getClass().getSimpleName();
  }

  private static Object findCapturedBean(Object root, Class<?> beanType) {
    Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    Deque<Object> pending = new ArrayDeque<>();
    seen.add(root);
    pending.add(root);
    Object statemachineInternalCandidate = null;
    int examined = 0;

    while (!pending.isEmpty() && examined++ < MAX_UNWRAP_DEPTH_NODES) {
      Object current = pending.poll();
      for (Field field : current.getClass().getDeclaredFields()) {
        if (Modifier.isStatic(field.getModifiers())) {
          continue;
        }
        Object value;
        try {
          field.setAccessible(true);
          value = field.get(current);
        } catch (RuntimeException | ReflectiveOperationException e) {
          continue;
        }
        if (value == null || !seen.add(value)) {
          continue;
        }
        if (beanType.isInstance(value)) {
          if (!value.getClass().getName().startsWith(SSM_PACKAGE_PREFIX)) {
            return value;
          }
          if (statemachineInternalCandidate == null) {
            statemachineInternalCandidate = value;
          }
        }
        if (worthDescendingInto(value)) {
          pending.add(value);
        }
      }
    }
    return statemachineInternalCandidate;
  }

  private static boolean worthDescendingInto(Object value) {
    Class<?> type = value.getClass();
    if (type.isArray() || type.isEnum()) {
      return false;
    }
    if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean) {
      return false;
    }
    return type.isSynthetic() || type.getName().startsWith(SSM_PACKAGE_PREFIX);
  }

  private static String describeForError(Transition<String, String> transition) {
    return stateId(transition.getSource())
        + " --("
        + eventOf(transition.getTrigger())
        + ")--> "
        + stateId(transition.getTarget());
  }

  private static String stateId(State<String, String> state) {
    return state == null ? null : state.getId();
  }

  private static String eventOf(Trigger<String, String> trigger) {
    return trigger == null ? null : trigger.getEvent();
  }

  private static List<String> sortedStateIds(Collection<State<String, String>> states) {
    Set<String> ids = new TreeSet<>();
    for (State<String, String> state : states) {
      if (state.getId() != null) {
        ids.add(state.getId());
      }
    }
    return List.copyOf(ids);
  }

  private static List<String> eventsOf(List<Map<String, Object>> transitions) {
    Set<String> events = new TreeSet<>();
    for (Map<String, Object> transition : transitions) {
      Object event = transition.get("event");
      if (event != null) {
        events.add((String) event);
      }
    }
    return List.copyOf(events);
  }

  private static JsonNode parse(String json) {
    try {
      return MAPPER.readTree(json);
    } catch (Exception e) {
      throw new IllegalStateException("Generated graph dump is not readable JSON", e);
    }
  }

  private static ObjectWriter canonicalWriter() {
    DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
    DefaultIndenter indenter = new DefaultIndenter("  ", LINE_SEPARATOR);
    printer.indentObjectsWith(indenter);
    printer.indentArraysWith(indenter);
    return MAPPER.writer(printer);
  }

  private static final Comparator<Map<String, Object>> TRANSITION_ORDER =
      Comparator.comparing(
              (Map<String, Object> transition) -> (String) transition.get("source"),
              Comparator.nullsFirst(Comparator.naturalOrder()))
          .thenComparing(
              transition -> (String) transition.get("event"),
              Comparator.nullsFirst(Comparator.naturalOrder()))
          .thenComparing(
              transition -> (String) transition.get("target"),
              Comparator.nullsFirst(Comparator.naturalOrder()))
          .thenComparing(
              transition -> (String) transition.get("kind"),
              Comparator.nullsFirst(Comparator.naturalOrder()));
}
