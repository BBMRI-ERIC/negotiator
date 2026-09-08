package eu.bbmri_eric.negotiator.lifecycle.definition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledGraph;
import eu.bbmri_eric.negotiator.lifecycle.graph.InvalidGraphException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongFunction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The compiled-graph cache, with no database, no Spring and no real producer. */
class CompiledGraphCacheTest {

  private static final long VERSION_ID = 42L;

  /** A second version, standing in for work pinned to it that a publish must not disturb. */
  private static final long PINNED_VERSION_ID = 7L;

  /**
   * How long a blocked production waits for the caller it is watching for. Generous, because it is
   * only ever waited out when the cache is broken; and bounded, because a cache that does hold a
   * lock must fail this suite rather than hang it.
   */
  private static final long A_RE_ENTRY_SECONDS = 5;

  /**
   * Longer than {@link #A_RE_ENTRY_SECONDS}, so a blocked production times out before its caller.
   */
  private static final long A_WHOLE_TEST_SECONDS = 30;

  private final CountingProducer producer = new CountingProducer();
  private final CompiledGraphCache cache = new CompiledGraphCache(producer);

  @Test
  @DisplayName("a Definition Version is produced once, however often its graph is asked for")
  void graphFor_producesOncePerVersion() {
    CompiledGraph first = cache.graphFor(VERSION_ID);
    CompiledGraph second = cache.graphFor(VERSION_ID);
    CompiledGraph third = cache.graphFor(VERSION_ID);

    assertThat(producer.productionsOf(VERSION_ID)).isEqualTo(1);
    assertThat(second).isSameAs(first);
    assertThat(third).isSameAs(first);
  }

  /**
   * The criterion that is easy to write and easy to fake: two threads calling a cache twice pass
   * against a {@code computeIfAbsent} as happily as against this one. So the producer itself
   * observes the re-entry — the first production blocks until a second caller has demonstrably
   * reached the producer — and the test can only pass if no lock is held across production.
   *
   * <p>Both callers ask for the <em>same</em> version deliberately. Two different ids usually land
   * in different bins of a {@code ConcurrentHashMap}, so a producer running under the map's own
   * per-bin lock would not serialize them and the test would prove nothing.
   *
   * <p>Producing one version twice is the accepted cost of that, not a defect: compilation is pure
   * and idempotent, so a race duplicates work rather than producing a wrong answer, which is the
   * right trade against holding a lock across the six queries the real producer will issue.
   */
  @Test
  @DisplayName("no lock is held across production: a producer can watch a second caller arrive")
  void graphFor_holdsNoLockWhileAGraphIsProduced() throws Exception {
    CountDownLatch aSecondCallerReachedTheProducer = new CountDownLatch(1);
    AtomicInteger productions = new AtomicInteger();
    AtomicBoolean firstProductionSawTheSecondCaller = new AtomicBoolean();

    CompiledGraphCache blockingProducer =
        new CompiledGraphCache(
            id -> {
              if (productions.incrementAndGet() == 1) {
                firstProductionSawTheSecondCaller.set(awaited(aSecondCallerReachedTheProducer));
              } else {
                aSecondCallerReachedTheProducer.countDown();
              }
              return graphOf(id);
            });

    ExecutorService callers = Executors.newFixedThreadPool(2);
    try {
      Future<CompiledGraph> first = callers.submit(() -> blockingProducer.graphFor(VERSION_ID));
      Future<CompiledGraph> second = callers.submit(() -> blockingProducer.graphFor(VERSION_ID));

      assertThat(first.get(A_WHOLE_TEST_SECONDS, TimeUnit.SECONDS).definitionVersionId())
          .isEqualTo(VERSION_ID);
      assertThat(second.get(A_WHOLE_TEST_SECONDS, TimeUnit.SECONDS).definitionVersionId())
          .isEqualTo(VERSION_ID);
    } finally {
      callers.shutdownNow();
    }

    assertThat(firstProductionSawTheSecondCaller)
        .withFailMessage(
            """
            The first production never saw a second caller reach the producer. A cache that \
            produces inside the map's lock makes the second caller wait for the first, which is \
            exactly what this asserts against.""")
        .isTrue();
    assertThat(productions).hasValue(2);
  }

  @Test
  @DisplayName("publishing invalidates one version's entry and leaves every other pinned one")
  void invalidate_evictsThatVersionAndNoOther() {
    cache.graphFor(VERSION_ID);
    cache.graphFor(PINNED_VERSION_ID);

    cache.invalidate(VERSION_ID);
    cache.graphFor(VERSION_ID);
    cache.graphFor(PINNED_VERSION_ID);

    assertThat(producer.productionsOf(VERSION_ID)).isEqualTo(2);
    assertThat(producer.productionsOf(PINNED_VERSION_ID)).isEqualTo(1);
  }

  @Test
  @DisplayName("invalidating a version that was never asked for changes nothing")
  void invalidate_isHarmlessForAVersionNeverCached() {
    cache.invalidate(VERSION_ID);

    assertThat(cache.graphFor(VERSION_ID).definitionVersionId()).isEqualTo(VERSION_ID);
    assertThat(producer.productionsOf(VERSION_ID)).isEqualTo(1);
  }

  @Test
  @DisplayName("the named graph exception propagates out of the cache unwrapped")
  void graphFor_propagatesTheNamedGraphExceptionUnwrapped() {
    CompiledGraphCache corrupt =
        new CompiledGraphCache(
            id -> {
              throw new InvalidGraphException(
                  "Definition Version %d declares no initial State".formatted(id));
            });

    assertThatThrownBy(() -> corrupt.graphFor(VERSION_ID))
        .isExactlyInstanceOf(InvalidGraphException.class)
        .hasMessage("Definition Version 42 declares no initial State");
  }

