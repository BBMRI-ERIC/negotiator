# Engine landscape research

Type: research
Status: resolved

## Question

From a clean slate — ignoring any prior-art exploration already sitting in this repo's other git worktrees (`proto+state-machine-stateless4j`, `proto+state-machine-stateless4j-claude`, `proto-flowable`, `proto-stateless4j`; do not read or reference them) — survey the current (2026) landscape of Java state-machine approaches and produce a markdown comparison covering at minimum:

- Actively maintained FSM libraries, if any exist beyond what's already known going in (Spring Statemachine is EOL, stateless4j is unmaintained, and no other good off-the-shelf option was identified in earlier scoping).
- Workflow-engine-style alternatives (e.g. Flowable, Camunda) and whether their weight/complexity fits a lifecycle this size.
- What a hand-rolled FSM implementation would need to cover, roughly sized (LOC/complexity estimate), to match the current feature set: named states/events, guards, actions, security-gated transitions — plus the new requirement that definitions be data-driven/configurable rather than compiled-in.
- Licensing/maintenance-risk notes for any library candidate surfaced.

Output a linked markdown asset with the comparison. Do not make the final call here — ticket 03 makes the engine decision using this research plus ticket 01's definition-model requirements.

## Answer

Full report: [`research/02-engine-landscape.md`](../research/02-engine-landscape.md)

Verified against primary sources (GitHub repo metadata, Maven Central, official license files/announcements) as of 2026-07-14:

1. **No viable off-the-shelf FSM library exists.** Spring Statemachine is confirmed EOL — archived to `spring-attic`, Maven Central frozen at `4.0.0` (Dec 2023), future releases commercial-only (Tanzu). Every other named candidate (stateless4j, Squirrel Foundation, EasyFlow, Apache Commons SCXML) is dormant or "zombie"-maintained. One active newer entrant, `jd-easyflow` (JD.com-backed, Apache-2.0, June 2025 release), exists but has low adoption and single-corporate-sponsor bus-factor risk.
2. **Workflow engines are all too heavy or licensing-encumbered.** Camunda 7 is archived/EOL. Camunda 8 is very active but its core engine (Zeebe/Operate/Tasklist/Optimize/Identity) requires a paid license for production use under the source-available Camunda License 1.0. Flowable is the cleanest fully-Apache-2.0, actively-maintained option, but it's a full BPMN/CMMN/DMN process engine — disproportionate to a single-entity lifecycle. jBPM (legacy) is frozen since 2023; its successor Apache KIE/Kogito is active but still ASF-incubating.
3. **Hand-rolled sizing**: current Spring-Statemachine implementation measures ~1,493 LOC across 25 files. A hardcoded hand-rolled equivalent would likely be similar order of magnitude (~1,200–1,800 LOC). Making it data-driven (states/events/transitions/guards as relational data) adds a CRUD/admin layer, a graph-loader/validator, and a bounded guard/action dispatch registry — sized by analogy to this repo's existing `info_requirement` package (380 LOC) — bringing the total estimate to roughly **2,500–4,000 LOC**, about 1.5–2.5× the current implementation.
4. Full licensing/maintenance-risk table for every candidate is in the linked report.

No engine recommendation is made here by design — ticket 03 (Engine choice) makes that call using this research plus the Definition model decision.

## Review findings (unresolved)

<!-- Surfaced by a wayfinder-map consistency review on 2026-07-16. Unchecked = not yet reconciled; pick up in a fresh session. -->

- [x] **Linked research asset has a stale header.** `research/02-engine-landscape.md` still reads `Status: draft — feeds ticket 03-engine-choice.md`, even though this ticket (which cites it) is `Status: resolved`. Update the asset's header to reflect resolution. — *Fixed.*
- [x] **Map's Decisions-so-far gist overstates this ticket's own finding** — see map.md's own Review findings block ("all named alternatives dormant" vs. this ticket explicitly carving out `jd-easyflow` as active, plus two more active niche libraries shown in the research asset's own table). — *Fixed.*
