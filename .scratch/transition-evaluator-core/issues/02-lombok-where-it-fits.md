# Lombok where it fits, with the three carve-outs

Status: ready-for-agent

## Parent

[PRD — Transition Evaluator core](../PRD.md), for map ticket
[09](../../state-machine-implementation/issues/09-transition-evaluator-core.md).

## What to build

Bring the adopted tree into house style. Lombok is not an optional flourish in this backend — 220 of
437 production files use it, at `provided` scope, and slab 08's six definition entities already
carry a fixed annotation set. New code that hand-writes what Lombok generates reads as a second
project inside the same package tree, which is the whole of why this slice exists (D11).

Two mechanical substitutions:

- **`@NonNull` replaces the hand-written null checks** on the graph's records — the evaluation
  context and its nested types, the compiled Transition, the action context, the verdict, and the
  graph builder. There is precedent for Lombok `@NonNull` on a record in this backend already.
- **`@RequiredArgsConstructor`** on the two registries and the compiler.

And three carve-outs, each of which must survive with its reason recorded in the code, because each
one looks like an oversight to the next reader:

1. **The graph's builder stays hand-written.** Lombok's `@Builder` generates one setter per field
   and calls a constructor; this builder is a different thing. Declaring a State as initial or
   terminal also declares it, declaring a Transition also declares its Event, and `build()` runs
   three validations and constructs two indexes. A generated builder would take the derived sets and
   both indexes *from the caller*, which is precisely the work this builder exists to remove — a
   fixture is one line per Transition and everything else is derived (user story 47).
2. **The Transition Evaluator's constructor stays hand-written.** Map ticket 09 requires that having
   no repository, `EntityManager` or Spring Data dependency "should be visible in its constructor".
   A generated constructor gives a reader nothing to look at.
3. **`@Nullable` is not adopted in `graph`.** Lombok has no such annotation and this backend's is
   `org.springframework.lang.Nullable` — a real runtime Spring dependency, and a far weaker case for
   the import allowlist than Lombok's source-retained annotations. Optional components stay
   documented in javadoc.

## Acceptance criteria

- [ ] Every hand-written null check on the graph's records is a Lombok `@NonNull`, and the
      behaviour a caller sees on a null argument is unchanged.
- [ ] The two registries and the compiler use `@RequiredArgsConstructor`.
- [ ] The graph builder and the evaluator constructor are untouched, and each carries a short
      comment saying why Lombok is deliberately absent there.
- [ ] No `@Nullable` annotation is introduced anywhere in `graph`, and `graph` still imports nothing
      but `java.` and `lombok.`.
- [ ] The formatter check passes and no test assertion changes.
- [ ] The parity half of [parity-gate.md](../../state-machine-implementation/parity-gate.md) is
      unchanged at **255 tests in 24 classes**.

## Notes

**One count in the PRD does not match the tree, and the slice should settle it.** D11 says 18
hand-written null checks and enumerates where: the evaluation context and its nested types (8), the
compiled Transition (4), the action context (2), the verdict (1) and the graph builder (3). The tree
has **21** — the sealed outcome type carries three more that D11's enumeration does not mention.
Convert those three as well if they are ordinary argument checks; if either refused half of the
outcome is load-bearing in a way `@NonNull` would change, leave it and say so.

**This slice and the import allowlist rule interact, and safely.** Adding `lombok.` imports to
`graph` is legal under the allowlist only because D18 writes it as `java.` **and** `lombok.` from the
start. Lombok's annotations are `SOURCE` retention at `provided` scope, so nothing Lombok reaches the
runtime classpath and the property the purity gate protects stays literally true of the compiled
output. Whichever of the two slices lands second must not have to weaken the other.

## Blocked by

- [01 — Adopt prototype B's tree](01-adopt-prototype-b-tree.md)
