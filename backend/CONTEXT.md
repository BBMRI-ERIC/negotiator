# Negotiator Backend

The Spring Boot service behind the BBMRI-ERIC Negotiator: negotiations over access to biobank resources, their lifecycles, and the governance structures (networks, representatives) around them.

## Language

### Lifecycles

**Lifecycle**:
The progression of a single Negotiation, or of a single Resource within a Negotiation, through the States of the Definition Version pinned to it. Not an entity of its own — the Negotiation or NegotiationResourceLink row is where its current State and history live.
_Avoid_: workflow, process, state machine

### Definitions

**Definition Family**:
A lineage of state-machine definition versions sharing an immutable `family_key`. What Resources and Networks associate with — never a specific version.
_Avoid_: definition group, machine type

**Definition Version**:
One immutable row of a Definition Family — a complete graph of States, Events and Transitions. Its row id is what everything else points at. Exactly one version per family is *active*: the one new work resolves to. Publishing a new version moves that flag, and never touches work already in flight.
_Avoid_: definition revision, definition instance

**Version (sequence)**:
A per-family, system-assigned incrementing integer on each Definition Version, for human display and audit only ("v3 of Standard flow"). Assigned at row creation; gaps are harmless. Carries no identity role.

**Definition Scope**:
Which of the two kinds of Lifecycle a Definition Family governs: a Negotiation's, or a Resource's within a negotiation. Fixed for the whole family.
_Avoid_: definition type, level, tier

### The definition graph

**State**:
A named position in a Definition Version's graph, carrying a human label plus *initial* and *terminal* flags. Exactly one State per Definition Version is initial.

**Legacy State**:
A State that exists only because pre-redesign data names it — no Transition leads to one, and new work can never enter one. Kept so that existing history, and any live state still holding an old value, continue to resolve.
_Avoid_: inactive state, dropped state, dead state, deprecated state

**Event**:
A named trigger that can be fired at a Lifecycle to move it out of its current State. One Event may drive Transitions from several different States of its Definition Version.

**Override Event**:
An Event carrying no Transition at all, naming an admin's direct change of a state rather than a move through the graph. It bypasses every gate, and exists so the change still appears in history under a name.
_Avoid_: manual override, admin bypass

**Guard**:
A named, configurable precondition checked before a Transition commits — whether the domain currently permits the move. Which Guards apply is data; each Guard's logic is code, referenced by name from a fixed catalogue.
_Avoid_: condition, validator, rule

**Action**:
A named, configurable effect run only after a Transition commits. Same catalogue-plus-configuration shape as a Guard.
_Avoid_: side effect, hook, listener

**Wiring**:
The configuration attaching a Guard or Action to where it applies — one Transition, or, for Guards only, an entire Definition Version and therefore all of its Transitions.
_Avoid_: binding, registration, attachment

**Transition**:
A directed edge from one State to another for a given Event, carrying the Required Authority and the Guard and Action Wiring that apply to it.

**Required Authority**:
Which kind of caller may fire a Transition: anyone, an admin, the Negotiation's creator, a Resource's representative, or the system alone. Distinct from a Guard in that it asks *who* is firing, never whether the domain permits the move.
_Avoid_: guard, role, permission, secured rule

**System Event**:
An Event whose Required Authority is the system alone, so only an Orchestration Trigger can ever fire it — it fails the authority check for every human caller and is never offered to one. Flipping an Event between system and a human authority is how "advances by itself" is configured.
_Avoid_: automatic event, signal event, auto-fire event

### Evaluation

**Transition Evaluator**:
The stateless component that answers what a Definition Version permits from a given State: whether a particular Event may fire and which State it leads to, or which Events could fire at all. It holds no state, reads no data of its own, and changes nothing — committing a move and running its Actions belong to the services around it.
_Avoid_: engine, state machine engine, workflow engine

**Evaluation Pipeline**:
The fixed order in which a Transition is gated: Required Authority, then the information-requirement check, then Guards — stopping at the first failure. One pipeline answers both "may this fire now" and "what could fire now", so the two can never disagree.

