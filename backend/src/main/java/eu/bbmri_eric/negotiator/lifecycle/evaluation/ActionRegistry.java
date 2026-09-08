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
 * The Action catalogue. It works like {@link GuardRegistry} over a separate key space.
 *
 * <p>The two registries stay separate on purpose. One shared map would let a Wiring name an Action
 * where a Guard belongs, and that mistake would surface at Event Firing instead of at Definition
 * Compilation.
 */
@Component
public class ActionRegistry implements ActionCatalogue {

  private final WiringConfigurationReader configurationReader;
  private final Map<String, Action<?>> strategies;

  public ActionRegistry(ObjectMapper objectMapper, List<Action<?>> actions) {
    this.configurationReader = WiringConfigurationReader.forActions(objectMapper);
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

  private <P> ActionStep bindTyped(Action<P> strategy, String paramsJson) {
    P params = configurationReader.read(strategy.typeKey(), strategy.paramsType(), paramsJson);
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
