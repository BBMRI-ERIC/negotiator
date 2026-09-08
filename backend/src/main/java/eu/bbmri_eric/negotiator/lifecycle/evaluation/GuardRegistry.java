package eu.bbmri_eric.negotiator.lifecycle.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardCatalogue;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardStep;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardVerdict;
import eu.bbmri_eric.negotiator.lifecycle.graph.InvalidGraphException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

/**
 * The Guard catalogue: every {@link Guard} bean, collected once at startup and keyed by the type
 * key it declares.
 *
 * <p>The constructor throws on a duplicate key, so a collision fails the boot instead of surfacing
 * at a user's first click. Every refusal here, whether an unknown key, params that do not fit or a
 * duplicate key, is an {@link InvalidGraphException}.
 *
 * <p>{@link #bindTyped} is the one place that narrows a Wiring row's jsonb to the params type the
 * strategy declared. It uses {@code Class.cast}, so the JVM checks the narrowing at runtime.
 */
@Component
public class GuardRegistry implements GuardCatalogue {

  private final WiringConfigurationReader configurationReader;
  private final Map<String, Guard<?>> strategies;

  public GuardRegistry(ObjectMapper objectMapper, List<Guard<?>> guards) {
    this.configurationReader = WiringConfigurationReader.forGuards(objectMapper);
    this.strategies = buildRegistry(guards);
  }

  /** The keys this catalogue knows, sorted. For diagnostics and for admin tooling to offer. */
  public java.util.Set<String> typeKeys() {
    return new TreeSet<>(strategies.keySet());
  }

  @Override
  public GuardStep bind(String typeKey, String paramsJson) {
    Guard<?> strategy = strategies.get(typeKey);
    if (strategy == null) {
      throw new InvalidGraphException(
          "No Guard strategy declares the type key '%s'. Known keys: %s."
              .formatted(typeKey, typeKeys()));
    }
    return bindTyped(strategy, paramsJson);
  }

  private <P> GuardStep bindTyped(Guard<P> strategy, String paramsJson) {
    P params = configurationReader.read(strategy.typeKey(), strategy.paramsType(), paramsJson);
    return new GuardStep() {
      @Override
      public String typeKey() {
        return strategy.typeKey();
      }

      @Override
      public GuardVerdict check(EvaluationContext context) {
        return strategy.check(context, params);
      }

      @Override
      public String toString() {
        return "GuardStep[%s]".formatted(strategy.typeKey());
      }
    };
  }

  private static Map<String, Guard<?>> buildRegistry(List<Guard<?>> guards) {
    Map<String, Guard<?>> registry = new HashMap<>();
    for (Guard<?> guard : guards) {
      Guard<?> existing = registry.putIfAbsent(guard.typeKey(), guard);
      if (existing != null) {
        throw new InvalidGraphException(
            "Multiple Guard strategies configured for type key: "
                + guard.typeKey()
                + ". Found: "
                + existing.getClass().getName()
                + " and "
                + guard.getClass().getName());
      }
    }
    return Map.copyOf(registry);
  }
}
