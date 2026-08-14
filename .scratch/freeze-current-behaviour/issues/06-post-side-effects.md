# Post side effects of Negotiation transitions

Status: ready-for-human

## Parent

[Freeze current behaviour](../PRD.md)

## What to build

Pin what Negotiation transitions do to posts, so the three Action beans can be re-registered in ADR
0002's Action registry with evidence behind them.

Three Actions hang off Negotiation transitions today: submitting enables public posts, approving
enables private posts, and abandoning from in-progress disables posts. Note the asymmetry worth
pinning — abandoning from *paused* has no Action attached, so the two abandon routes are not
equivalent.

Separately, the Negotiation service accepts an optional message alongside an event, and when one is
supplied it becomes a `PUBLIC` post attributed to the caller, with the creation timestamp set at
persist time. Pin that the post is created, its type, its body and its author, and pin that no post
is created when the message is absent or empty.

One more effect belongs here because it is written by the same persist listener: reaching SUBMITTED
resets the Negotiation's creation date.

All of these are observed after an asynchronous persist path, so Awaitility with a bounded timeout
throughout.

## Acceptance criteria

- [x] Public posts are pinned as enabled after the submit transition.
- [x] Private posts are pinned as enabled after the approve transition.
- [x] Posts are pinned as disabled after abandoning from in-progress.
- [x] Abandoning from paused is pinned as leaving the post flags untouched, recording the asymmetry.
- [x] Sending an event with a message is pinned as creating exactly one post, of type `PUBLIC`, with
      the given body and the caller as author.
- [x] Sending an event with no message, and with an empty message, are both pinned as creating no post.
- [x] Reaching SUBMITTED is pinned as resetting the Negotiation's creation date.
- [x] All assertions use Awaitility with a bounded timeout. (Precisely: every observable written by
      the asynchronous persist path is reached through `LifecyclePersistence` first. The plain
      assertions that follow read a row whose existence was already awaited, or a precondition
      asserted before the send.)
- [x] Every State and Event is named as a string; the forbidden-import guard passes.
- [x] No production code is modified.

## Blocked by

- [Negotiation transition and authority parity](03-negotiation-transition-parity.md)

## What landed

| File | What it does |
|---|---|
| `characterization/service/NegotiationGraphV1.java` | new `PostEffect`, `PostFlags`, `POST_EFFECTS` (all eight Transitions) and `postEffects(state, event)` |
| `characterization/service/NegotiationGraphV1BindingTest.java` | 3 new tests binding `POST_EFFECTS` to the committed dump's `actions` arrays |
| `characterization/service/SeededNegotiationSubject.java` | new: the Negotiation-side subject helper and the SQL that reads back flags, posts and the creation date |
| `characterization/service/NegotiationPostEffectsTest.java` | 9 tests: the eight-Transition walk (each fired twice) plus the abandon asymmetry |
| `characterization/service/NegotiationEventMessagePostTest.java` | 7 tests: the optional message |
| `characterization/service/NegotiationCreationDateResetTest.java` | 2 tests: the SUBMITTED creation-date reset |
| `characterization/service/LifecyclePersistence.java` | extended with `awaitValue` / `awaitValueAfterSettling` for observables that are not States |

Parity gate after: **189 tests, 0 failures, 0 errors, 1 intentional skip** (was 168).

## Findings

**1. The asymmetry is real, and the dump settles it.** The committed
`lifecycle/negotiation-graph-v1.json` attaches exactly three Actions across eight Transitions, and
`PAUSED --ABANDON--> ABANDONED` is not one of them. Verbatim before-picture, which is what the
binding test reads and what nothing else in the suite may name:

| Transition | `actions` in the dump |
|---|---|
| `DRAFT --SUBMIT--> SUBMITTED` | `EnablePublicPostsAction` |
| `SUBMITTED --APPROVE--> IN_PROGRESS` | `EnablePrivatePostsAction` |
| `IN_PROGRESS --ABANDON--> ABANDONED` | `DisablePostsAction` |
| the other five | `[]` |

