# Engine landscape research (2026)

Status: resolved — feeds ticket `03-engine-choice.md`. **No recommendation is made here.**

## Scope and method

This is a clean-slate survey of the current (July 2026) Java state-machine / workflow-engine
landscape, done as a replacement candidate search for Spring Statemachine. It does not draw on,
and was not informed by, any of this repo's other git worktrees
(`proto+state-machine-stateless4j`, `proto+state-machine-stateless4j-claude`, `proto-flowable`,
`proto-stateless4j`).

Every maintenance/license claim below was checked against a primary source — the project's own
GitHub repository metadata (archived flag, last push, license file/API field, release/tag dates)
fetched via the GitHub REST API, and/or Maven Central's `solrsearch` index for the actual published
artifact history — not secondary "top N libraries" roundups. Dates are as observed on 2026‑07‑14.

For grounding on the current feature set being replaced, this report also references (read-only,
in this repo, not the other worktrees):
- `backend/src/main/java/eu/bbmri_eric/negotiator/negotiation/state_machine/negotiation/NegotiationStateMachineConfig.java`
- `backend/src/main/java/eu/bbmri_eric/negotiator/negotiation/state_machine/resource/ResourceStateMachineConfig.java`
- `backend/src/main/java/eu/bbmri_eric/negotiator/info_requirement/` (an existing relational,
  data-driven concept in this codebase — see §3)

---

## 1. Actively maintained FSM libraries for Java

**Bottom line: no actively maintained, broadly adopted, general-purpose Java FSM library
surfaced.** Every specifically-named candidate is either formally EOL, dormant (no functional
release in 5+ years despite the repo still existing), or a low-adoption single-maintainer project.
One genuinely active newer entrant was found (`jd-easyflow`), but it is a small, low-adoption,
single-vendor-driven project, not an established de facto standard.

