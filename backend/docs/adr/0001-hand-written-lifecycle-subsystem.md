---
status: accepted
---

# Hand-written Lifecycle subsystem

Spring Statemachine ran both Lifecycles from Java configuration classes, and it is end-of-life. The project is archived to `spring-attic`, Maven Central stops at `4.0.0` (December 2023), and further releases are commercial-only. Staying on it means no security or compatibility fixes. A 2026 survey of the alternatives found nothing worth moving to.

We write the whole Lifecycle subsystem ourselves instead of adopting a library. That covers the Definition Version tables, the catalogue of Guards and Actions, the admin CRUD over definitions, Event Firing, and the Transition Evaluator at the centre. The Transition Evaluator is the one piece a library could have supplied, and it is the cheap part. Our definitions live in a relational schema with a named strategy catalogue (see [Lifecycle definitions are relational configuration](0002-lifecycle-definitions-are-relational-configuration.md)). No surveyed library reads such a schema, so adopting one means bending its execution model to ours. The schema, the catalogue, the admin tooling and the write path cost the same whatever evaluates the graph, and they make up most of the work.

How the Transition Evaluator judges a move, and how it gets its definition, is its own decision: [One Transition Evaluator over a Compiled Graph](0006-one-transition-evaluator-over-a-compiled-graph.md). How a permitted move is committed is in [Event Firing is the single write path](0007-event-firing-is-the-single-write-path.md).

We delete Spring Statemachine entirely. The `spring-statemachine-*` dependencies leave the POM, and the hand-written subsystem replaces every class built on them: both configurations, their Guard and Action classes, persist handlers, converters and listeners. Production traffic never runs on both at once.

## Considered options

- **Adopt an FSM library.** stateless4j, Squirrel Foundation, EasyFlow and Apache Commons SCXML are dormant or barely maintained. The one active candidate, `jd-easyflow` (Apache-2.0, from JD.com), has low adoption and depends on a single corporate sponsor. That is a worse position than the one we are leaving.
- **Adopt a BPMN engine.** Camunda 7 is archived, and Camunda 8 needs a paid licence for production use under the source-available Camunda License 1.0. Flowable is fully Apache-2.0 and maintained, but it is a complete BPMN, CMMN and DMN engine. That is far more than one entity's Lifecycle needs, and it brings its own transactional store to keep in step with our domain entities.
- **Keep Spring Statemachine.** Upstream is frozen.
