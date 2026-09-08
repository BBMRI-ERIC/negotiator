# Strict Wiring configuration, and the no-configuration rule

Status: resolved

## Parent

[PRD — Transition Evaluator core](../PRD.md), for map ticket
[09](../../state-machine-implementation/issues/09-transition-evaluator-core.md).

## What to build

Two rules about reading a Wiring row's `params` into a strategy's declared type, both living in code
rather than in the container's ambient Jackson settings (D9).

**1. Configuration supplied to a strategy that takes none is refused**, naming the key and the blob.
Null, blank, the literal `null` and `{}` are all accepted — a strategy that takes no parameters
needs none, and an empty object is an ordinary way for a row to say so. Anything else is a row that
looks configured while its configuration goes nowhere, and silently ignoring it is how an
administrator comes to believe a Guard is doing something it is not.

**2. Unknown fields are refused.** The reader used for Wiring configuration enables
`FAIL_ON_UNKNOWN_PROPERTIES` explicitly, regardless of which `ObjectMapper` the container built.
This goes past both prototypes and is a deliberate decision: Spring Boot disables that feature by
default and this repository does not re-enable it, so **today a misspelled field in a Wiring row
binds the type's default and nobody hears about it** — the exact failure that binding configuration
at compile time exists to prevent. Wiring configuration is configuration, not user input.

Neither rule may depend on the injected mapper's leniency, and proving that is the third piece of
work. Prototype B's registry tests construct a strict `ObjectMapper` while production injects Boot's
lenient one, so its suite could not observe production behaviour in either direction — it would have
passed whether the production reader was strict or not. **One test builds a deliberately lenient
mapper and asserts both rules still hold.**

Because binding happens when the graph is compiled and not when an Event fires (D3), both refusals
are compile-time. A definition with a typo in a Wiring row never reaches a user; it fails once,
loudly, where an administrator publishing it can see it (user story 28).

## Acceptance criteria

- [x] Configuration supplied for a strategy whose params type takes none is refused, and the message
      names both the type key and the offending blob.
- [x] Null, blank, the literal `null` and `{}` are accepted for such a strategy. `{}` is matched as
      a literal after `strip()`, so `{ }` is refused — see Outcome.
- [x] A field name that no property of the declared params type matches is refused.
- [x] Both refusals raise the named exception from
      [04](04-one-named-exception-for-an-impossible-graph.md), at compile time, and both are
      structurally unreachable from the evaluator — which is why they are tested at the catalogue
      seam and not through a fired Event.
- [x] The reader enables `FAIL_ON_UNKNOWN_PROPERTIES` itself rather than relying on an injected
      mapper's configuration, and the no-configuration rule lives above the mapper rather than
      inside it.
- [x] A test constructs a deliberately **lenient** `ObjectMapper`, hands it to the registry, and
      asserts both rules still hold. Run red against the pre-slice code — the transcript is in the
      Outcome, along with the four mutations each rule was then checked against.
- [x] Well-formed configuration still binds to the declared type, including the three-valued
      post-visibility scope.
- [~] `ApplicationTest` is green — the production reader is the one under test, so the container
      folding every strategy bean with the real mapper is part of the evidence. Green, but measured
      **before** the review fixes landed; five attempts to re-run it afterwards were OOM-killed by
      the sandbox. The parity gate's 24 context boots ran after the fixes and cover the same
      property more heavily. See Verification.
- [x] The parity half of [parity-gate.md](../../state-machine-implementation/parity-gate.md) is
      unchanged at **255 tests in 24 classes**. Run, and it took two attempts — the first was
      wrecked by a concurrent build in the same worktree, recorded in the Outcome.

## Notes

**One row-level fact the strict reading must not break** (D4). There is no uniqueness constraint on
`type_key` in either Wiring table, so one Definition Version may legitimately wire the same type key
twice at different sort orders — the same Guard, configured two ways. A compiled chain is therefore a
**list** and never a map, and two steps in one chain may report the same key. A strictness change
that started deduplicating by key would break a legal definition.

Note also that the ordering column is `sort_order`, not `order`, and that `params` is `JSONB` mapped
to a Java `String` through `@JdbcTypeCode` — so what a catalogue receives is already a string, and
there is no `JsonNode` in the seam to be strict or lenient about.

## Blocked by

- [04 — One named exception for a graph that cannot exist](04-one-named-exception-for-an-impossible-graph.md)

---

## Outcome

`WiringConfigurationReader`, package-private and final in `lifecycle.evaluation`, is the one place a
Wiring row's `params` becomes a strategy's declared type. It replaces the `readParams` /
`defaultParams` pair that each registry had privately, so the two rules are stated once and both
tables are read the same way.

