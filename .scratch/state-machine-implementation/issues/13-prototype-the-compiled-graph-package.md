# Prototype the compiled-graph package

Type: prototype
Status: open
Blocked by: 08

## Question

Prototype [09 Transition Evaluator core](09-transition-evaluator-core.md) to later inform the PRD.
Code first.

## Instructions

- Create a new graph package. It should own the not concretely defined, but in several documents
  mentioned "compiled-graph" the evaluator evaluates on.
- The graph is not assembled by JPA entities, it has its own types.
- `RequiredAuthority` moves to the graph package.
- The "compilation" (might need another word) should happen in the definition package.
- `DefinitionScope` stays in the definition package.

For decisions not covered here, stick close to issue 9 and the ADRs.

While implementing the prototype, judgement calls may need to be made. All of them should be
recorded in a separate artifact for further analysis.

Use the `/codebase-design` skill in planning.

Do everything in a local git worktree, and don't check out other worktrees or workspaces.

Plan first, then implement.

## Prior art — read before starting, do not re-derive

- **[recon-strategies.md](../transition-evaluator-core/recon-strategies.md)** *(on branch
  `slice-01-vocabulary-move`, not on this one)* — the Guard and Action inventory with `file:line`
  citations, five places issue 09 describes the code wrongly, and six divergences with owners.
- **[before-picture-findings.md](../before-picture-findings.md)** parts 3 and 7.
- ADRs [0001](../../../backend/docs/adr/0001-hand-written-lifecycle-subsystem.md),
  [0002](../../../backend/docs/adr/0002-lifecycle-definitions-are-relational-configuration.md),
  [0003](../../../backend/docs/adr/0003-definition-versioning-and-identity.md),
  [0005](../../../backend/docs/adr/0005-information-requirements-gate-transitions-as-a-built-in-stage.md).

## This supersedes a package layout already chosen elsewhere

Branch `slice-01-vocabulary-move` is three commits ahead of `feat/state-machine-implementation` and
has already begun slab 09 on a different layout — a 474-line PRD, ten commit-sized slices, and one
landed commit moving **both** `DefinitionScope` **and** `RequiredAuthority` up to
`eu.bbmri_eric.negotiator.lifecycle`, with the evaluator in a new `lifecycle.evaluation` and no graph
package. **None of it is merged.** The instructions above contradict it on two of the five points and
add a package it does not have; they are the later word.

Left open for whoever claims this: whether that branch's slice 01 is rebased, redone or abandoned.
Note also that it amended issue 09's own text with a `## Progress` section and five corrections —
those are about the code, not the packaging, and are not on this branch.

`DefinitionInertnessGuardTest.java:87-88` lists both `DefinitionScope` and `RequiredAuthority` among
the 14 names no `src/main` file outside `definition` may mention. Moving `RequiredAuthority` out
needs that list amended; `DefinitionScope` staying means its entry stands.

## Out of scope

Everything issue 09 already excludes, plus **the PRD itself** — this issue produces the code and the
judgement-call artifact to write it from.