| Library | Repo | Last release (source) | Last commit/push | Archived? | License | Verdict |
|---|---|---|---|---|---|---|
| Spring Statemachine | [spring-attic/spring-statemachine](https://github.com/spring-attic/spring-statemachine) (moved from `spring-projects/spring-statemachine`) | Maven Central: `4.0.0` (2023‑12‑04); GitHub-only tags `4.0.1` (2025‑06‑29), `4.0.2` (2026‑06‑11) — **never published to Maven Central** | 2026‑07‑05 ("Create LICENSE prior to archive") | **Yes** — moved to the `spring-attic` org, README now says *"Spring Statemachine is no longer maintained"* | Apache-2.0 | Confirmed EOL |
| stateless4j | [stateless4j/stateless4j](https://github.com/stateless4j/stateless4j) | `2.6.0` (2019‑09‑14, Maven Central `com.github.stateless4j`) | 2023‑06‑17 | No | Apache-2.0 | Confirmed unmaintained (no functional commits in 3+ years, only a stale open-issue queue of 13) |
| Squirrel Foundation (`squirrel-foundation`) | [hekailiang/squirrel](https://github.com/hekailiang/squirrel) | `0.3.10` (2022‑08‑23, Maven Central `org.squirrelframework`) | 2024‑06‑04 | No | Apache-2.0 (per `LICENSE.txt`; GitHub API license field is `NOASSERTION`) | Dormant — 2 years since last commit, no release since 2022, single primary maintainer (`hekailiang`) |
| EasyFlow | [Beh01der/EasyFlow](https://github.com/Beh01der/EasyFlow) | `1.3.1` (2014‑01‑11, Maven Central `au.com.datasymphony:EasyFlow`) | 2023‑02‑17 | No | Apache-2.0 | Dormant — 12 years since last Maven Central release, single maintainer |
| Apache Commons SCXML | [apache/commons-scxml](https://github.com/apache/commons-scxml) | `commons-scxml2-2.0-M1` milestone tag, 2014‑04‑03 (never a final 2.0 release; no Maven Central listing found via search index despite mvnrepository showing the milestone jar) | 2026‑07‑13 | No | Apache-2.0 | "Zombie" maintenance — the repo gets near-daily commits, but they are CI/dependency-bump and Javadoc housekeeping (e.g. `Bump actions/setup-java`, `Javadoc` fixes on 2026‑07‑10/11/13); no functional release in over a decade |
| `jd-easyflow` (JD.com / Jingdong Technology) | [jd-opensource/jd-easyflow](https://github.com/jd-opensource/jd-easyflow) | `1.6.1` (2025‑06‑12, Maven Central `com.jd.easyflow`) | 2026‑06‑27 | No | Apache-2.0 | **Actively maintained** newer entrant — corporate-backed (JD.com), FSM + lightweight BPMN-style "flow" module, but small adoption (265 stars), single corporate sponsor = bus-factor risk if JD deprioritizes it |
| `davidmoten/state-machine` | [davidmoten/state-machine](https://github.com/davidmoten/state-machine) | not checked on Maven Central; GitHub-only activity | 2026‑07‑02 | No | Apache-2.0 | Active but niche (144 stars), single prolific maintainer (well-known Java OSS author, but still a bus-factor-of-one project); generates immutable state classes from a spec, no built-in security-gating |
| `pnavais/state-machine` | [pnavais/state-machine](https://github.com/pnavais/state-machine) | GitHub-only | 2025‑10‑09 | No | Apache-2.0 | Active but very low adoption (26 stars), zero-dependency, single maintainer |
| Tinder `StateMachine` | [Tinder/StateMachine](https://github.com/Tinder/StateMachine) | GitHub-only | 2024‑07‑22 | No | BSD-style (Match Group, LLC copyright header) | **Not a Java library** — Kotlin/Swift DSL only (no JVM Java API surface); mentioned only because it's an explicit check-item in scope. Corporate-backed (Match Group) but not applicable to a plain-Java backend |
| `j-easy/easy-states` | [j-easy/easy-states](https://github.com/j-easy/easy-states) | GitHub-only | 2020‑11‑15 | **Yes** (formally archived) | MIT | Confirmed dead |
| `statefulj/statefulj` | [statefulj/statefulj](https://github.com/statefulj/statefulj) | GitHub-only | 2022‑06‑25 | No | Apache-2.0 | Dormant |

Sources for the above (primary, checked directly): GitHub REST API (`/repos/{owner}/{repo}`,
`/releases`, `/tags`, `/commits`, `/git/tags/{sha}` for annotated-tag dates) for each repo listed;
[Maven Central `solrsearch` index](https://search.maven.org) for each groupId/artifactId; the
Spring announcement at
[spring.io/blog/2025/04/21/spring-cloud-data-flow-commercial](https://spring.io/blog/2025/04/21/spring-cloud-data-flow-commercial/)
(covers Spring Statemachine alongside Spring Cloud Data Flow/Deployer — *"Spring Statemachine 4.0.x
will be the last open-source line"*); the `spring-attic/spring-statemachine` README (post-archive),
which states outright: *"Spring Statemachine is no longer maintained."*

**Notable secondary confirmation:** the very last Maven Central artifact for
`org.springframework.statemachine:spring-statemachine-core` is `4.0.0` from December 2023 — the
`4.0.1`/`4.0.2` GitHub tags cut in 2025/2026 were never published as public Maven artifacts,
consistent with the announced move to a Tanzu-customers-only distribution model. Anyone depending
on Spring Statemachine from Maven Central today is permanently stuck on `4.0.0`.

---

## 2. Workflow-engine-style alternatives

All of these are current, real projects (not abandoned), but every one of them is architected
around general BPM/workflow orchestration — process definitions in an external DSL (BPMN/CMMN/DMN
or JSON), human task assignment/inboxes, forms, sub-processes, and (for the cloud-native ones) a
separate broker/orchestrator process — which is substantially more machinery than a single-entity
lifecycle with named states/events/guards/actions/security-gated transitions needs.

| Engine | Repo | Status | License | Weight/fit assessment |
|---|---|---|---|---|
| Camunda 7 | [camunda/camunda-bpm-platform](https://github.com/camunda/camunda-bpm-platform) | **Archived** (`archived: true`), last push 2025‑11‑04; repo description reads *"Camunda 7 CE is End of Life (EoL). Please check out Camunda 8 instead"* | Apache-2.0 | Confirmed EOL, not viable |
| Camunda 8 | [camunda/camunda](https://github.com/camunda/camunda) | Active — releases as recently as `8.9.12`/`8.8.31`/`8.7.35` (2026‑07‑07) | **Mixed**: connector SDK/clients are Apache-2.0, but the core engine (Zeebe), Operate, Tasklist, Optimize, Identity, and bundled connectors are under the **Camunda License 1.0** (source-available, not OSI-approved) — per [Camunda 8 Docs: Licenses](https://docs.camunda.io/docs/8.7/reference/licenses/) and the [2024 licensing update post](https://camunda.com/blog/2024/04/licensing-update-camunda-8-self-managed/): free for development, **a paid license is required for production use** of the core components | Would introduce a commercial-license dependency for a production research-negotiation platform, plus a distributed broker (Zeebe) architecture built for horizontally-scaled process orchestration — disproportionate to embedding a lifecycle in one Spring Boot app |
| Flowable | [flowable/flowable-engine](https://github.com/flowable/flowable-engine) | Active — `flowable-8.0.0` (2026‑02‑27), `flowable-7.2.0` (2025‑08‑21) | Apache-2.0 (verified via repo `LICENSE` file — plain Apache License 2.0, no source-available carve-out) | Fully open-source and genuinely active (commercially backed by the Flowable company, community commit rights per its [open-source governance model](https://www.flowable.com/open-source), forked from Activiti in 2016). But it is a full embeddable BPMN+CMMN+DMN process engine — process definitions as BPMN XML, a process-instance/task/execution data model, human task & forms support, a REST API layer, its own persistence schema — much larger surface than a single-entity, code-level lifecycle needs |
| jBPM (legacy, `kiegroup/jbpm`) | [kiegroup/jbpm](https://github.com/kiegroup/jbpm) | **Frozen** — last tagged release `7.74.1.Final` (2023‑07‑13); repo still gets sporadic pushes (2026‑07‑01) but no new version tags since 2023 | Apache-2.0 | Legacy line effectively superseded; not a going concern on its own |
| Apache KIE (incubating) / Kogito — jBPM's successor | [apache/incubator-kie-kogito-runtimes](https://github.com/apache/incubator-kie-kogito-runtimes) | Active — `10.0.0`/`10.1.0`/`10.2.0` released Dec 2024–Mar 2026 per the [Apache Incubator status page](https://incubator.apache.org/clutch/kie.html) (65 committers, 10 PPMC members, mentored by 3 Apache mentors) | Apache-2.0, but **still incubating** (started 2023‑01‑13, not yet graduated as of the last status update) | Community-governed (reduces single-vendor bus factor vs. Camunda/Flowable) but brings the full Drools rules-engine + Kogito cloud-native runtime stack; still working through ASF incubation/licensing graduation requirements, so its long-term packaging/API stability is less settled than Flowable's |
| Netflix Conductor → Conductor OSS | [conductor-oss/conductor](https://github.com/conductor-oss/conductor) | Active — Netflix stopped maintaining the original `Netflix/conductor` repo; development continues under `conductor-oss`, backed by Orkes | Apache-2.0 (Netflix-era license carried forward) | JSON-defined, data-driven workflow definitions (relevant precedent for §3) but designed for **distributed microservice/task-worker orchestration** (durable execution, external task workers polling a queue) — an entirely different deployment shape than an in-process JPA-entity lifecycle |

**Assessment for this domain:** none of these looks like a good weight/complexity fit for "a
single-entity lifecycle with named states/events, guards, actions, and security-gated transitions"
in a research-data-negotiation app. The lightest genuinely-open option (Flowable) is still a
full process engine with its own schema and API surface; the two cloud-native options (Camunda 8,
Conductor) assume a distributed-orchestration deployment model and, in Camunda 8's case, put the
core engine behind a source-available commercial license for production use.

---

## 3. Hand-rolled FSM implementation sizing

### What already exists today (feature-parity floor)

The current Spring-Statemachine-based implementation — two state machines (Negotiation, Resource),
covering 8 negotiation states / 8 events and 9 resource states / 10 events, with guards, actions,
and Spring-Security-gated transitions (`.secured(...)`) — is **~1,493 lines of Java** across 25
files in
`backend/src/main/java/eu/bbmri_eric/negotiator/negotiation/state_machine/{negotiation,resource}/`
(measured with `wc -l` on this repo's current `master`-derived branch). That total already includes
the state/event enums, the two `*StateMachineConfig` classes, action/guard beans, persistence
listeners, the append-only `*LifecycleRecord` history tables/entities, and the REST controllers
that expose available transitions — i.e., it's a reasonable proxy for "what feature parity costs in
this codebase's style," independent of which engine sits underneath.

A hand-rolled, **hardcoded** (compiled-in enum/config) equivalent — dropping the Spring Statemachine
dependency but keeping the same states/events/guards/actions/security gates and reusing the
already-existing `*LifecycleRecord` audit tables (which are already the source of truth, not
Spring Statemachine's own persistence) — would likely land in a similar order of magnitude, roughly
**1,200–1,800 LOC**: you lose Spring Statemachine's declarative graph builder and its built-in
`StateContext`/listener plumbing, but you also drop a fair amount of framework-adapter boilerplate
(the `Configurer` classes, `EnumSet`-to-`String` conversions, `SecurityRule` wiring). This part is
mostly a wash.

### What the new data-driven requirement adds

The new requirement — states, events, transitions, guards, and info-requirements stored as
relational data and editable at runtime, not as Java enums/config — is the part that meaningfully
grows the estimate, and this codebase already has a directly comparable precedent to size it from:
`backend/src/main/java/eu/bbmri_eric/negotiator/info_requirement/` already models one
config-as-relational-data concept (which access form is required for which
`NegotiationResourceEvent`) as a full vertical slice — entity (`InformationRequirement`), two DTOs,
repository, service interface + impl, controller, and an assembler — and that slice is **380 LOC**
end to end. `info_submission` (the runtime-facing counterpart, capturing submitted data against
those requirements) is another 712 LOC.

Using that in-repo slice as the unit of measure, a data-driven transition model needs a small
number of comparable slices, plus one genuinely new piece of engine logic:

1. **`State` / `Event` / `Transition` entities as data** (per entity type, i.e. Negotiation and
   Resource) — source state, event, target state, an ordered list of guard references, an ordered
   list of action references, and a security expression/required-role column — roughly 2–3
   `InformationRequirement`-sized slices: **~600–900 LOC** (entities + repositories + DTOs +
   admin CRUD controllers, reusing this repo's existing Spring Data REST / assembler conventions).
2. **A small runtime "transition engine"**: loads the state/event/transition rows for an entity type
   once (with a cache, since this data changes rarely relative to how often it's read), validates
   the graph at startup or on data change (no unreachable states, no event referencing an
   undefined transition — work Spring Statemachine's builder used to do for free), and exposes a
   `fireEvent(entity, event, principal)` operation that resolves the matching transition row,
   evaluates its guard(s), checks its security expression against the current principal, executes
   its action(s), and persists the new state (writing to the existing `*LifecycleRecord` tables,
   unchanged): **~400–700 LOC**.
3. **A guard/action registry**, because a relational `Transition` row can only reference guards and
   actions by name/id, not by arbitrary compiled Java — this needs a small bounded registry
   (e.g. a `Map<String, Guard>` / `Map<String, Action>` populated from Spring beans, keyed by the
   name stored in the `Transition` row) rather than genuinely dynamic code execution. This is a
   deliberately small, closed set of pre-registered handlers, not a scripting engine: **~100–200
   LOC**. (This mirrors the pattern used by Squirrel Foundation's `UntypedStateMachineImporter`,
   which imports an "SCXML-similar" external definition but still resolves actions through
   Java-side handler registration — see the
   [squirrel-foundation docs](https://hekailiang.github.io/squirrel/) — and by Apache Commons
   SCXML's own model, where the state chart is external XML *data* but custom actions are
   registered Java classes invoked by name. Both are real precedent for "definition as data, action
   dispatch through a bounded registry," which is the same shape this hand-rolled engine would
   need.)
4. **Data migration/seeding** of the current 8+9 states and 8+10 events and their transitions from
   Java enums into the new tables — mechanical, low-LOC (migration scripts + a one-time loader),
   not counted heavily here.

**Rough total estimate: ~2,500–4,000 LOC of new/changed backend Java**, roughly 1.5–2.5× the size
of the current (1,493 LOC) hardcoded implementation — the multiplier being the cost of turning a
compiled-in graph into an admin-editable relational one (CRUD/admin surface + a small graph
loader/validator + a bounded action/guard dispatch registry), not the cost of the state-machine
logic itself, which stays roughly the same size either way. This is an engineering judgment call,
not a citation-backed number, but it is grounded in this codebase's own measured LOC for both the
thing being replaced and the closest existing analogous "config-as-data" slice already in the repo.

The main complexity risk this estimate doesn't fully capture is **graph-consistency validation**:
Spring Statemachine's builder rejects an invalid state graph at startup for free; a hand-rolled,
data-driven engine has to reimplement that validation itself (or accept the risk of a bad admin
edit producing a broken lifecycle at runtime), which is exactly the kind of thing that's cheap to
underestimate in a first LOC pass.

---

## 4. Licensing / maintenance-risk summary

| Candidate | License | Last functional release | Maintenance signal | Bus-factor / backing |
|---|---|---|---|---|
| Spring Statemachine | Apache-2.0 | `4.0.0` on Maven Central (2023‑12); GitHub-only `4.0.2` (2026‑06) | **Archived** to `spring-attic`, README says "no longer maintained" | Was VMware/Broadcom (Tanzu Spring); future releases commercial-only |
| stateless4j | Apache-2.0 | `2.6.0` (2019‑09) | No commits since 2023‑06; 13 stale open issues | Community, effectively abandoned |
| Squirrel Foundation | Apache-2.0 | `0.3.10` (2022‑08) | No commits since 2024‑06 | Single maintainer (`hekailiang`) |
| EasyFlow (`Beh01der`) | Apache-2.0 | `1.3.1` on Maven Central (2014‑01) | No commits since 2023‑02 | Single maintainer |
| Apache Commons SCXML | Apache-2.0 | Milestone `2.0-M1` only (2014‑04), never a final 2.0 | "Zombie" — recent commits are CI/dependency bumps only | ASF project, but functionally unstaffed for feature work |
| `jd-easyflow` | Apache-2.0 | `1.6.1` (2025‑06) | Actively pushed (2026‑06) | Single corporate sponsor (JD.com) |
| `davidmoten/state-machine` | Apache-2.0 | GitHub-only, active (2026‑07) | Actively pushed | Single individual maintainer |
| `pnavais/state-machine` | Apache-2.0 | GitHub-only, active (2025‑10) | Low activity, low adoption | Single individual maintainer |
| Tinder `StateMachine` | BSD-style | GitHub-only, active (2024‑07) | Active, but Kotlin/Swift, not Java | Match Group (corporate) |
| Camunda 7 | Apache-2.0 | N/A | **Archived**, confirmed EOL | Camunda GmbH — redirected users to Camunda 8 |
| Camunda 8 (core: Zeebe/Operate/Tasklist/Optimize/Identity) | **Camunda License 1.0** (source-available; production use requires a paid license) | Active, weekly-ish patch releases | Very active | Camunda GmbH (well-funded, but licensing risk is structural, not a maintenance risk) |
| Camunda 8 (connectors/clients) | Apache-2.0 | Active | Very active | Camunda GmbH |
| Flowable | Apache-2.0 | `8.0.0` (2026‑02) | Very active | Flowable company + community commit rights |
| jBPM (legacy) | Apache-2.0 | `7.74.1.Final` (2023‑07) | Frozen | Superseded by Apache KIE/Kogito |
| Apache KIE / Kogito | Apache-2.0 | `10.2.0` (~2026‑03) | Very active | ASF-governed (65 committers), but **still incubating**, not yet graduated |
| Conductor OSS | Apache-2.0 | Active | Very active | Orkes + community (post-Netflix handoff) |

---

## Sources consulted directly

- GitHub REST API (`/repos/{owner}/{repo}`, `/releases`, `/tags`, `/commits`, `/git/tags/{sha}`,
  `/contents/LICENSE*`) for every repository named above.
- [Maven Central search (`search.maven.org/solrsearch`)](https://search.maven.org) for every
  groupId/artifactId named above.
- [spring.io/blog/2025/04/21/spring-cloud-data-flow-commercial](https://spring.io/blog/2025/04/21/spring-cloud-data-flow-commercial/) — Spring's own EOL announcement covering Spring Statemachine.
- [`spring-attic/spring-statemachine` README](https://github.com/spring-attic/spring-statemachine) — post-archive maintainer statement.
- [Camunda 8 Docs: Licenses](https://docs.camunda.io/docs/8.7/reference/licenses/) and [Camunda's 2024 licensing update post](https://camunda.com/blog/2024/04/licensing-update-camunda-8-self-managed/).
- [Apache Incubator status page for KIE](https://incubator.apache.org/clutch/kie.html).
- [Flowable's open-source governance page](https://www.flowable.com/open-source).
- [Squirrel Foundation documentation](https://hekailiang.github.io/squirrel/) (for the SCXML-import precedent cited in §3).
- This repo's own `backend/src/main/java/eu/bbmri_eric/negotiator/negotiation/state_machine/` and `info_requirement`/`info_submission` packages (LOC measurements via `wc -l`), for grounding §3's sizing estimate.