Both registries still take the container's `ObjectMapper` on their constructor — the shape the
sixth acceptance criterion needs — and derive the reader from it:

```java
this.configurationReader = WiringConfigurationReader.forGuards(objectMapper);
```

`objectMapper.reader().with(FAIL_ON_UNKNOWN_PROPERTIES)` rather than `objectMapper.copy()`. The
repo precedent for a purpose-configured mapper is `WebhookEventMapper`, which copies; a reader was
taken instead because what this class needs is one immutable, thread-safe reader rather than a
second whole mapper whose serialization half would go unused. Nothing depends on the choice: a
`copy()` with the feature enabled would satisfy every test here.

### Where each rule lives, and why that is the criterion

Rule 1 is decided from the declared params type and the raw string **before** either reaches
Jackson, which is what "above the mapper" buys:

- the refusal can name the type key *and* the blob, which Jackson's own `UnrecognizedPropertyException`
  message cannot; and
- it holds for the same reason under any mapper. Left to the mapper it would be an accident of
  configuration twice over — read leniently, an empty record accepts any object at all.

Rule 2 is the reader's own feature flag, so it is equally independent of what the container built.

### Three things the issue did not ask for, one of them a deviation

**A JSON literal `null` is now refused for a strategy that *does* take params.** The issue admits
the literal `null` only among the accepted spellings for the no-params case. But if `null` is a
spelling of nothing there, it is a spelling of nothing everywhere, and pre-slice it bound Java
`null` and reached the strategy as a `NullPointerException` at fire time — precisely the failure
`defaultParams`' own javadoc said the missing-params refusal existed to pre-empt. It now takes the
"carries none" branch, with its own test. **This is beyond the letter of the issue and is easy to
reverse**: delete the `JSON_NULL` clause from `carriesNothing` and its one test.

**`{}` is matched literally, so `{ }` and `{\n}` are refused.** A structural emptiness check would
accept them, at the price of parsing the blob before the rule that is supposed to sit above the
parser. It was not taken because `params` is `JSONB`, Postgres canonicalises `{ }` to `{}`, and no
other producer of these rows exists — and because the refusal explains itself: the message ends
"accepts only an absent params column, null or {}", so an administrator who somehow typed `{ }`
reads the fix. A caller synthesising the string by hand (a test, a future seed loader) is the one
context where this would surprise, and it is worth knowing before writing such a loader.

**`{}` handed to a strategy that *needs* params is unchanged**, and deliberately: it is a present
object, is read as such, and binds the declared type's defaults. Only "absent" was extended.

### Review feedback, applied

- **The class javadoc argued with alternatives it had rejected**, which is the one thing this
  effort's standing convention forbids beside the code — [04](04-one-named-exception-for-an-impossible-graph.md)'s
  Outcome settled it as "a decision not to do something is recorded in the slice's issue file, never
  in a comment beside the code". Two paragraphs went: the `reader()`-versus-`copy()` defence and the
  "left to the mapper, the rule would be an accident of configuration twice over" defence. Both are
  above, in this file, which is where they belong. What stayed is the fact no code shows — that Boot
  disables `FAIL_ON_UNKNOWN_PROPERTIES` and this repository does not re-enable it. The same shape,
  weaker, came out of both registries: the constructor already shows that the reader is shared and
  that each side names its own kind.
- **The strategy noun was a bare `String` at both call sites** — Primitive Obsession, and the one
  thing its own javadoc said mattered was that a Guard row must not be refused with a message about
  the Action table. It is now two named factories, `forGuards` and `forActions`, over a private
  constructor, so the two spellings exist once each and cannot be mistyped.
- **`NoOpPostVisibility` duplicated `RecordingPostVisibility` inside one test class** for no gain.
  Deleted; the recording one serves both.
- **The field named a reader after what it reads** — `configuration` became `configurationReader`.
- **One refusal message split a sentence across the `.formatted` boundary**, so the second half was
  concatenated raw onto a formatted first half. It is one parenthesised literal formatted once.
- **The test class javadoc claimed the opposite of its point.** "Every assertion here would pass
  against a bare mapper whether or not the production reader was strict at all" reads as a confession
  that the class is vacuous; what is true is that a suite built on a bare mapper would. Rewritten to
  say which two tests go red when the reader stops setting the feature.
- **Suppressed:** `ThresholdGuard` is now declared in both `StrictWiringConfigurationTest` and
  `GuardRegistryTest`, and `RecordingPostVisibility` in both this class and `LifecycleStrategiesTest`.
  Real duplication, left alone: every test class in this package is deliberately self-contained —
  `GuardRegistryTest`'s javadoc makes a point of "no Spring context anywhere" and hand-built lists —
  and a shared fixture holder would couple three classes to buy about twenty lines. There is no
  production Guard that takes params (both ported ones ask a fixed question), which is the only
  reason a stand-in is needed at all; the duplication should go when a real one arrives.