  @Test
  @DisplayName("a failed production is not cached: the next request retries and is served")
  void graphFor_retriesAfterAFailedProduction() {
    AtomicInteger attempts = new AtomicInteger();
    CompiledGraphCache fixedAfterAFailedCompile =
        new CompiledGraphCache(
            id -> {
              if (attempts.incrementAndGet() == 1) {
                throw new InvalidGraphException(
                    "Definition Version %d declares two initial States".formatted(id));
              }
              return graphOf(id);
            });

    assertThatThrownBy(() -> fixedAfterAFailedCompile.graphFor(VERSION_ID))
        .isInstanceOf(InvalidGraphException.class);

    assertThat(fixedAfterAFailedCompile.graphFor(VERSION_ID).definitionVersionId())
        .isEqualTo(VERSION_ID);
    assertThat(attempts.get()).isEqualTo(2);
  }

  /**
   * The one thing the key being a row id cannot itself guarantee. A producer asked for one version
   * that reads another's rows compiles a graph that is internally consistent — the compiler's own
   * version-mixing refusal sees nothing wrong with it — and only the cache knows which version was
   * asked for. Filing it under the id asked for would serve one version's graph to work pinned to
   * another, which is the failure the Definition Version Pin exists to prevent.
   */
  @Test
  @DisplayName("a graph that is not the version asked for is refused rather than filed under it")
  void graphFor_refusesAGraphThatIsNotTheVersionAskedFor() {
    CompiledGraphCache misreadingProducer =
        new CompiledGraphCache(id -> graphOf(PINNED_VERSION_ID));

    assertThatThrownBy(() -> misreadingProducer.graphFor(VERSION_ID))
        .isExactlyInstanceOf(InvalidGraphException.class)
        .hasMessageContaining(String.valueOf(VERSION_ID))
        .hasMessageContaining(String.valueOf(PINNED_VERSION_ID));

    assertThatThrownBy(() -> misreadingProducer.graphFor(VERSION_ID))
        .withFailMessage("The refused graph was cached, so the next caller is served it silently.")
        .isInstanceOf(InvalidGraphException.class);
  }

  /**
   * Identity, asserted against the class rather than against a call. Every other test here passes a
   * row id because that is the only thing the cache accepts, which is precisely what a reader
   * cannot tell from a green call — so the key type is stated once, here, where a method taking a
   * family key alongside a version number would break it.
   *
   * <p>The rule reads the cache's callable surface and not its private helpers, which take whatever
   * their one caller has already keyed. A composite key can only arrive as something a caller can
   * reach, and every one of those is covered however it is spelled.
   */
  @Test
  @DisplayName("every question the cache answers is keyed on the Definition Version's row id alone")
  void theCache_isKeyedOnTheRowIdAlone() {
    List<Method> questions =
        Arrays.stream(CompiledGraphCache.class.getDeclaredMethods())
            .filter(method -> !method.isSynthetic())
            .filter(method -> !Modifier.isPrivate(method.getModifiers()))
            .toList();

    assertThat(questions)
        .withFailMessage("Found no methods on the cache at all; this rule would pass vacuously.")
        .isNotEmpty();
    assertThat(questions)
        .allSatisfy(
            method ->
                assertThat(method.getParameterTypes())
                    .withFailMessage(
                        """
                        %s takes %s. A Definition Version is identified by its row id alone: a \
                        method taking a family key and a version number together reintroduces the \
                        composite lookup that identity decision removed.""",
                        method.getName(), Arrays.toString(method.getParameterTypes()))
                    .containsExactly(long.class));
  }

  @Test
  @DisplayName(
      "the cache is package-private, so nothing outside the definition package can call it")
  void theCache_isPackagePrivate() {
    assertThat(Modifier.isPublic(CompiledGraphCache.class.getModifiers()))
        .withFailMessage(
            """
            The cache is public. Nothing in production may resolve a graph in this slice: the \
            producer that would load one reads the definition tables, and that read is the \
            cutover's to make, not this slab's.""")
        .isFalse();
    assertThat(CompiledGraphCache.class.getDeclaredMethods())
        .allSatisfy(method -> assertThat(Modifier.isPublic(method.getModifiers())).isFalse());
  }

  /**
   * Stands in for the producer the cutover slab will pass: six queries and a compile. It counts
   * what it was asked to produce, which is the only way the cache's whole point — that a version is
   * produced once — is observable from outside it.
   */
  private static final class CountingProducer implements LongFunction<CompiledGraph> {

    private final Map<Long, AtomicInteger> productions = new ConcurrentHashMap<>();

    @Override
    public CompiledGraph apply(long definitionVersionId) {
      productions.computeIfAbsent(definitionVersionId, id -> new AtomicInteger()).incrementAndGet();
      return graphOf(definitionVersionId);
    }

    int productionsOf(long definitionVersionId) {
      AtomicInteger count = productions.get(definitionVersionId);
      return count == null ? 0 : count.get();
    }
  }

  private static CompiledGraph graphOf(long definitionVersionId) {
    return CompiledGraph.builder(definitionVersionId).initialState("DRAFT").build();
  }

  /** Whether the latch fired before the wait ran out. An interrupt counts as it did not. */
  private static boolean awaited(CountDownLatch latch) {
    try {
      return latch.await(A_RE_ENTRY_SECONDS, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }
}