The ticket's description of all three effects is accurate. Fired for real, the two abandon routes
end in the same State with the flags in different places: `(true, true)` survives an abandon from
`PAUSED` and becomes `(false, false)` on an abandon from `IN_PROGRESS`.

**2. The Action bean names are named in exactly one place, deliberately.** `EFFECT_OF_ACTION` in
`NegotiationGraphV1BindingTest` maps bean name to `PostEffect`, and it reads them out of the frozen
committed dump - a statement about a file, not about code ADR 0002 rewrites. Everything downstream
(`POST_EFFECTS`, `NegotiationPostEffectsTest`) speaks only of effects, so no behavioural assertion
embeds a class name the cutover moves. This is ticket 09's rule applied to Actions.

**3. The message-borne post is not an Action, and must not become one.** It is written by the same
code that writes the new State, unconditionally on every Transition, from a header the service sets
on every send. It therefore appears on Transitions that carry an Action and on Transitions that
carry none alike - pinned across `APPROVE` (has an Action), `DECLINE` (none) and `ABANDON` from
`PAUSED` (none). Modelling it as a per-Transition Action in ADR 0002's registry would be wrong in
both directions: it would need registering on all eight edges, and it has nothing to do with the
three beans this ticket's other half is about.

**4. The emptiness test is `isEmpty`, not `isBlank`.** A message of a single space creates a post
like any other. Pinned as `eventWithABlankMessage_createsAPostAllTheSame`, because it is exactly the
kind of edge a reimplementation tidies without noticing it changed what a caller gets.

**5. Observing an Action needs no separate wait.** A Transition's Actions run while it executes,
ahead of the new State being written, so a State that has landed is a Transition whose Actions have
already completed - confirmed across the 16 fired arms of the walk. Awaiting the target State is
therefore sufficient to read a flag the Action moved. The one place that is not good enough is a
claim that *nothing* moved, so the abandon-asymmetry test settles for three seconds before asserting
the paused route left the flags alone.

**6. The flags must be placed before every row, or the walk is vacuous.** The seed leaves
`negotiation-2` with public posts already enabled and private disabled, and `negotiation-6` the same.
A test that fired `SUBMIT` without first setting both flags to `false` would pass without the Action
having run at all. Every row here therefore writes a known flag setting first, and fires each
Transition twice - from `(false, false)` and from `(true, true)` - so no row can pass by starting
where it was going to end up.

**7. The creation-date reset is keyed on the State arrived in, not on the Event.** The write is
`if (currentState == SUBMITTED) setCreationDate(now())`, after the new State has been assigned. No
test can separate "arriving in SUBMITTED" from "the SUBMIT Event" today, because `DRAFT --SUBMIT-->
SUBMITTED` is the only Transition that reaches `SUBMITTED`. What is pinned is the pair: reaching
`SUBMITTED` resets it, and a Transition arriving anywhere else leaves it alone. If ADR 0009's seed
ever makes a second Transition target `SUBMITTED`, this becomes two different behaviours.

**8. Unpinned, read from source, worth knowing.** The post's author is resolved at persist time by
`personRepository.findById(postSenderId).orElse(null)`, so a caller whose id has no Person row would
produce a post with no author rather than a refusal. Not reachable through the Lifecycle seam
without an authenticated principal that no seeded Person backs, so it is recorded rather than
pinned; a re-registration that made the lookup strict would be a behaviour change nothing catches.

**9. Corpus facts later tickets can rely on.** `negotiation-2` carries no posts at all in the seed -
every seeded post belongs to `negotiation-1` - which is what makes "exactly one post" assertable
without believing a baseline count. It also has no Resources, so driving it never Spawns Resource
Lifecycles. `negotiation-6` is the only Negotiation seeded in `DRAFT` and its creation date is in the
past, which is what makes the reset observable.
