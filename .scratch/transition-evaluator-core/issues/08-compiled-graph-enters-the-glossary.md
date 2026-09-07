# The compiled graph enters the glossary

Status: ready-for-agent

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

- [ ] `backend/CONTEXT.md` defines the compiled graph — what it is, what it is handed to, and that
      it is derived from a single Definition Version's rows.
- [ ] It defines the act of compiling one.
- [ ] Each new term carries an `_Avoid_` line, in the style of the surrounding entries.
- [ ] The entry for the compiled graph records that it is **not** a source for admin tooling: it has
      forgotten sort orders, scopes and configuration on purpose, and ADR 0002 already says the
      effective chain admin tooling needs is one query against the Wiring tables (D3).
- [ ] The terms are placed with the sections they belong to rather than appended, and the existing
      "The definition graph" and "Evaluation" entries still read coherently alongside them.
- [ ] The Java class names in the slab and the glossary terms agree. Any disagreement is reported on
      this ticket rather than resolved by renaming.
- [ ] No production code changes.
- [ ] The parity half of [parity-gate.md](../../state-machine-implementation/parity-gate.md) is
      unchanged at **255 tests in 24 classes**.

## Notes

This is sequenced last deliberately, and the trade-off is recorded so nobody re-opens it: running it
first would let the glossary drive the Java names, at the cost of a blocking documentation ticket at
the head of the slab. D13 settles the names on other grounds, so the risk this ordering carries is
small and named — a term that comes back different, on a ticket whose only job is to say so.

## Blocked by

- [01 — Adopt prototype B's tree](01-adopt-prototype-b-tree.md)
