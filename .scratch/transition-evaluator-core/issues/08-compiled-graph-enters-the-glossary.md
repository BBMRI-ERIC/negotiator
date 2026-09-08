# The compiled graph enters the glossary

Status: resolved

## Parent

[PRD — Transition Evaluator core](../PRD.md), for map ticket
[09](../../state-machine-implementation/issues/09-transition-evaluator-core.md).

## What to build

`backend/CONTEXT.md` has **no term for the compiled graph at all**, and none for the act of
producing one. Its "The definition graph" section defines the States, Events and Transitions *of a
Definition Version* — the relational rows. Nothing names the evaluator-ready value compiled from
those rows, and nothing names the compiling. Ticket 13's own instructions noticed the gap while
creating the thing, hedging it as "the 'compilation' (might need another word)".

By the time this slice runs, the whole slab is written in a vocabulary that exists only in Java
class names. Two terms plus their `_Avoid_` lines close that: the value the evaluator is handed, and
the step that produces it. This is `/domain-modeling`'s job rather than a naming preference, because
the map's binding constraints put glossary terms there — invoke that skill and let it place the
terms in the section they belong to.

The Java names follow the glossary, not the reverse. In practice D13 has already settled them —
prototype B's naming set is adopted whole, over prototype A's alternatives — so the expected outcome
is that the glossary ratifies the names the code already uses. If `/domain-modeling` lands on a
different word for either term, say so on this ticket before renaming anything: a rename across the
slab is a decision, not a tidy-up.

## Acceptance criteria

- [x] `backend/CONTEXT.md` defines the compiled graph — what it is, what it is handed to, and that
      it is derived from a single Definition Version's rows.
- [x] It defines the act of compiling one.
- [x] Each new term carries an `_Avoid_` line, in the style of the surrounding entries.
- [~] The entry for the compiled graph records that it is **not** a source for admin tooling: it has
      forgotten sort orders, scopes and configuration on purpose, and ADR 0002 already says the
      effective chain admin tooling needs is one query against the Wiring tables (D3).
- [x] The terms are placed with the sections they belong to rather than appended, and the existing
      "The definition graph" and "Evaluation" entries still read coherently alongside them.
- [x] The Java class names in the slab and the glossary terms agree. Any disagreement is reported on
      this ticket rather than resolved by renaming.
- [x] No production code changes.
- [x] The parity half of [parity-gate.md](../../state-machine-implementation/parity-gate.md) is
      unchanged at **255 tests in 24 classes**.

## Notes

This is sequenced last deliberately, and the trade-off is recorded so nobody re-opens it: running it
first would let the glossary drive the Java names, at the cost of a blocking documentation ticket at
the head of the slab. D13 settles the names on other grounds, so the risk this ordering carries is
small and named — a term that comes back different, on a ticket whose only job is to say so.

## Blocked by