- **Not done:** `GuardRegistryTest` still builds a bare `new ObjectMapper()`. That class's subject is
  the fold and the collision rule, and none of its assertions turn on leniency. The lenient mapper
  the issue asked for is one test class, not a sweep.

### Verification

`StrictWiringConfigurationTest` is new — 12 tests, every one against a mapper with
`FAIL_ON_UNKNOWN_PROPERTIES` explicitly **disabled**. No test was deleted and no assertion weakened.

The redness the sixth criterion demands, witnessed rather than asserted. The first test written was
the Action-side unknown-field refusal, run against the pre-slice tree:

```
Tests run: 1, Failures: 1
bind_whenAFieldMatchesNoPropertyOfTheDeclaredType_isRefusedUnderALenientMapper
java.lang.AssertionError:
Expecting code to raise a throwable.
```

Not a wrong message or a wrong type — **nothing was thrown at all**, because the lenient mapper bound
`Params[scope=BOTH, enabled=false]` from `{"scope":"BOTH","enabeld":false}` and reported nothing.
That is the failure the issue describes, reproduced.

Each of the four rules was then broken in turn against the finished suite, because a rule with no
test that dies for it is the vacuity this effort has been bitten by before:

| Rule broken | Tests that went red |
|---|---|
| `.with(FAIL_ON_UNKNOWN_PROPERTIES)` dropped, inheriting the injected mapper | 2 |
| The no-params branch accepts unconditionally | 2 |
| `JSON_NULL` dropped from `carriesNothing` | 2 |
| `EMPTY_JSON_OBJECT` dropped from the accepted set | 1 |

**223 tests green across `lifecycle/**`** in 20 classes — both registries, the compiler, the compiled
graph, the evaluator, the strategies, the exception, plus the four architecture guard tests, which are
the ones that would notice a new class in these packages. `EvaluatorPurityGuardTest` passes unchanged:
the new class names no repository and no persistence package, and it does not reach into `definition`.

**`ApplicationTest` is the one criterion not fully met, and this is the honest shape of it.** It ran
green against a real context boot — the production reader constructed from Boot's own mapper, for
every strategy bean — but that run happened *before* the review fixes were applied. Afterwards it
could not be re-run: **five attempts were OOM-killed by the sandbox**, the last two with
`-Djacoco.skip=true` and `-DargLine=-Xmx1024m`, every one killed before the Spring context emitted
a line. The surefire output file exists and is empty. Nothing about the slice caused this; the
machine had ~11 GB free and no JVMs running at each attempt, and a Testcontainers-backed boot simply
stopped fitting partway through the session.

Two things make the gap narrow rather than open:

- **The parity gate ran after the review fixes and boots the real application context across 24
  characterization classes**, with the real `ObjectMapper` and every real strategy bean, and is
  exact. That is heavier evidence for "the container folds every strategy bean with the real mapper"
  than one context boot. A strict reader that broke the boot produces 255 errors, not 0 — which is
  exactly what a broken boot looked like in attempt 1 below.
- **The fixes applied after the green run cannot reach the boot.** Both registry constructors keep
  their `(ObjectMapper, List<...>)` signatures; what changed was javadoc, a private-constructor-plus-
  static-factory refactor of a class the registries already constructed, a field rename, one string
  concatenation, and test-only edits.

Whoever picks up the next slice should re-run `ApplicationTest` once the sandbox allows it. It is
expected green and nothing in this slice's behaviour depends on the difference.

The parity half is **unchanged at 24 classes, 255 tests, 0 failures, 0 errors, 1 skipped** — exact.
The one skip is `dump.LifecycleGraphDumpGeneratorTest`, as the gate specifies.

**It took two attempts, and the first failure is worth recording because it was not a parity
movement and not the stale-target trap either.** Attempt 1 ran on a freshly cleaned target and
returned 24 classes, 255 tests, 1 skipped — discovery exact — with **204 errors and 0 failures**.
Every error was a context load failing on `class path resource [....Test.class] cannot be opened
because it does not exist`, and the class files were present again afterwards. The cause: a
concurrent Maven build in the same worktree, started by a review sub-agent while the gate was
running, which wiped and rebuilt `backend/target` underneath the forked JVMs. Attempt 2, run with
nothing else touching the worktree, was green and exact.

The lesson generalises past this slice, and past sub-agents: **`backend/target` is single-writer, and
the gate's own advice to read a red gate for its failure shape is what catches this.** 0 failures
with a three-figure error count, all of them at context load, is never a behaviour change — it is the
build directory being pulled out from under the run. The gate should be the only Maven process in a
worktree while it runs.
