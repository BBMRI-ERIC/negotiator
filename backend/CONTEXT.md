# Negotiator backend

The Spring Boot service behind the BBMRI-ERIC Negotiator. It runs negotiations over access to biobank resources, tracks their lifecycles, and holds the governance structures around them, such as networks and representatives.

## Language

### Lifecycles

**Lifecycle**:
The path a single Negotiation, or a single Resource within a Negotiation, takes through the States of the Definition Version pinned to it. It is not an entity of its own. The Negotiation or NegotiationResourceLink row holds its current State and its history.
_Avoid_: workflow, process, state machine

### Definitions

**Definition Family**:
A line of state-machine definition versions sharing one immutable `family_key`. Resources and Networks attach to a family, never a version.
_Avoid_: definition group, machine type

**Definition Version**:
One immutable row of a Definition Family: a full graph of States, Events and Transitions, known by its row id. At most one version per family is active, and new work resolves to it; a Definition Family with none cannot be resolved. Publishing moves the flag and leaves work in flight alone.
_Avoid_: definition revision, definition instance

**Version (sequence)**:
A counter set on each new Definition Version, rising per family, for display and audit only ("v3 of Standard flow"). Gaps do no harm; it plays no part in identity.

**Definition Scope**:
Which of the two kinds of Lifecycle a Definition Family governs: a Negotiation's, or a Resource's within one. All its versions share it.
_Avoid_: definition type, level, tier

### The definition graph

**State**:
A named position in a Definition Version's graph, with a label, a description, a Progress Order, and initial and terminal flags. Each Definition Version has exactly one initial State. A write from outside the Lifecycle can put a Lifecycle in a State that is not initial and that no Transition targets, so occupancy does not prove a State can be entered.

**Progress Order**:
How far along its Lifecycle a State sits; it picks "the most advanced State" of several. The State records it because a graph with branches has no total order to derive it from. The REST API still publishes it under its older name, `ordinal`.
_Avoid_: rank, sort order, sequence, advancement, stage

**Legacy State**:
A State that exists only because data from before the redesign names it. No Transition leads to one, and new work never enters one. It stays so that existing history, and live state still holding an old value, resolve.
_Avoid_: inactive state, dropped state, dead state, deprecated state

**Well-known State**:
A State that code outside the Lifecycle names directly instead of reading it from a Definition Version. Effects not configured as Actions use it to know when to run. Nothing in the definition marks one, so the dependency lives in the code that depends on it, not on the State. A Definition Version that leaves one out does not fail. It never triggers what depended on it.
_Avoid_: hardcoded state, special state, magic state

**Event**:
A named trigger fired at a Lifecycle to move it out of its current State. It has a label and a description for people. One Event can drive Transitions from several States of its Definition Version.

**Override Event**:
An Event with no Transition at all. It names an admin's direct change of a state, not a move through the graph. It skips every gate and exists so the change still shows in history under a name.
_Avoid_: manual override, admin bypass

**Guard**:
A named, configurable precondition, checked before a Transition commits, on whether the domain allows the move right now. Data says which Guards apply. Each Guard's logic is code, named in a fixed catalogue.
_Avoid_: condition, validator, rule

**Action**:
A named, configurable effect run only after a Transition commits, shaped like a Guard.
_Avoid_: side effect, hook, listener

**Wiring**:
The configuration that attaches a Guard or Action to where it applies: one Transition, or, for Guards only, a whole Definition Version and so all of its Transitions.
_Avoid_: binding, registration, attachment

**Transition**:
A directed edge from one State to another for a given Event, carrying the Required Authority and the Guard and Action Wiring that apply to it.

**Required Authority**:
The kinds of caller who may fire a Transition: an admin, the Negotiation's creator, a Resource's representative, or the system. Each Transition holds a set of them, and holding any one is enough. An empty set admits any person but not the system. A set that holds the system holds nothing else. It differs from a Guard because it asks who is firing, never whether the domain allows the move.
_Avoid_: guard, role, permission, secured rule

**System Event**:
An Event whose Transitions require the system, so only an Orchestration Trigger can fire it.
_Avoid_: automatic event, signal event, auto-fire event

**Compiled Graph**:
One Definition Version's States, Events and Transitions in the form the Transition Evaluator receives. It is immutable and carries each Transition's Guards and Actions as the chain that runs. It comes from that one version's rows and that version alone identifies it, so the system judges each Lifecycle against its pinned version's graph. Admin tools never read it, because it drops sort orders and the two Guard scopes and folds in each Guard's configuration so nobody can read it back. To show an admin the effective chain, query the Wiring rows.
_Avoid_: state machine, machine instance, materialized graph, in-memory definition

**Definition Compilation**:
Building a Compiled Graph from the rows of one Definition Version. It finds each Guard and Action in its catalogue, reads its configuration, and folds the two Guard scopes into one chain per Transition. Loading the rows is a separate step. Compiling runs once per version, not once per evaluation, so a definition too broken to compile fails here and not on a caller's first attempt.
_Avoid_: loading, hydration, parsing

### Evaluation

**Transition Evaluator**:
The stateless component that says what a Definition Version allows from a given State: whether an Event may fire and where it leads, or which Events could fire at all. It changes nothing and runs no queries; it asks whether forms were submitted through one injected port. The services around it commit moves and run Actions.
_Avoid_: engine, state machine engine, workflow engine

