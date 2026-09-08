package eu.bbmri_eric.negotiator.lifecycle.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.lifecycle.graph.ActionContext;
import eu.bbmri_eric.negotiator.lifecycle.graph.ActionStep;
import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledTransition;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext.Caller;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext.Subject;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardStep;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardVerdict;
import eu.bbmri_eric.negotiator.lifecycle.graph.InvalidGraphException;
import eu.bbmri_eric.negotiator.lifecycle.graph.RequiredAuthority;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The rules for reading a Wiring row's configuration, tested against a lenient {@link
 * ObjectMapper}.
 *
 * <p>Spring Boot's auto-configured mapper, which production injects, disables {@code
 * FAIL_ON_UNKNOWN_PROPERTIES}. A bare {@code new ObjectMapper()} enables it by default, so a test
 * using one would pass whether or not the production reader sets the feature itself. The
 * unknown-field tests here fail as soon as the reader stops setting it.
 */
class StrictWiringConfigurationTest {

  /** Well-formed configuration for a Guard that reads none. */
  private static final String PARENT_STATE_OVERRIDE = "{\"requiredParentState\":\"PAUSED\"}";

  /** Matches Spring Boot's mapper, not Jackson's default. */
  private static ObjectMapper lenientMapper() {
    return new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  private static ActionRegistry actionsUnderALenientMapper(Action<?>... actions) {
    return new ActionRegistry(lenientMapper(), List.of(actions));
  }

  private static GuardRegistry guardsUnderALenientMapper(Guard<?>... guards) {
    return new GuardRegistry(lenientMapper(), List.of(guards));
  }

  /**
   * A lenient mapper silently binds the declared type's default for a misspelled field, so the
   * reader must refuse the field itself.
   */
  @Test
  @DisplayName(
      "a field matching no property of the declared type is refused under a lenient mapper")
  void bind_whenAFieldMatchesNoPropertyOfTheDeclaredType_isRefusedUnderALenientMapper() {
    ActionRegistry registry =
        actionsUnderALenientMapper(new SetPostVisibilityAction(new RecordingPostVisibility()));

    assertThatThrownBy(
            () ->
                registry.bind(
                    SetPostVisibilityAction.TYPE_KEY, "{\"scope\":\"BOTH\",\"enabeld\":false}"))
        .isInstanceOf(InvalidGraphException.class)
        .hasMessageContaining(SetPostVisibilityAction.TYPE_KEY)
        .hasMessageContaining("enabeld");
  }

  /**
   * Ignoring the configuration would let an administrator believe a Guard uses it, so the refusal
   * names both the type key and the configuration.
   */
  @Test
  @DisplayName("configuration handed to a Guard that takes none is refused, naming key and blob")
  void bind_whenAGuardThatTakesNoParamsIsHandedConfiguration_isRefusedNamingKeyAndBlob() {
    GuardRegistry registry = guardsUnderALenientMapper(new NegotiationApprovedGuard());

    assertThatThrownBy(
            () -> registry.bind(NegotiationApprovedGuard.TYPE_KEY, PARENT_STATE_OVERRIDE))
        .isInstanceOf(InvalidGraphException.class)
        .hasMessageContaining(NegotiationApprovedGuard.TYPE_KEY)
        .hasMessageContaining(PARENT_STATE_OVERRIDE);
  }

  /**
   * Guards and Actions share one reader, so this also checks that the refusal names an Action. A
   * refusal naming the wrong kind would send an administrator to the wrong Wiring rows.
   */
  @Test
  @DisplayName("configuration handed to an Action that takes none is refused, naming its own kind")
  void bind_whenAnActionThatTakesNoParamsIsHandedConfiguration_isRefusedNamingItsOwnKind() {
    ActionRegistry registry = actionsUnderALenientMapper(new SpawnResourceLifecyclesAction());

    assertThatThrownBy(
            () -> registry.bind(SpawnResourceLifecyclesAction.TYPE_KEY, PARENT_STATE_OVERRIDE))
        .isInstanceOf(InvalidGraphException.class)
        .hasMessageContaining("Action")
        .hasMessageContaining(SpawnResourceLifecyclesAction.TYPE_KEY)
        .hasMessageContaining(PARENT_STATE_OVERRIDE);
  }

  /**
   * None of the four forms of empty configuration may be an error. The empty object matters most,
   * because a {@code JSONB} column holds it when someone writes one.
   */
  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "null", "{}"})
  @DisplayName("a strategy that takes no params accepts every spelling of no configuration")
  void bind_whenAStrategyTakesNoParams_acceptsEverySpellingOfNoConfiguration(String paramsJson) {
    GuardStep step =
        guardsUnderALenientMapper(new NegotiationApprovedGuard())
            .bind(NegotiationApprovedGuard.TYPE_KEY, paramsJson);

    assertThat(step.typeKey()).isEqualTo(NegotiationApprovedGuard.TYPE_KEY);
  }

  /** No production Guard takes params, so this uses a stand-in. */
  @Test
  @DisplayName("an unknown field in a Guard's configuration is refused under a lenient mapper")
  void bind_whenAFieldMatchesNoPropertyOfAGuardsDeclaredType_isRefusedUnderALenientMapper() {
    GuardRegistry registry = guardsUnderALenientMapper(new ThresholdGuard());

    assertThatThrownBy(() -> registry.bind("THRESHOLD", "{\"minimun\":3}"))
        .isInstanceOf(InvalidGraphException.class)
        .hasMessageContaining("THRESHOLD")
        .hasMessageContaining("minimun");
  }

  /**
   * Read as a value, a JSON {@code null} binds Java {@code null}, and the strategy would throw a
   * {@code NullPointerException} at fire time. The reader treats it like an absent column instead.
   */
  @Test
  @DisplayName("a strategy that needs params refuses a JSON null the way it refuses an absent one")
  void bind_whenAStrategyNeedsParamsAndTheRowSpellsNothingAsJsonNull_refusesIt() {
    GuardRegistry registry = guardsUnderALenientMapper(new ThresholdGuard());

    assertThatThrownBy(() -> registry.bind("THRESHOLD", "null"))
        .isInstanceOf(InvalidGraphException.class)
        .hasMessageContaining("THRESHOLD")
        .hasMessageContaining("carries none");
  }

  /**
   * Covers all three scope values, because an enum property is the kind a strict reader is most
   * likely to break.
   */
  @ParameterizedTest
  @CsvSource({
    "PUBLIC,true,'public=true'",
    "PRIVATE,true,'private=true'",
    "BOTH,false,'public=false,private=false'"
  })
  @DisplayName("well-formed configuration still binds, over all three values of the scope")
  void bind_whenTheConfigurationIsWellFormed_stillBindsOverAllThreeValuesOfTheScope(
      String scope, boolean enabled, String expected) {
    RecordingPostVisibility posts = new RecordingPostVisibility();
    ActionStep step =
        actionsUnderALenientMapper(new SetPostVisibilityAction(posts))
            .bind(
                SetPostVisibilityAction.TYPE_KEY,
                "{\"scope\":\"%s\",\"enabled\":%s}".formatted(scope, enabled));

    step.run(committingAbandon());

    assertThat(String.join(",", posts.calls)).isEqualTo(expected);
  }

  private static ActionContext committingAbandon() {
    return new ActionContext(
        EvaluationContext.forNegotiation(
            Caller.system(), Subject.negotiation("negotiation-1", "IN_PROGRESS", 101L), List.of()),
        new CompiledTransition("IN_PROGRESS", "ABANDON", "ABANDONED", RequiredAuthority.IS_ADMIN));
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
      return GuardVerdict.fail("BELOW_THRESHOLD", Map.of("minimum", params.minimum()));
    }
  }

  private static final class RecordingPostVisibility implements PostVisibility {

    private final List<String> calls = new ArrayList<>();

    @Override
    public void setPublicPostsEnabled(String negotiationId, boolean enabled) {
      calls.add("public=" + enabled);
    }

    @Override
    public void setPrivatePostsEnabled(String negotiationId, boolean enabled) {
      calls.add("private=" + enabled);
    }
  }
}
