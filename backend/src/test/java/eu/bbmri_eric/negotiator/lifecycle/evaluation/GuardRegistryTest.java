package eu.bbmri_eric.negotiator.lifecycle.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext.Caller;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext.Subject;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardStep;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardVerdict;
import eu.bbmri_eric.negotiator.lifecycle.graph.InvalidGraphException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The Guard catalogue: the map of strategies by type key, the rule against two strategies sharing a
 * key, and the one place a jsonb configuration becomes a typed object.
 *
 * <p>The tests build it from a hand-written list without a Spring context, which is why the
 * constructor builds the map rather than a {@code @PostConstruct} method.
 */
class GuardRegistryTest {

  private static final EvaluationContext ANY_CONTEXT =
      EvaluationContext.forNegotiation(
          Caller.person(1L, Set.of()), Subject.negotiation("negotiation-1", "OPEN", 1L), List.of());

  private final ObjectMapper objectMapper = new ObjectMapper();

  private GuardRegistry registryOf(Guard<?>... guards) {
    return new GuardRegistry(objectMapper, List.of(guards));
  }

  @Test
  @DisplayName("the catalogue reports every type key it knows")
  void typeKeys_reportsEveryKeyTheCatalogueKnows() {
    assertThat(registryOf(new ParamsFreeGuard(), new ThresholdGuard()).typeKeys())
        .containsExactly("PARAMS_FREE", "THRESHOLD");
  }

  /**
   * Strict on purpose. The notification subsystem allows several handlers per event, but two Guards
   * claiming one key are an error.
   */
  @Test
  @DisplayName("two strategies claiming one type key fail the boot, naming both classes")
  void constructor_whenTwoStrategiesDeclareOneTypeKey_throwsNamingBothClasses() {
    Guard<NoParams> duplicate =
        new Guard<>() {
          @Override
          public String typeKey() {
            return "PARAMS_FREE";
          }

          @Override
          public Class<NoParams> paramsType() {
            return NoParams.class;
          }

          @Override
          public GuardVerdict check(EvaluationContext context, NoParams params) {
            return GuardVerdict.pass();
          }
        };

    assertThatThrownBy(() -> registryOf(new ParamsFreeGuard(), duplicate))
        .isInstanceOf(InvalidGraphException.class)
        .hasMessageContaining("PARAMS_FREE")
        .hasMessageContaining(ParamsFreeGuard.class.getName())
        .hasMessageContaining(duplicate.getClass().getName());
  }

  @Test
  @DisplayName("an unknown type key is refused, and the refusal offers the keys it does know")
  void bind_whenNoStrategyDeclaresTheKey_throwsAndOffersTheKeysItKnows() {
    assertThatThrownBy(() -> registryOf(new ParamsFreeGuard()).bind("MISSPELLED", null))
        .isInstanceOf(InvalidGraphException.class)
        .hasMessageContaining("MISSPELLED")
        .hasMessageContaining("PARAMS_FREE");
  }

  @Test
  @DisplayName("a Wiring row's raw configuration is read into the strategy's declared type")
  void bind_readsTheRawJsonIntoTheStrategysDeclaredType() {
    GuardStep step = registryOf(new ThresholdGuard()).bind("THRESHOLD", "{\"minimum\":3}");

    assertThat(step.check(ANY_CONTEXT).passed()).isFalse();
    assertThat(step.check(ANY_CONTEXT).details()).containsEntry("minimum", 3);
  }

  @Test
  @DisplayName("a strategy that takes no configuration binds against a null params column")
  void bind_whenTheParamsColumnIsNullAndTheStrategyTakesNone_binds() {
    GuardStep step = registryOf(new ParamsFreeGuard()).bind("PARAMS_FREE", null);

    assertThat(step.check(ANY_CONTEXT).passed()).isTrue();
  }

  @Test
  @DisplayName("a strategy needing configuration refuses a null params column at compile time")
  void bind_whenTheParamsColumnIsNullAndTheStrategyNeedsSome_saysSoAtCompileTime() {
    assertThatThrownBy(() -> registryOf(new ThresholdGuard()).bind("THRESHOLD", null))
        .isInstanceOf(InvalidGraphException.class)
        .hasMessageContaining("THRESHOLD")
        .hasMessageContaining("carries none");
  }

  @Test
  @DisplayName(
      "configuration not fitting the declared type is refused at compile time, not at fire time")
  void bind_whenTheParamsDoNotFitTheDeclaredType_saysSoAtCompileTimeRatherThanAtFireTime() {
    assertThatThrownBy(
            () -> registryOf(new ThresholdGuard()).bind("THRESHOLD", "{\"minimum\":\"three\"}"))
        .isInstanceOf(InvalidGraphException.class)
        .hasMessageContaining("THRESHOLD")
        .hasMessageContaining("could not read its params");
  }

  /**
   * A step has only a type key and a no-argument check. Nothing about params or JSON survives
   * binding, so the evaluator's Guard stage is a plain loop over steps.
   */
  @Test
  @DisplayName("a bound step names its key and carries its configuration invisibly")
  void bind_producesAStepThatNamesItsKeyAndCarriesItsParamsInvisibly() {
    GuardStep step = registryOf(new ThresholdGuard()).bind("THRESHOLD", "{\"minimum\":7}");

    assertThat(step.typeKey()).isEqualTo("THRESHOLD");
    assertThat(step.check(ANY_CONTEXT).details()).containsEntry("minimum", 7);
  }

  private static final class ParamsFreeGuard implements Guard<NoParams> {

    @Override
    public String typeKey() {
      return "PARAMS_FREE";
    }

    @Override
    public Class<NoParams> paramsType() {
      return NoParams.class;
    }

    @Override
    public GuardVerdict check(EvaluationContext context, NoParams params) {
      return GuardVerdict.pass();
    }
  }

  /** Fails always, and echoes its params into the details so binding is observable. */
  private static final class ThresholdGuard implements Guard<ThresholdGuard.Threshold> {

    record Threshold(int minimum) {}

    @Override
    public String typeKey() {
      return "THRESHOLD";
    }

    @Override
    public Class<Threshold> paramsType() {
      return Threshold.class;
    }

    @Override
    public GuardVerdict check(EvaluationContext context, Threshold params) {
      return GuardVerdict.fail("BELOW_THRESHOLD", java.util.Map.of("minimum", params.minimum()));
    }
  }
}
