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
 * Guard and Action strategies that exist only to be observed, and the log they write to.
 *
 * <p><b>Why test strategies rather than the real ones.</b> Proving that a permitted move runs its
 * Action chain <em>in order</em> needs Actions that run and can be told apart, and today there are
 * none: {@code SET_POST_VISIBILITY} reaches a {@code PostVisibility} port whose only bean throws,
 * and {@code SPAWN_RESOURCE_LIFECYCLES} throws outright because its body belongs to the coupling
 * slab. Implementing either to get a test green would be building a slab's work inside another
 * slab's test. These record and do nothing else, which is all the chain's ordering needs.
 *
 * <p><b>Two type keys for the two Guard scopes.</b> {@link #DEFINITION_WIDE} is wired with a null
 * {@code transition_id} and {@link #TRANSITION_SCOPED} against one Transition, so the log they
 * share shows the order the two scopes were folded into one chain — the one thing about Wiring that
 * no single row can state about itself.
 *
 * <p><b>One Action type, two wirings, distinguished by params.</b> Ordering is asserted from two
 * rows of the <em>same</em> strategy carrying different labels, which makes the assertion about
 * {@code sort_order} rather than about which of two beans Spring happened to hand over first. It
 * also takes the jsonb params binding through the whole path, which no other integration test does.
 *
 * <p>Registered through {@code @Import} on the test class rather than as components, so no other
 * test's application context grows a Guard type it never asked for — and so the duplicate-key check
 * that fails the boot stays a statement about production beans.
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
   * Fails every time, with a reason code of its own. A Guard's refusal is always categorised as a
   * domain-state conflict whatever it says, so what this proves out to the caller is that the
   * reason code survives while the category is the pipeline's to assign.
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

  /** An Action whose whole behaviour is to write its configured label to the log. */
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
   * <p>Synchronized because it is a singleton of a shared application context; the assertions are
   * single-threaded, but a bean that could be written from two contexts at once should not be the
   * thing a failure is blamed on.
   *
   * <p>Deliberately not a {@code @Component}: the test tree is on the classpath the application's
   * component scan walks, so an annotation here would put a second Recorder in every context in the
   * suite. It is a bean of this configuration only, and this configuration is imported by hand.
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