**Event Firing**:
Presenting an Event to a Lifecycle. The Evaluation Pipeline judges it and, if it passes, the Lifecycle moves: firing records the new State, appends a Lifecycle Record unless it arrived at the initial State, and runs the Actions. The Transition Evaluator only judges. Firing reads the Definition Version Pin to judge against, and only firing writes.
_Avoid_: send event, trigger, dispatch, execute transition

**Evaluation Pipeline**:
The fixed order that gates a Transition: Required Authority, then the information requirement check, then Guards. It stops at the first failure. One pipeline answers both "may this fire now" and "what could fire now", so the two answers always agree.

**Possible Events**:
The Events a caller could fire right now against a Lifecycle in its current State. It leaves out anything blocked by authority, an unmet requirement or a Guard, rather than marking it unavailable.
_Avoid_: available events, allowed transitions

**Built-in Stage**:
A step of the Evaluation Pipeline that is structural, not configured. It always runs, and no Wiring can leave it out. Use one where a forgotten configuration row would break a guarantee unnoticed.
_Avoid_: hardcoded check, implicit guard

### Resolution and pinning

**Definition Resolution**:
How new work finds the Definition Version it will run under: the active version of a resolved Definition Family. A Negotiation has one family to resolve. A Resource's family comes from a fixed order of precedence, resolved per Resource, so two Resources in one Negotiation can run different definitions.
_Avoid_: lookup, selection, matching

**Global Default Family**:
The Resource-scope Definition Family a Resource resolves to when nothing more specific applies. At most one family carries the flag. Definition Resolution relies on it, so having none is a configuration error.
_Avoid_: fallback definition, default machine

**Definition Version Pin**:
The immutable Definition Version recorded on a Negotiation or a NegotiationResourceLink when its Lifecycle begins, so publishing a new active version never moves work in flight. A Negotiation pins at creation. A Resource pins at Spawn, or when it is linked if the Negotiation is already running. Read anything about work in flight, such as a State's label or the Events it could fire, through the pin. Resolving the active version instead would describe a Definition Version that Lifecycle is not running. The pin also says whether a Lifecycle has started. A NegotiationResourceLink with no pin has not started, and any State on such a link came from an Override, not from the graph.
_Avoid_: version lock, snapshot, freeze

**Unstarted Lifecycle**:
A NegotiationResourceLink with no Definition Version Pin. Its Resource Lifecycle has not begun, so it has no graph, no Possible Events and no terminal judgement, and the system refuses to judge it rather than answer against the active version. The Override path can still write it a State before approval. That value is not a position in any graph, and nothing may read it as one. The reverse does not occur, because whatever writes a pin also gives the link a State.
_Avoid_: uninitialized resource, stateless link, orphan link

### Lifecycle coupling

**Orchestration Trigger**:
Something outside the Transition Evaluator that retries a Lifecycle's System Events from its current State, letting the Evaluation Pipeline decide whether any fires. Two things set it off: another Lifecycle committing a Transition or an Override Event, and a clock tick.
_Avoid_: scheduler, poller, self-firing transition

**Spawn**:
Starting a Negotiation's Resource Lifecycles. For each requested Resource with no Definition Version Pin, it resolves the Definition Family and pins the version. For each with no State, it sets the State that says whether a representative was reached: the Well-known States `REPRESENTATIVE_CONTACTED` or `REPRESENTATIVE_UNREACHABLE`, or the definition's initial State if it has neither. It is an Action wired on every Transition that starts Resource work, so it can run more than once for one Negotiation, and each run starts only what earlier runs did not. It creates nothing, since the Resources are already linked; the name means setting up, not making new.
_Avoid_: fan-out, instantiate, launch

**Feedback**:
The reverse of Spawn: a Negotiation's Resources all reaching terminal States drives a Transition of the Negotiation itself. Nothing configures which Event they feed. A Resource belongs to its Negotiation by structure, not by an admin's choice.
_Avoid_: fan-in, rollup, propagation

### Information requirements

**Information Requirement**:
A form that must be filled in before a given Event can fire. It attaches to the Event, so it applies wherever that Event is used. Nothing stores whether it is satisfied; each check works it out from the submissions.
_Avoid_: prerequisite, IR instance, requirement state

**Audience**:
The people an Information Requirement asks for input. Each check works it out fresh; nothing fixes it in advance.
_Avoid_: assignee, group, recipients

**Audience Resolver**:
The named strategy that produces an Audience: a Resource's representatives, a Negotiation's creators, or the members of an IAM group. Each one states which Definition Scope it can serve, and saving a requirement with a mismatch fails.
_Avoid_: assignee type, audience provider

**Qualifying Submission**:
A submitted form that counts toward an Information Requirement, because its submitter is in the Audience when the check runs. A form from someone outside the Audience is kept but never counts.
_Avoid_: valid submission, accepted submission

**Quantifier**:
How many of an Audience must submit before an Information Requirement counts as satisfied: any one, all, or a set number.
_Avoid_: aggregation mode, cardinality, completion condition

**Audience Notification**:
Telling an Audience that an Information Requirement is waiting on them. Nothing configures it. It follows when a Lifecycle enters a State one step away from the Event the requirement guards, and it reaches only the members who have not submitted yet. Nothing records that it happened.
_Avoid_: contact, invitation

### Lifecycle history

**Lifecycle Record**:
One row per State a Lifecycle has entered, in order: the history of a Negotiation, or of a Resource within one. It records only the State reached, not the Event that caused it. The record before it shows where the move came from.
_Avoid_: audit log, state history, transition log