**Possible Events**:
The Events a caller could fire right now against a Lifecycle in its current State. Anything blocked — by authority, an unmet requirement, or a Guard — is left out rather than listed as unavailable.
_Avoid_: available events, allowed transitions

**Built-in Stage**:
A step of the Evaluation Pipeline that is structural rather than configured: it always runs, and no Wiring exists that could omit it. Used where a forgotten configuration row would silently break a guarantee.
_Avoid_: hardcoded check, implicit guard

### Resolution and pinning

**Definition Resolution**:
How new work finds the Definition Version it will run under: the active version of a resolved Definition Family. A Negotiation has one family to resolve; a Resource's is looked up in a fixed order of precedence, and resolved per resource — so two Resources in one Negotiation may run different definitions.
_Avoid_: lookup, selection, matching

**Global Default Family**:
The one Resource-scope Definition Family a Resource resolves to when nothing more specific applies. Exactly one family carries the flag, which is what makes Definition Resolution total: it can never fail to find a family.
_Avoid_: fallback definition, default machine

**Definition Version Pin**:
The immutable Definition Version recorded on a Negotiation or a NegotiationResourceLink when its Lifecycle begins, so publishing a new active version never moves work already in flight. A Negotiation pins at creation; a Resource pins at Spawn, since that is when its Lifecycle starts.
_Avoid_: version lock, snapshot, freeze

### Lifecycle coupling

**Orchestration Trigger**:
What, outside the Transition Evaluator, re-attempts a Lifecycle's System Events from whatever State it is currently in, leaving the Evaluation Pipeline to decide whether any of them fires. Two things can pull it: another Lifecycle's Transition committing, and a clock tick.
_Avoid_: scheduler, poller, self-firing transition

**Spawn**:
Starting the Resource Lifecycles of a Negotiation: for each requested Resource, resolve its Definition Family, pin the version, and set its State to that definition's initial one. Nothing is created — the Resources are already linked — so Spawn names the initialization, not an instantiation.
_Avoid_: fan-out, instantiate, launch

**Feedback**:
The opposite direction to Spawn: a Negotiation's Resources all reaching terminal States driving a Transition of the Negotiation itself. Nothing configures which Event they feed — that a Resource belongs to its Negotiation is structural, not an admin's choice.
_Avoid_: fan-in, rollup, propagation

### Information requirements

**Information Requirement**:
A form that must be filled in before a given Event can fire. It attaches to the Event, so it applies wherever that Event is used. Whether it is satisfied is never stored — it is worked out from the submissions each time.
_Avoid_: prerequisite, IR instance, requirement state

**Audience**:
The people an Information Requirement is asked of. Worked out fresh every time the requirement is checked, never fixed in advance.
_Avoid_: assignee, group, recipients

**Audience Resolver**:
The named strategy that produces an Audience — a Resource's representatives, a Negotiation's creators, or the members of an IAM group. Each one says which Definition Scope it can serve, and a mismatch is refused when the requirement is saved rather than discovered later.
_Avoid_: assignee type, audience provider

**Qualifying Submission**:
A submitted form that counts toward an Information Requirement: one whose submitter is in the Audience at the moment it is checked. A form filled in by someone outside the Audience is kept, but never counts.
_Avoid_: valid submission, accepted submission

**Quantifier**:
How many of an Audience must have submitted before an Information Requirement counts as satisfied: any one of them, all of them, or a set number.
_Avoid_: aggregation mode, cardinality, completion condition

**Audience Notification**:
Telling an Audience that an Information Requirement is waiting on them. Nothing configures it — it follows from a Lifecycle entering a State one step away from the Event the requirement guards, and reaches only the members who have not submitted yet. That it happened is never recorded.
_Avoid_: contact, invitation

### Lifecycle history

**Lifecycle Record**:
One row per State a Lifecycle has entered, in order — the history of a Negotiation, or of a Resource within one. It records only the State arrived at, not the Event that caused it; where a move came from is simply the record before it.
_Avoid_: audit log, state history, transition log
