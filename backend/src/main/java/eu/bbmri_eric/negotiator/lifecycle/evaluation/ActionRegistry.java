package eu.bbmri_eric.negotiator.lifecycle.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.lifecycle.graph.ActionCatalogue;
import eu.bbmri_eric.negotiator.lifecycle.graph.ActionContext;
import eu.bbmri_eric.negotiator.lifecycle.graph.ActionStep;
import eu.bbmri_eric.negotiator.lifecycle.graph.InvalidGraphException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

/**
 * The Action catalogue. Same fold, same collision rule, same {@link InvalidGraphException} on every
 * refusal and same single narrowing as {@link GuardRegistry}, over a separate key space.
 *
 * <p>The one thing genuinely shared rather than duplicated is {@link WiringConfigurationReader},
 * because how a {@code params} blob is read is a property of Wiring configuration and not of either
 * key space. This registry hands it its own noun, so a refusal names the Action table an
 * administrator should be looking at.
 *
 * <p>Two registries rather than one is the shape ADR 0002 already chose for the two wiring tables,
 * and the duplication is small and deliberate: sharing one keyed map would let an Action be wired
 * where a Guard belongs and have that discovered at fire time, which is exactly what compiling a
 * definition is supposed to rule out. The webhook subsystem folds one strategy list twice for two
 * purposes; this folds two lists once each, for two.
 */
@Component
public class ActionRegistry implements ActionCatalogue {

  private final WiringConfigurationReader configuration;
  private final Map<String, Action<?>> strategies;

  public ActionRegistry(ObjectMapper objectMapper, List<Action<?>> actions) {
    this.configuration = new WiringConfigurationReader("Action", objectMapper);
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
      throw new InvalidGraphException(
          "No Action strategy declares the type key '%s'. Known keys: %s."
              .formatted(typeKey, typeKeys()));
    }
    return bindTyped(strategy, paramsJson);
  }

  /** The bridge, as {@code GuardRegistry.bindTyped} — {@code Class.cast}, not an unchecked cast. */
  private <P> ActionStep bindTyped(Action<P> strategy, String paramsJson) {
    P params = configuration.read(strategy.typeKey(), strategy.paramsType(), paramsJson);
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

  private static Map<String, Action<?>> buildRegistry(List<Action<?>> actions) {
    Map<String, Action<?>> registry = new HashMap<>();
    for (Action<?> action : actions) {
      Action<?> existing = registry.putIfAbsent(action.typeKey(), action);
      if (existing != null) {
        throw new InvalidGraphException(
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
