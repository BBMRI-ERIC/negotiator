package eu.bbmri_eric.negotiator.lifecycle.definition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

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

/** The Compiled Graph cache, with no database, no Spring and no real producer. */
class CompiledGraphCacheTest {

  private static final long VERSION_ID = 42L;

  /** A second version, for work pinned to it that a publish must not disturb. */
  private static final long PINNED_VERSION_ID = 7L;

  /**
   * How long a blocked production waits for the caller it is watching for. The wait only runs out
   * when the cache is broken, so it can be long. It is bounded so that a cache holding a lock fails
   * this suite instead of hanging it.
   */
  private static final long A_RE_ENTRY_SECONDS = 5;

  /**
   * How long a caller's own wait runs before the test gives up on it. Longer than {@link
   * #A_RE_ENTRY_SECONDS}, so a blocked production always times out before the caller waiting on it
   * does, and a broken cache reports the rule it broke rather than a bare timeout.
   */
  private static final long A_STUCK_CALL_SECONDS = 30;

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
   * Two threads calling a cache twice pass against a {@code computeIfAbsent} as well as against
   * this cache, so the producer observes the re-entry itself. The first production blocks until a
   * second caller reaches the producer, and the test passes only if the cache holds no lock across
   * production.
   *
   * <p>Both callers ask for the same version. Two different ids usually land in different bins of
   * a {@code ConcurrentHashMap}, so a producer running under the map's per-bin lock would not
   * serialize them and the test would prove nothing.
   *
   * <p>Compiling one version twice is an accepted cost, for the reason given on {@link
   * CompiledGraphCache#graphFor}.
   */
  @Test
  @DisplayName("no lock is held across production: a producer can watch a second caller arrive")
  void graphFor_holdsNoLockWhileAGraphIsProduced() throws Exception {
    CountDownLatch aSecondCallerReachedTheProducer = new CountDownLatch(1);
    AtomicInteger productions = new AtomicInteger();
    AtomicBoolean firstProductionSawTheSecondCaller = new AtomicBoolean();

    CompiledGraphCache cacheOverABlockingProducer =
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
      Future<CompiledGraph> first =
          callers.submit(() -> cacheOverABlockingProducer.graphFor(VERSION_ID));
      Future<CompiledGraph> second =
          callers.submit(() -> cacheOverABlockingProducer.graphFor(VERSION_ID));

      assertThat(first.get(A_STUCK_CALL_SECONDS, TimeUnit.SECONDS).definitionVersionId())
          .isEqualTo(VERSION_ID);
      assertThat(second.get(A_STUCK_CALL_SECONDS, TimeUnit.SECONDS).definitionVersionId())
          .isEqualTo(VERSION_ID);
    } finally {
      callers.shutdownNow();
    }

    assertThat(firstProductionSawTheSecondCaller)
        .withFailMessage(
            """
            The first production never saw a second caller reach the producer. A cache that \
            produces inside the map's lock makes the second caller wait for the first.""")
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
    CompiledGraphCache cacheOverACorruptVersion =
        new CompiledGraphCache(
            id -> {
              throw new InvalidGraphException(
                  "Definition Version %d declares no initial State".formatted(id));
            });

    assertThatThrownBy(() -> cacheOverACorruptVersion.graphFor(VERSION_ID))
        .isExactlyInstanceOf(InvalidGraphException.class)
        .hasMessage("Definition Version 42 declares no initial State");
  }

  @Test
  @DisplayName("a failed production is not cached: the next request retries and is served")
  void graphFor_retriesAfterAFailedProduction() {
    AtomicInteger attempts = new AtomicInteger();
    CompiledGraphCache cacheOverAFixedDefinition =
        new CompiledGraphCache(
            id -> {
              if (attempts.incrementAndGet() == 1) {
                throw new InvalidGraphException(
                    "Definition Version %d declares two initial States".formatted(id));
              }
              return graphOf(id);
            });

    assertThatThrownBy(() -> cacheOverAFixedDefinition.graphFor(VERSION_ID))
        .isInstanceOf(InvalidGraphException.class);

    assertThat(cacheOverAFixedDefinition.graphFor(VERSION_ID).definitionVersionId())
        .isEqualTo(VERSION_ID);
    assertThat(attempts.get()).isEqualTo(2);
  }

  /**
   * A row-id key cannot rule out this corruption by itself, for the reason given on {@link
   * CompiledGraphCache}. The refusal names both ids, the version asked for and the version
   * produced, because the report says nothing else.
   */
  @Test
  @DisplayName("a graph that is not the version asked for is refused rather than filed under it")
  void graphFor_refusesAGraphThatIsNotTheVersionAskedFor() {
    CompiledGraphCache cacheOverAMisreadingProducer =
        new CompiledGraphCache(id -> graphOf(PINNED_VERSION_ID));

    assertThatThrownBy(() -> cacheOverAMisreadingProducer.graphFor(VERSION_ID))
        .isExactlyInstanceOf(InvalidGraphException.class)
        .hasMessageContaining(String.valueOf(VERSION_ID))
        .hasMessageContaining(String.valueOf(PINNED_VERSION_ID));

    Throwable secondRequest =
        catchThrowable(() -> cacheOverAMisreadingProducer.graphFor(VERSION_ID));

    assertThat(secondRequest)
        .withFailMessage(
            "The refused graph was cached, so the next caller is served it silently instead.")
        .isInstanceOf(InvalidGraphException.class);
  }

  /**
   * Asserts the key type against the class, not against a call. A passing call cannot show that
   * the cache accepts only a row id, so this test fails if any method takes a family key alongside
   * a version number.
   *
   * <p>The rule checks the non-private methods only. Private helpers take whatever their caller has
   * already keyed, so a composite key can only arrive through a method a caller can reach.
   *
   * <p>Constructors get a weaker rule, because one takes the producer: no family key or Version
   * sequence may be among their parameters. A cache given a family key at construction is keyed on
   * it just as much as one taking it per call, and {@code getDeclaredMethods} would not see it.
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
                        method taking a family key and a version number together brings back a \
                        composite lookup.""",
                        method.getName(), Arrays.toString(method.getParameterTypes()))
                    .containsExactly(long.class));

    assertThat(CompiledGraphCache.class.getDeclaredConstructors())
        .isNotEmpty()
        .allSatisfy(
            constructor ->
                assertThat(constructor.getParameterTypes())
                    .withFailMessage(
                        """
                        The constructor takes %s. A family key or a Version sequence given to the \
                        cache at construction keys it on a composite identity instead of the row \
                        id.""",
                        Arrays.toString(constructor.getParameterTypes()))
                    .doesNotContain(String.class, int.class, Integer.class));
  }

  @Test
  @DisplayName(
      "the cache is package-private, so nothing outside the definition package can call it")
  void theCache_isPackagePrivate() {
    assertThat(Modifier.isPublic(CompiledGraphCache.class.getModifiers()))
        .withFailMessage(
            """
            The cache is public. Only tests may resolve a graph, because the producer reads \
            the definition tables and no production path reads them.""")
        .isFalse();
    assertThat(CompiledGraphCache.class.getDeclaredMethods())
        .allSatisfy(method -> assertThat(Modifier.isPublic(method.getModifiers())).isFalse());
  }

  /**
   * Stands in for the real producer, which runs six queries and a compile. It counts what it was
   * asked to produce, the only way to observe from outside that the cache produces a version once.
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

  /** Whether the latch fired before the wait ran out. An interrupt counts as not fired. */
  private static boolean awaited(CountDownLatch latch) {
    try {
      return latch.await(A_RE_ENTRY_SECONDS, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }
}
