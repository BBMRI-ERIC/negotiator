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
 * The Guard catalogue: every {@link Guard} bean, folded once at startup and keyed by the type key
 * it declares for itself.
 *
 * <p>The fold is the one the webhook subsystem already uses — a {@code private static} pure method
 * called from the constructor, {@code putIfAbsent}, a throw from <em>inside the constructor</em> so
 * a duplicate key is a bean creation failure and therefore a failed boot, a message naming the key
 * and both colliding classes, and {@code Map.copyOf} to freeze. Two things differ from that
 * precedent and are worth naming: the key is a {@code String} rather than a {@code Class}, which is
 * new to this backend, and the strict "any duplicate is fatal" rule is a choice rather than a
 * default — the notification subsystem folds the same shape into a multimap with no collision rule
 * at all, because several handlers per event are legal there. Two Guards claiming one key are not.
 *
 * <p>Every refusal here is an {@link InvalidGraphException} — unknown key, params that do not fit,
 * and the duplicate-key collision alike; that type's javadoc says why the three are one. Naming the
 * collision did not move it: it is still thrown from the constructor, so a duplicate key still
 * fails the boot rather than surfacing at a user's first click.
 *
 * <p>It also owns the subsystem's <b>one unchecked narrowing, in one place</b>: {@link #bindTyped}
 * is a private generic bridge that carries a Wiring row's jsonb through {@link
 * WiringConfigurationReader} and back as whatever type the strategy declared. Everything either
 * side of it is type-safe, and no other class needs a {@code @SuppressWarnings}.
 *
 * <p>Reading that jsonb is the reader's work rather than this class's, and strictly the reader's:
 * both registries share it so that a Wiring row is read the same way whichever table it came from,
 * and so that neither inherits the container mapper's leniency.
 */
@Component
public class GuardRegistry implements GuardCatalogue {

  private final WiringConfigurationReader configuration;
  private final Map<String, Guard<?>> strategies;

  public GuardRegistry(ObjectMapper objectMapper, List<Guard<?>> guards) {
    this.configuration = new WiringConfigurationReader("Guard", objectMapper);
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

  /**
   * The bridge. {@code Class.cast} rather than an unchecked cast, so the narrowing is checked at
   * runtime by the same {@link Class} object the strategy declared — which is exactly the idiom
   * {@code WebhookEventMapper.mapWithStrategy} already uses for the same reason.
   */
  private <P> GuardStep bindTyped(Guard<P> strategy, String paramsJson) {
    P params = configuration.read(strategy.typeKey(), strategy.paramsType(), paramsJson);
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