- [01 — Adopt prototype B's tree](01-adopt-prototype-b-tree.md)

## Outcome

Two terms, eight added lines, one file. `backend/CONTEXT.md` gains **Compiled Graph** and
**Definition Compilation** at the end of the "The definition graph" section, and nothing else in
the repository changed.

The expected outcome is the one that happened: `/domain-modeling` ratified the names the code
already uses. No rename is proposed and none is needed.

### Why "Definition Compilation" rather than "Compilation"

The act needed a name that `DefinitionCompiler` is recognisably the agent of, and the glossary
already had the pattern: `DefinitionResolver` sits under **Definition Resolution** two sections
down. So the same shape one section up, and the reader who knows one knows the other. Bare
"Compilation" would have agreed with the class name just as well and carried less; the qualifier is
what says *which* thing is compiled, in a file where "Definition" is a loaded word.

Nothing in the slab is renamed by either term. `CompiledGraph`, `CompiledTransition` and
`CompiledGraphCache` all sit under **Compiled Graph**; `DefinitionCompiler` under **Definition
Compilation**; `DefinitionVersionRows` is the "rows of one Definition Version" the second entry
names. `RequiredAuthority` moved into the graph package back in `df270bdc` and the glossary already
had **Required Authority** in this same section, so that one was already agreed.

**The ADRs had got there first**, which is the strongest evidence the names are right and was not
noticed until the entries were written. ADR 0001 already says "The compiled graph is cached in
memory per Definition Version"; ADR 0003 already says "the evaluator's compiled-graph cache key".
The glossary was the only place the word was missing.

### Placement, and why both went in one section

Both terms are in "The definition graph", not "Evaluation". A Compiled Graph is a *form* of the
definition graph: every part of it — States, Events, Transitions, Guards, Actions, Wiring, Required
Authority — is defined in that section and nothing in it comes from Evaluation. Definition
Compilation belongs beside it for the same reason and one more: it is not a step of the **Evaluation
Pipeline**, which is Required Authority, then the information-requirement check, then Guards.
Filing compilation under "Evaluation" would have implied it runs per evaluation, which is the exact
opposite of the property that entry exists to record.

Within the section they go last, after every part they are made of — an entry that says Wiring has
been folded into per-Transition chains cannot precede **Wiring**. That also puts them immediately
before **Transition Evaluator**, whose "reads no data of its own" is the sentence a reader wants
**Compiled Graph** in hand for. The existing entries are untouched: no incoherence needed fixing,
because **Transition Evaluator** says the evaluator answers what a Definition Version permits and
**Compiled Graph** says it is handed that version's compiled form.

### One criterion met in substance, not to the letter

The admin-tooling criterion names "ADR 0002 ... (D3)". **The entry records the substance and not
the citation.** `backend/CONTEXT.md` contains no ADR reference anywhere — no numbers, no links, in
148 lines — and `CONTEXT-MAP.md` is explicit that the glossary and the decisions are separate
places, one `CONTEXT.md` and one `docs/adr/` per context. A first citation in the glossary is a new
convention in that file, and this ticket is not the place to start one.

What the entry does say is that the Compiled Graph is never a source for admin tooling, that it has
forgotten sort orders, the two Guard scopes and each Guard's configuration on purpose, and that the
effective chain an admin has to be shown is one query against the Wiring rows instead — D3's claim,
in D3's words, pointed at a term the glossary already defines. Flagged here rather than resolved
silently, because reading the criterion strictly would want the number printed.

### The `_Avoid_` lines, and two words that needed thought

**Compiled Graph** avoids *state machine, machine instance, in-memory definition*. "machine
instance" is the one doing real work: a Compiled Graph is per Definition Version and shared by every
Lifecycle pinned to it, so "instance" is precisely the wrong instinct, and it is the instinct a
cache invites.

**Definition Compilation** avoids *loading, hydration, parsing*.

- *loading* is on the line even though ADR 0001 uses "loading the definition graph" for a real and
  adjacent step — loading the rows, which `DefinitionVersionRows` exists to keep separate. The line
  means "not this word for *this* concept", which is the established reading here: **Required
  Authority** avoids "guard" while **Guard** is a term of its own two entries up. The entry's own
  second sentence says loading is a separate step, so the distinction is stated and not merely
  banned.
- *parsing* replaced a first draft's *graph build*, which was wrong. `CompiledGraph.builder(...)` is
  the general way any graph is assembled — by compiling rows, by a test, or one day by a definition
  file — so building is not a synonym for compiling, it is the mechanism compiling goes through, and
  rejecting the word would have read as rejecting the class. "parsing" carries the intended
  distinction (reading jsonb params is *part* of compiling, not what compiling is) with nothing to
  collide with.

### Terms considered and not added

`CompiledTransition`, `GuardStep` and `ActionStep` have no entries of their own. Each is the
compiled form of a term the section already defines, and **Compiled Graph** says the graph carries
Guards and Actions "as the chain that will actually run", which is the whole of what a reader needs.
`CompiledGraphCache` has none either: a cache is a general programming concept, and the glossary
format rules exclude those however much the project leans on them. Three more entries would have
made the section longer without making the model sharper.

### What the two review axes changed

Both axes landed on the same paragraph from opposite directions, which is the useful part. The Spec
axis called the admin-tooling sentence **required**; the Standards axis called it a **hard
violation** of the format rule that `CONTEXT.md` be "totally devoid of implementation details … a
glossary and nothing else", since it re-records in the glossary a decision ADR 0002 already holds.
Both are right, and the criterion wins: the ticket is the contract, and it is the ticket — not a
preference — that asked for the sentence. What the Standards axis did win is everything in the entry
that the criterion did **not** ask for.

Three fixes landed.

- **"indexed so that every question it answers is a lookup" is gone.** A data-structure and
  performance property, not what the term *is*. `CompiledGraph`'s javadoc is the right home for it
  and already says it. Cutting it also took the entry from 106 words to 98.
- **"it has forgotten … each Guard's configuration" was factually wrong.** Sort orders and the two
  Guard scopes really are gone, but `guardCatalogue.bind(typeKey, params)` closes the params *into*
  the step — the configuration is applied and un-inspectable, not absent. Now: "each Guard's
  configuration is bound into it rather than readable off it", which is both accurate and the
  stronger reason it is no good to admin tooling.
- **`materialized graph` joined the `_Avoid_` line**, and this one is a genuine conflict worth
  naming rather than a tidy-up. **ADR 0001 uses both words for the same value, one paragraph
  apart**: "handed an already-materialized definition graph", then "The compiled graph is cached in
  memory per Definition Version". The drift is already in the settled record and an ADR cannot be
  edited to remove it. `CONTEXT-MAP.md` makes `backend/CONTEXT.md` "the authority on those terms",
  with binding `_Avoid_` lines, so the glossary is the thing that gets to settle it — on "compiled",
  which is the word the code, ADR 0003 and now the glossary all use. Recorded here because
  `docs/agents/domain.md` asks that an ADR conflict be surfaced rather than silently overridden: a
  reader of ADR 0001 will meet a word the glossary rejects, and that is deliberate.

**Declined, with the reason.** The Standards axis also flagged "compiling happens once per version
rather than once per evaluation" as ADR 0001 territory. It stays: it is the whole content of the
term. An act that happened once per evaluation would be a step of the **Evaluation Pipeline**, and
distinguishing those two is what the entry is *for*. Its rationale tail was trimmed to one clause.

The Spec axis added one fact that hardens the citation decision rather than softening it: **"D3" is
not a label ADR 0002 carries.** That ADR's decisions are unnumbered bold paragraphs; D3 is a label
from the decision map. Printing "(D3)" would have cited something the ADR does not have, in a file
that cites nothing.

### Bookkeeping, and one stale doc

The acceptance criteria are ticked, which the first pass forgot — every resolved sibling ticks
theirs. The admin-tooling criterion carries `- [~]` and an inline note, following
[05](05-unknown-current-state-refused-everywhere.md)'s convention for a criterion met in substance
but not to the letter.

`docs/agents/triage-labels.md` lists five role strings and `resolved` is not among them, though six
resolved siblings in this directory use it and `issue-tracker.md`'s wayfinding section defines it.
The doc is stale, not this commit. Left alone — amending the repo's triage vocabulary is not this
ticket's business.

### Gates

**The parity half is unchanged at 24 classes, 255 tests, 0 failures, 0 errors, 1 skipped — exact.**
The one skip is `dump.LifecycleGraphDumpGeneratorTest`, as the gate specifies. Numbers summed from
the 24 `TEST-*.xml` reports rather than off a summary line or the `.txt` writer, per the gate's own
warning; no report exists for `IntendedDeltasAdr0005WillInvertTest`, which is how the
`-DexcludedGroups` split is verified. A `clean` ran first, so no stale report could have been
counted.

Running it at all is belt-and-braces: the diff is two Markdown files and touches nothing under
`backend/src`, so the test tree is byte-identical to the tip slice 07 measured. It was run anyway,
because "no Java changed" is a claim about a diff and the gate is a measurement.

**The Nix shell works again, and is required again.** Slice 07 recorded `nix develop` failing with
"setting up a private mount namespace: Operation not permitted" and `mvn` being on `PATH` without
it. Both have flipped back: `nix develop .#opencode --command` succeeds, and a bare `mvn` or `java`
is command-not-found here (nothing Maven-shaped on `PATH` at all). So the prefix that
[parity-gate.md](../../state-machine-implementation/parity-gate.md) specifies is once more the only
way to run the gate. Its advice — try the prefix first, drop it if it fails — held up in both
directions, which is the reason to leave the note as it is rather than rewrite it for whichever
state is current.

No formatter run: the fmt plugin formats Java, and no Java changed.
