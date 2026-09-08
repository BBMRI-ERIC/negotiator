package eu.bbmri_eric.negotiator.lifecycle.evaluation;

import com.fasterxml.jackson.core.JsonProcessingException;
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
 * <p>Every refusal here is an {@link InvalidGraphException} — unknown key, params that do not fit,
 * and the duplicate-key collision alike; that type's javadoc says why the three are one. Naming the
 * collision did not move it: it is still thrown from the constructor, so a duplicate key still
 * fails the boot rather than surfacing at a user's first click.
 *
 * <p>It also owns the subsystem's <b>one unchecked narrowing, in one place</b>: {@link #bindTyped}
 * is a private generic bridge that reads a Wiring row's jsonb into whatever type the strategy
 * declared. Everything either side of it is type-safe, and no other class needs a
 * {@code @SuppressWarnings}.
 */
@Component
public class GuardRegistry implements GuardCatalogue {

  private final ObjectMapper objectMapper;
  private final Map<String, Guard<?>> strategies;

  public GuardRegistry(ObjectMapper objectMapper, List<Guard<?>> guards) {
    this.objectMapper = objectMapper;
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
    P params = readParams(strategy, paramsJson);
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

  private <P> P readParams(Guard<P> strategy, String paramsJson) {
    if (paramsJson == null || paramsJson.isBlank()) {
      return strategy.paramsType().cast(defaultParams(strategy));
    }
    try {
      return objectMapper.readValue(paramsJson, strategy.paramsType());
    } catch (JsonProcessingException e) {
      throw new InvalidGraphException(
          "Guard '%s' could not read its params as %s: %s"
              .formatted(strategy.typeKey(), strategy.paramsType().getSimpleName(), paramsJson),
          e);
    }
  }

  /**
   * A null {@code params} column is legal and ordinary, so it must not be an error. For a strategy
   * that takes none it means {@link NoParams#INSTANCE}; for one that does take params it is a
   * definition that is missing configuration, and saying so here beats a {@code
   * NullPointerException} inside the strategy at fire time.
   */
  private static Object defaultParams(Guard<?> strategy) {
    if (strategy.paramsType() == NoParams.class) {
      return NoParams.INSTANCE;
    }
    throw new InvalidGraphException(
        "Guard '%s' declares params of type %s but its Wiring row carries none."
            .formatted(strategy.typeKey(), strategy.paramsType().getSimpleName()));
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
