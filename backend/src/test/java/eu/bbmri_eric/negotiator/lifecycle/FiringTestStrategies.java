package eu.bbmri_eric.negotiator.lifecycle;

import eu.bbmri_eric.negotiator.lifecycle.evaluation.Action;
import eu.bbmri_eric.negotiator.lifecycle.evaluation.Guard;
import eu.bbmri_eric.negotiator.lifecycle.evaluation.NoParams;
import eu.bbmri_eric.negotiator.lifecycle.graph.ActionContext;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardVerdict;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Guard and Action strategies that only record that they ran, and the log they write to.
 *
 * <p>Proving that a permitted Transition runs its Action chain in order needs Actions that run and
 * can be told apart. The production Actions cannot do that in a test, because the only {@code
 * PostVisibility} bean behind {@code SET_POST_VISIBILITY} throws, and {@code
 * SPAWN_RESOURCE_LIFECYCLES} throws outright.
 *
 * <p>{@link #DEFINITION_WIDE} is wired with a null {@code transition_id} and {@link
 * #TRANSITION_SCOPED} against one Transition, so the shared log shows the order in which the two
 * Guard scopes join one chain.
 *
 * <p>Action order is asserted on two wirings of the same strategy with different labels, so the
 * assertion tests {@code sort_order} and not the order in which Spring hands over beans. This also
 * takes the jsonb params through the whole path.
 *
 * <p>Test classes register this through {@code @Import} rather than component scan, so no other
 * test's application context gets these Guard types, and the boot-time duplicate type key check
 * still covers production beans only.
 */
@TestConfiguration
public class FiringTestStrategies {

  public static final String DEFINITION_WIDE = "TEST_DEFINITION_WIDE_GUARD";
  public static final String TRANSITION_SCOPED = "TEST_TRANSITION_GUARD";
  public static final String REFUSING = "TEST_REFUSING_GUARD";
  public static final String RECORDING_ACTION = "TEST_RECORDING_ACTION";

  /** The reason code {@link #REFUSING} fails with, which a refusal must carry out to the caller. */
  public static final String REFUSAL_REASON = "TEST_GUARD_SAID_NO";

  @Bean
  Recorder firingRecorder() {
    return new Recorder();
  }

  @Bean
  Guard<NoParams> definitionWideTestGuard(Recorder recorder) {
    return passingGuard(DEFINITION_WIDE, recorder);
  }

  @Bean
  Guard<NoParams> transitionScopedTestGuard(Recorder recorder) {
    return passingGuard(TRANSITION_SCOPED, recorder);
  }

  /**
   * Always fails with its own reason code. The pipeline categorises every Guard refusal as a
   * domain-state conflict, so the caller should see this reason code under the pipeline's category.
   */
  @Bean
  Guard<NoParams> refusingTestGuard(Recorder recorder) {
    return new Guard<>() {
      @Override
      public String typeKey() {
        return REFUSING;
      }

      @Override
      public Class<NoParams> paramsType() {
        return NoParams.class;
      }

      @Override
      public GuardVerdict check(EvaluationContext context, NoParams params) {
        recorder.record(REFUSING);
        return GuardVerdict.fail(REFUSAL_REASON);
      }
    };
  }

  @Bean
  Action<RecordingAction.Params> recordingTestAction(Recorder recorder) {
    return new RecordingAction(recorder);
  }

  private static Guard<NoParams> passingGuard(String typeKey, Recorder recorder) {
    return new Guard<>() {
      @Override
      public String typeKey() {
        return typeKey;
      }

      @Override
      public Class<NoParams> paramsType() {
        return NoParams.class;
      }

      @Override
      public GuardVerdict check(EvaluationContext context, NoParams params) {
        recorder.record(typeKey);
        return GuardVerdict.pass();
      }
    };
  }

  /** An Action that only writes its configured label to the log. */
  public record RecordingAction(Recorder recorder) implements Action<RecordingAction.Params> {

    /** Carries the label this wiring writes to the log, so two rows of it can be told apart. */
    public record Params(String label) {}

    @Override
    public String typeKey() {
      return RECORDING_ACTION;
    }

    @Override
    public Class<Params> paramsType() {
      return Params.class;
    }

    @Override
    public void run(ActionContext context, Params params) {
      recorder.record(params.label());
    }
  }

  /**
   * What ran, in the order it ran.
   *
   * <p>Synchronized because it is a singleton in a shared application context.
   *
   * <p>It is not a {@code @Component}, because the application's component scan walks the test
   * classpath and would put a Recorder in every context in the suite.
   */
  public static class Recorder {

    private final List<String> ran = Collections.synchronizedList(new ArrayList<>());

    void record(String what) {
      ran.add(what);
    }

    public List<String> ran() {
      synchronized (ran) {
        return List.copyOf(ran);
      }
    }

    public void clear() {
      ran.clear();
    }
  }
}
