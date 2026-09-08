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
 * The two rules about reading a Wiring row's configuration, asserted against a deliberately
 * <b>lenient</b> {@link ObjectMapper}.
 *
 * <p>The mapper this class hands the registries has {@code FAIL_ON_UNKNOWN_PROPERTIES} disabled,
 * which is the shape Spring Boot's auto-configured mapper has and therefore what production
 * injects. That is the whole point of the class: a suite that constructs a bare {@code new
 * ObjectMapper()} gets strictness for free, because the feature is enabled by Jackson's own
 * default, and so cannot tell a strict reader from a lenient one — it would pass whether or not the
 * production reader set the feature itself. The two unknown-field tests below go red the moment the
 * reader stops setting it, which is the difference this class exists to be able to see.
 */
class StrictWiringConfigurationTest {

  /**
   * Plausible-looking configuration for a Guard that asks a fixed question and reads no
   * configuration at all. It is well-formed JSON and it names nothing the Guard has ever heard of.
   */
  private static final String PARENT_STATE_OVERRIDE = "{\"requiredParentState\":\"PAUSED\"}";

  /** Boot's shape, not Jackson's: the feature the container turns off. */
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
   * A misspelled field is the failure binding configuration at compile time exists to prevent.
   * Under the injected mapper's own leniency it binds the declared type's default and nobody hears
   * about it; the reader must refuse it regardless.
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
   * A row that looks configured while its configuration goes nowhere. Silently ignoring it is how
   * an administrator comes to believe a Guard is doing something it is not, so the refusal names
   * the blob it is refusing as well as the key that cannot use it.
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
   * The Action side of the same rule, through the other registry. Both share one reader, so this is
   * asserting the sharing as much as the rule — a refusal naming the wrong table would send an
   * administrator to the wrong Wiring rows.
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
   * The four spellings of "this row configures nothing", every one of them ordinary. A strategy
   * that takes no parameters needs none, so none of these may be an error — the empty object least
   * of all, since it is what a {@code JSONB} column holds when someone writes one.
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

  /**
   * The Guard side of the unknown-field rule. There is no production Guard that takes params yet —
   * both ported ones ask a fixed question — so this is the one place the slice needs a stand-in.
   */
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
   * A JSON {@code null} is a spelling of nothing wherever it appears, so a strategy that needs
   * configuration must refuse it the way it refuses an absent column. Read as a value it binds Java
   * {@code null}, and the strategy meets it as a {@code NullPointerException} at fire time — which
   * is the failure the missing-params refusal exists to pre-empt.
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
   * Strictness must not cost the ordinary case. Walked over all three values of the scope because
   * the three-valued scope is what {@code SET_POST_VISIBILITY} exists to carry, and an enum is the
   * kind of property a reader configured feature by feature is most likely to break.
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
