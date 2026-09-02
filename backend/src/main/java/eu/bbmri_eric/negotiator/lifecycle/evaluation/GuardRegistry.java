package eu.bbmri_eric.negotiator.lifecycle.evaluation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardCatalogue;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardStep;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardVerdict;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

/**
 * The Guard catalogue: every {@link Guard} bean, folded once at startup and keyed by the type key
 * it declares for itself.
 *
 * <p>The fold is the one the webhook subsystem already uses — a {@code private static} pure method
 * called from the constructor, {@code putIfAbsent}, an {@link IllegalStateException} thrown
 * <em>from the constructor</em> so a duplicate key is a bean creation failure and therefore a
 * failed boot, a message naming the key and both colliding classes, and {@code Map.copyOf} to
 * freeze. Two things differ from that precedent and are worth naming: the key is a {@code String}
 * rather than a {@code Class}, which is new to this backend, and the strict "any duplicate is
 * fatal" rule is a choice rather than a default — the notification subsystem folds the same shape
 * into a multimap with no collision rule at all, because several handlers per event are legal
 * there. Two Guards claiming one key are not.
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
      throw new IllegalArgumentException(
          "No Guard strategy declares the type key '%s'. Known keys: %s."
              .formatted(typeKey, typeKeys()));
    }
    return bindTyped(strategy, paramsJson);
  }

  /**
   * The bridge. {@code Class.cast} rather than an unchecked cast, so the narrowing is checked at
   * runtime by the same {@link Class} object the strategy declared — which is exactly the idiom
   * {@code WebhookEventMapper.mapWithStrategy} already uses for the same reason.
   */
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
      throw new IllegalArgumentException(
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
    throw new IllegalArgumentException(
        "Guard '%s' declares params of type %s but its Wiring row carries none."
            .formatted(strategy.typeKey(), strategy.paramsType().getSimpleName()));
  }

  private static Map<String, Guard<?>> buildRegistry(List<Guard<?>> guards) {
    Map<String, Guard<?>> registry = new HashMap<>();
    for (Guard<?> guard : guards) {
      Guard<?> existing = registry.putIfAbsent(guard.typeKey(), guard);
      if (existing != null) {
        throw new IllegalStateException(
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
