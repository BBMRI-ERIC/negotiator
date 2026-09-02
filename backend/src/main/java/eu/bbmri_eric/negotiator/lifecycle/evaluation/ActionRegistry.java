package eu.bbmri_eric.negotiator.lifecycle.evaluation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.lifecycle.graph.ActionCatalogue;
import eu.bbmri_eric.negotiator.lifecycle.graph.ActionContext;
import eu.bbmri_eric.negotiator.lifecycle.graph.ActionStep;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

/**
 * The Action catalogue. Same fold, same collision rule and same single narrowing as {@link
 * GuardRegistry}, over a separate key space.
 *
 * <p>Two registries rather than one is the shape ADR 0002 already chose for the two wiring tables,
 * and the duplication is small and deliberate: sharing one keyed map would let an Action be wired
 * where a Guard belongs and have that discovered at fire time, which is exactly what compiling a
 * definition is supposed to rule out. The webhook subsystem folds one strategy list twice for two
 * purposes; this folds two lists once each, for two.
 */
@Component
public class ActionRegistry implements ActionCatalogue {

  private final ObjectMapper objectMapper;
  private final Map<String, Action<?>> strategies;

  public ActionRegistry(ObjectMapper objectMapper, List<Action<?>> actions) {
    this.objectMapper = objectMapper;
    this.strategies = buildRegistry(actions);
  }

  /** The keys this catalogue knows, sorted. For diagnostics and for admin tooling to offer. */
  public Set<String> typeKeys() {
    return new TreeSet<>(strategies.keySet());
  }

  @Override
  public ActionStep bind(String typeKey, String paramsJson) {
    Action<?> strategy = strategies.get(typeKey);
    if (strategy == null) {
      throw new IllegalArgumentException(
          "No Action strategy declares the type key '%s'. Known keys: %s."
              .formatted(typeKey, typeKeys()));
    }
    return bindTyped(strategy, paramsJson);
  }

  /** The bridge, as {@code GuardRegistry.bindTyped} — {@code Class.cast}, not an unchecked cast. */
  private <P> ActionStep bindTyped(Action<P> strategy, String paramsJson) {
    P params = readParams(strategy, paramsJson);
    return new ActionStep() {
      @Override
      public String typeKey() {
        return strategy.typeKey();
      }

      @Override
      public void run(ActionContext context) {
        strategy.run(context, params);
      }

      @Override
      public String toString() {
        return "ActionStep[%s]".formatted(strategy.typeKey());
      }
    };
  }

  private <P> P readParams(Action<P> strategy, String paramsJson) {
    if (paramsJson == null || paramsJson.isBlank()) {
      return strategy.paramsType().cast(defaultParams(strategy));
    }
    try {
      return objectMapper.readValue(paramsJson, strategy.paramsType());
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(
          "Action '%s' could not read its params as %s: %s"
              .formatted(strategy.typeKey(), strategy.paramsType().getSimpleName(), paramsJson),
          e);
    }
  }

  private static Object defaultParams(Action<?> strategy) {
    if (strategy.paramsType() == NoParams.class) {
      return NoParams.INSTANCE;
    }
    throw new IllegalArgumentException(
        "Action '%s' declares params of type %s but its Wiring row carries none."
            .formatted(strategy.typeKey(), strategy.paramsType().getSimpleName()));
  }

  private static Map<String, Action<?>> buildRegistry(List<Action<?>> actions) {
    Map<String, Action<?>> registry = new HashMap<>();
    for (Action<?> action : actions) {
      Action<?> existing = registry.putIfAbsent(action.typeKey(), action);
      if (existing != null) {
        throw new IllegalStateException(
            "Multiple Action strategies configured for type key: "
                + action.typeKey()
                + ". Found: "
                + existing.getClass().getName()
                + " and "
                + action.getClass().getName());
      }
    }
    return Map.copyOf(registry);
  }
}
